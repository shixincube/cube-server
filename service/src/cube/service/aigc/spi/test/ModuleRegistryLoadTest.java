/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.spi.test;

import cube.aigc.spi.AIGCPluginContextLite;
import cube.auth.AuthToken;
import cube.aigc.spi.AIGCSPI;
import cube.aigc.spi.ActionBinding;
import cube.aigc.spi.ActionModule;
import cube.aigc.spi.ActionRouter;
import cube.aigc.spi.ModuleDescriptor;
import cube.common.Language;
import cube.common.entity.ObjectInfo;
import cube.service.aigc.spi.ModuleRegistry;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 模块注册表装载流程验证。
 *
 * <p>覆盖装载语义：启用开关 → 实例化 → 绑定 → setup 的顺序、
 * setup 失败后的动作回滚、可选性对阻塞状态的影响、能力校验，以及停机卸载。</p>
 *
 * <p>本用例在临时目录内构造配置文件，通过切换工作目录来驱动
 * {@link ModuleRegistry#load()}，不依赖真实宿主环境。</p>
 */
public class ModuleRegistryLoadTest {

    /**
     * 断言计数。
     */
    private static int passed = 0;

    /**
     * 失败计数。
     */
    private static int failed = 0;

    public static void main(String[] args) throws Exception {
        // 注意：ModuleRegistry 以相对路径 "config/aigc-modules.properties" 定位配置，
        // 因此本用例必须在本进程的工作目录内运行（由启动脚本 cd 到临时目录后再执行 java）。
        // 用例不再自行切换工作目录——System.setProperty("user.dir") 对 File 的相对路径解析
        // 在部分 JDK 上不生效，会导致 load() 读不到配置而产生假失败。
        File configDir = new File("config");
        if (!configDir.isDirectory()) {
            System.out.println("[FATAL] Working directory must contain a 'config' directory: "
                    + new File(".").getAbsolutePath());
            System.exit(2);
        }

        File config = new File(configDir, "aigc-modules.properties");

        try {
            //场景1：出厂零模块
            write(config, "# empty");

            ModuleRegistry registry = newRegistry(new NoopHost());
            int count = registry.load();
            assertTrue("S1零模块时count=0", 0 == count);
            assertTrue("S1 路由仍标记为已装载", registry.getRouter().isLoaded());
            assertTrue("S1 绑定表为空", 0 == registry.getRouter().size());
            assertTrue("S1 无阻塞失败", !registry.hasBlockingFailure());

            // 场景2：setup 被调用且早于绑定可见
            resetModuleEvents();
            write(config, "module.1.class=cube.service.aigc.spi.test.ModuleRegistryLoadTest$GoodModule");
            registry = newRegistry(new NoopHost());
            count = registry.load();
            assertTrue("S2 成功装载1个模块", 1 == count);
            assertTrue("S2 setup 已调用", 1 == GoodModule.setupCount);
            assertTrue("S2 setup 时动作已可见", GoodModule.actionsVisibleAtSetup > 0);
            assertTrue("S2 setup 收到非空host", GoodModule.hostReceived);
            assertTrue("S2 模块可按名查找", null != registry.findModule("good"));

            // 场景3：enabled=false 时跳过，且不调用 setup
            resetModuleEvents();
            write(config, "module.1.class=cube.service.aigc.spi.test.ModuleRegistryLoadTest$GoodModule\nmodule.1.enabled=false");
            registry = newRegistry(new NoopHost());
            count = registry.load();
            assertTrue("S3 禁用的模块不装载", 0 == count);
            assertTrue("S3 禁用时 setup 未被调用", 0 == GoodModule.setupCount);

            // 场景4：未写enabled 时默认启用
            resetModuleEvents();
            write(config, "module.1.class=cube.service.aigc.spi.test.ModuleRegistryLoadTest$GoodModule");
            registry = newRegistry(new NoopHost());
            assertTrue("S4 未配置enabled时默认启用", 1 == registry.load());

            // 场景5：setup 抛异常时回滚动作绑定
            resetModuleEvents();
            write(config, "module.1.class=cube.service.aigc.spi.test.ModuleRegistryLoadTest$FailingSetupModule");
            registry = newRegistry(new NoopHost());
            count = registry.load();
            assertTrue("S5 setup失败的模块不计入", 0 == count);
            assertTrue("S5 其动作已回滚", 0 == registry.getRouter().size());
            assertTrue("S5 非可选模块失败置阻塞标记", registry.hasBlockingFailure());
            // 声明不随绑定回滚而移除：宿主据此识别「该动作本属该模块，只是模块未就绪」，
            // 从而回 ModuleNotLoaded 而非让请求悬挂
            assertTrue("S5 失败后动作声明仍保留", registry.getRouter().isDeclared("failingAction"));
            assertTrue("S5 失败后动作未绑定", null == registry.getRouter().lookup("failingAction"));

            // 场景5b：模块未配置时不声明任何动作（区分「未装」与「已声明但失败」）
            resetModuleEvents();
            write(config, "module.1.enabled=false");
            registry = newRegistry(new NoopHost());
            registry.load();
            assertTrue("S5b 未装载的模块不产生声明", !registry.getRouter().isDeclared("failingAction"));

            // 场景6：optional=true 的失败不置阻塞标记
            resetModuleEvents();
            write(config, "module.1.class=cube.service.aigc.spi.test.ModuleRegistryLoadTest$OptionalFailingModule");
            registry = newRegistry(new NoopHost());
            count = registry.load();
            assertTrue("S6 可选模块失败仍不装载", 0 == count);
            assertTrue("S6 可选模块失败不阻塞宿主", !registry.hasBlockingFailure());

            // 场景7：坏类不影响好模块
            resetModuleEvents();
            write(config, "module.1.class=cube.service.aigc.spi.test.ModuleRegistryLoadTest$NoSuchModule\nmodule.2.class=cube.service.aigc.spi.test.ModuleRegistryLoadTest$GoodModule");
            registry = newRegistry(new NoopHost());
            count = registry.load();
            assertTrue("S7 坏类被拒但好模块仍装载", 1 == count);
            assertTrue("S7 好模块已绑定", registry.getRouter().isBound("goodAction"));

            // 场景8：动作冲突时整模块不装载
            resetModuleEvents();
            write(config, "module.1.class=cube.service.aigc.spi.test.ModuleRegistryLoadTest$GoodModule\nmodule.2.class=cube.service.aigc.spi.test.ModuleRegistryLoadTest$ConflictModule");
            registry = newRegistry(new NoopHost());
            count = registry.load();
            assertTrue("S8 冲突模块不装载、先注册者保留", 1 == count);
            assertTrue("S8 先注册者仍持有该动作",
                    "good".equals(registry.getRouter().lookup("goodAction").getOwner().getName()));

            // 场景9：能力校验——hasUnit 假值时报告不可用
            resetModuleEvents();
            write(config, "module.1.class=cube.service.aigc.spi.test.ModuleRegistryLoadTest$NeedsCapabilityModule");
            NoopHost host = new NoopHost();
            host.capabilityAvailable = false;
            registry = newRegistry(host);
            registry.load();
            assertTrue("S9 能力缺失时校验不通过", !registry.verifyCapabilities());

            // 场景10：能力齐备时校验通过
            resetModuleEvents();
            host = new NoopHost();
            host.capabilityAvailable = true;
            registry = newRegistry(host);
            registry.load();
            assertTrue("S10 能力齐备时校验通过", registry.verifyCapabilities());

            // 场景11：teardownAll 释放全部模块与绑定
            resetModuleEvents();
            write(config, "module.1.class=cube.service.aigc.spi.test.ModuleRegistryLoadTest$GoodModule");
            registry = newRegistry(new NoopHost());
            registry.load();
            registry.teardownAll();
            assertTrue("S11 teardown 已调用", 1 == GoodModule.teardownCount);
            assertTrue("S11 绑定已清空", 0 == registry.getRouter().size());
            assertTrue("S11 模块列表已清空", 0 == registry.getModules().size());
            assertTrue("S11 按名查找返回null", null == registry.findModule("good"));
            assertTrue("S11 阻塞标记已复位", !registry.hasBlockingFailure());

            // 场景12：SPI 版本不匹配时拒绝装载
            resetModuleEvents();
            write(config, "module.1.class=cube.service.aigc.spi.test.ModuleRegistryLoadTest$BadSpiVersionModule");
            registry = newRegistry(new NoopHost());
            assertTrue("S12 SPI版本不匹配被拒", 0 == registry.load());

            // 场景13：SPI 版本常量与描述符一致
            // 用 >= 而非 == ：本用例只关心「常量与各测试模块声明的版本同源」，
            // 不应因契约升版而红灯（升版时需同步确认各测试模块的兼容性）
            assertTrue("S13 当前SPI版本至少为6", AIGCSPI.VERSION >= 6);

            // 场景14：Lite 上下文可承载字段且不泄漏 service 类型
            resetModuleEvents();
            AIGCPluginContextLite lite = new AIGCPluginContextLite("task-x");
            lite.setInputTokens(11);
            lite.setOutputTokens(22);
            lite.addFileCode("f1");
            lite.addFileCode(null);
            lite.addFileCode("f2");
            assertTrue("S14 任务名可读写", "task-x".equals(lite.getTask()));
            assertTrue("S14 token计数可读写", 11 == lite.getInputTokens() && 22 == lite.getOutputTokens());
            assertTrue("S14 null 文件码被忽略", 2 == lite.getFileCodeList().size());
            assertTrue("S14 文件码顺序保持", "f1".equals(lite.getFileCodeList().get(0)));
            assertTrue("S14 get按名取值", "task-x".equals(lite.get("task")));
            lite.set("task", "task-y");
            assertTrue("S14 set按名赋值生效", "task-y".equals(lite.getTask()));
        }
        finally {
            config.delete();
        }

        System.out.println("");
        System.out.println("passed: " + passed + ", failed: " + failed);
        System.out.println(0 == failed ? "ALL PASSED" : "HAS FAILURE");

        if (0 != failed) {
            System.exit(1);
        }
    }

    /**
     * 写入配置文件（UTF-8）。
     *
     * @param file 目标文件。
     * @param content 内容。
     */
    private static void write(File file, String content) throws Exception {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            writer.write(content);
        }
    }

    /**
     * 断言为真。
     *
     * @param message 断言描述。
     * @param condition 条件。
     */
    private static void assertTrue(String message, boolean condition) {
        if (condition) {
            ++passed;
            System.out.println("[PASS] " + message);
        }
        else {
            ++failed;
            System.out.println("[FAIL] " + message);
        }
    }

/**
 * 重置测试模块的静态事件记录。
     */
    private static void resetModuleEvents() {
        GoodModule.setupCount = 0;
        GoodModule.teardownCount = 0;
        GoodModule.actionsVisibleAtSetup = 0;
        GoodModule.hostReceived = false;
    }

    /**
     * 构造注册表并把路由器注入测试模块，使其能在 setup 中自查动作可见性。
     *
     * @param host 宿主实现。
     * @return 返回注册表。
     */
    private static ModuleRegistry newRegistry(cube.aigc.spi.AIGCHost host) {
        ActionRouter router = new ActionRouter();
        GoodModule.routerRef = router;

        ModuleRegistry registry = new ModuleRegistry(router);
        registry.setHost(host);

        return registry;
    }

    // ───────── 测试用模块 ─────────

    /**
     * 正常模块。
     */
    public static class GoodModule implements ActionModule {

        static int setupCount = 0;

        static int teardownCount = 0;

        /**
         * setup 时刻本模块动作在路由器中已可见的数量，用于验证「绑定早于 setup」的顺序契约。
         */
        static int actionsVisibleAtSetup = 0;

        static boolean hostReceived = false;

        /**
         * 由用例注入的路由器引用。
         */
        static ActionRouter routerRef;

        @Override
        public String getName() {
            return "good";
        }

        @Override
        public ModuleDescriptor getDescriptor() {
            return new ModuleDescriptor("good", "1.0.0", AIGCSPI.VERSION,
                    Collections.singletonList("good"), Collections.emptyList(),
                    Collections.emptyList(), Collections.emptyList(), 1000L, false);
        }

        @Override
        public void setup(cube.aigc.spi.AIGCHost host) {
            ++setupCount;
            hostReceived = (null != host);

            // 顺序契约：setup 被调用时，本模块的动作必须已在路由表中可见，
            // 否则派发线程可能读到尚未初始化的实例
            actionsVisibleAtSetup = (null != routerRef && routerRef.isBound("goodAction")) ? 1 : 0;
        }

        @Override
        public void teardown() {
            ++teardownCount;
        }

        @Override
        public List<ActionBinding> getActions() {
            return Collections.singletonList(new ActionBinding("goodAction", ctx -> null));
        }
    }

    /**
     * setup 必定失败的非可选模块。
     */
    public static class FailingSetupModule extends GoodModule {

        @Override
        public String getName() {
            return "failing";
        }

        @Override
        public void setup(cube.aigc.spi.AIGCHost host) {
            super.setup(host);
            throw new IllegalStateException("intentional setup failure");
        }

        @Override
        public ModuleDescriptor getDescriptor() {
            return new ModuleDescriptor("failing", "1.0.0", AIGCSPI.VERSION,
                    Collections.singletonList("failing"), Collections.emptyList(),
                    Collections.emptyList(), Collections.emptyList(), 1000L, false);
        }

        @Override
        public List<ActionBinding> getActions() {
            return Collections.singletonList(new ActionBinding("failingAction", ctx -> null));
        }
    }

    /**
     * setup 失败但声明为可选的模块。
     */
    public static class OptionalFailingModule extends FailingSetupModule {

        @Override
        public String getName() {
            return "optionalFailing";
        }

        @Override
        public ModuleDescriptor getDescriptor() {
            return new ModuleDescriptor("optionalFailing", "1.0.0", AIGCSPI.VERSION,
                    Collections.singletonList("optionalFailing"), Collections.emptyList(),
                    Collections.emptyList(), Collections.emptyList(), 1000L, true);
        }

        @Override
        public List<ActionBinding> getActions() {
            return Collections.singletonList(new ActionBinding("optionalFailingAction", ctx -> null));
        }
    }

    /**
     * 与正常模块争抢同一动作名的模块。
     */
    public static class ConflictModule extends GoodModule {

        @Override
        public String getName() {
            return "conflict";
        }

        @Override
        public ModuleDescriptor getDescriptor() {
            return new ModuleDescriptor("conflict", "1.0.0", AIGCSPI.VERSION,
                    Collections.singletonList("conflict"), Collections.emptyList(),
                    Collections.emptyList(), Collections.emptyList(), 1000L, false);
        }

        @Override
        public List<ActionBinding> getActions() {
            return Collections.singletonList(new ActionBinding("goodAction", ctx -> null));
        }
    }

    /**
     * 声明了必需能力的模块。
     */
    public static class NeedsCapabilityModule extends GoodModule {

        @Override
        public String getName() {
            return "needsCapability";
        }

        @Override
        public ModuleDescriptor getDescriptor() {
            return new ModuleDescriptor("needsCapability", "1.0.0", AIGCSPI.VERSION,
                    Collections.singletonList("needsCapability"), Collections.emptyList(),
                    Collections.singletonList("SomeCapability"), Collections.emptyList(), 1000L, false);
        }
    }

    /**
     * SPI 版本不匹配的模块。
     */
    public static class BadSpiVersionModule extends GoodModule {

        @Override
        public String getName() {
            return "badSpi";
        }

        @Override
        public ModuleDescriptor getDescriptor() {
            return new ModuleDescriptor("badSpi", "1.0.0", AIGCSPI.VERSION + 999,
                    Collections.singletonList("badSpi"), Collections.emptyList(),
                    Collections.emptyList(), Collections.emptyList(), 1000L, false);
        }
    }

    // ───────── 测试用宿主 ─────────

    /**
     * 全部方法返回空值的宿主，用于在不启动真实服务的前提下驱动装载流程。
     */
    public static class NoopHost implements cube.aigc.spi.AIGCHost {

        /**
         * 模拟单元能力是否可用。
         */
        boolean capabilityAvailable = true;

        @Override
        public cube.auth.AuthToken resolveToken(String tokenCode) {
            return null;
        }

        @Override
        public boolean isRegistered(String tokenCode) {
            return false;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public List<String> tokenize(String text) {
            return Collections.emptyList();
        }

        @Override
        public List<String> segmentWords(String text) {
            return Collections.emptyList();
        }

        @Override
        public List<String> extractKeywords(String content, int topN) {
            return Collections.emptyList();
        }

        @Override
        public JSONObject queryPaintingReport(long sn, String format) {
            return null;
        }

        @Override
        public JSONObject queryScaleReport(long sn) {
            return null;
        }

        @Override
        public JSONObject listPaintingReports(long contactId, int page, int size, boolean descending, int state) {
            return null;
        }

        @Override
        public JSONObject listScaleReports(long contactId, boolean descending, int state) {
            return null;
        }

        @Override
        public JSONObject stopReportGeneration(long sn) {
            return null;
        }

        @Override
        public JSONObject resetReportAttention(long sn, Integer newAttention) {
            return null;
        }

        @Override
        public void fillHexagonScoreDescription(cube.aigc.psychology.composition.HexagonDimensionScore hds,
                Language language) {
        }

        @Override
        public String extractContent(String query) {
            return null;
        }

        @Override
        public cube.aigc.psychology.composition.PaintingFeatureSet getPaintingFeatureSet(long reportSn) {
            return null;
        }

        @Override
        public cube.aigc.psychology.PaintingReport getPaintingReport(long sn) {
            return null;
        }

        @Override
        public ObjectInfo detectObject(String domain, String fileCode, boolean visualize) {
            return null;
        }

        @Override
        public JSONObject getPaintingInferenceData(long sn) {
            return null;
        }

        @Override
        public cube.aigc.psychology.Painting getPredictedPainting(AuthToken token, String fileCode) {
            return null;
        }

        @Override
        public cube.common.entity.FileLabel getPredictedPainting(AuthToken token, long sn,
                boolean boundingBox, boolean visualParam, double probability) {
            return null;
        }

        @Override
        public cube.common.entity.AIGCChannel getChannelByToken(String tokenCode) {
            return null;
        }

        @Override
        public cube.common.entity.AIGCChannel acquireChannel(cube.auth.AuthToken token, String participant,
                String channelCode, cube.common.Language language) {
            return null;
        }

        @Override
        public cube.common.entity.AIGCChannel requestChannel(String tokenCode, String participant) {
            return null;
        }

        @Override
        public cube.common.entity.AIGCChannel stopChannel(String channelCode) {
            return null;
        }

        @Override
        public cube.common.entity.AIGCUnit selectUnit(String capabilityName) {
            return null;
        }

        @Override
        public cube.common.entity.AIGCUnit selectUnitBySubtask(String subtask) {
            return null;
        }

        @Override
        public int countUnits(String capabilityName) {
            return this.capabilityAvailable ? 1 : 0;
        }

        @Override
        public boolean hasUnit(String capabilityName) {
            return this.capabilityAvailable;
        }

        @Override
        public java.util.concurrent.atomic.AtomicInteger increaseUnitCounter(String unitName) {
            return new java.util.concurrent.atomic.AtomicInteger(0);
        }

        @Override
        public cube.common.entity.GeneratingRecord syncGenerateText(cube.common.entity.AIGCUnit unit,
                String prompt, cube.common.entity.GeneratingOption option,
                List<cube.common.entity.GeneratingRecord> history,
                cube.common.entity.Contact participantContact) {
            return null;
        }

        @Override
        public cube.common.entity.GeneratingRecord syncGenerateText(cube.auth.AuthToken token,
                String capabilityName, String prompt, cube.common.entity.GeneratingOption option,
                List<cube.common.entity.GeneratingRecord> history,
                cube.common.entity.Contact participantContact) {
            return null;
        }

        @Override
        public JSONObject generateStructured(cube.common.entity.AIGCUnit unit, String prompt,
                String jsonSchema, String schemaName, long timeoutMs) {
            return null;
        }

        @Override
        public cell.core.talk.dialect.ActionDialect invokeUnit(cube.common.entity.AIGCUnit unit,
                String action, JSONObject data, long timeoutMs) {
            return null;
        }

        @Override
        public cube.common.entity.FileLabel getFile(String domain, String fileCode) {
            return null;
        }

        @Override
        public File loadFile(String domain, String fileCode) {
            return null;
        }

        @Override
        public cube.common.entity.FileLabel saveFile(cube.auth.AuthToken token, String fileCode, File file,
                String filename, boolean deleteAfterSave) {
            return null;
        }

        @Override
        public cube.common.entity.FileLabel deleteFile(String domain, String fileCode) {
            return null;
        }

        @Override
        public File getModuleWorkingPath(String moduleName) {
            return null;
        }

        @Override
        public String readModuleResource(String moduleName, String relativePath) {
            return null;
        }

        @Override
        public String getGuidePrompt(String promptName) {
            return null;
        }

        @Override
        public cube.common.entity.FileLabel saveFileWithContext(cube.auth.AuthToken token, String fileCode,
                java.io.File file, String filename, boolean deleteAfterSave, org.json.JSONObject context) {
            return null;
        }

        @Override
        public java.io.File convertWavToMp3(String wavFileName, String mp3FileName) {
            return null;
        }

        @Override
        public cube.common.entity.User getUserById(long uid) {
            return null;
        }

        @Override
        public java.util.List<cube.aigc.text.Keyword> extractWeightedKeywords(String content, int topN) {
            return null;
        }

        @Override
        public cube.common.entity.AIGCUnit selectUnitForContact(String capabilityName, long contactId) {
            return null;
        }

        @Override
        public cube.common.entity.AIGCChannel createChannelByCode(String tokenCode, String participant,
                String channelCode, cube.common.Language language) {
            return null;
        }

        @Override
        public cube.common.entity.User getUser(String tokenCode) {
            return null;
        }

        @Override
        public cube.common.entity.AIGCUnit selectIdleUnit(String capabilityName) {
            return null;
        }

        @Override
        public cube.common.entity.AIGCChannel getChannel(String channelCode) {
            return null;
        }

        @Override
        public cube.common.entity.AIGCChannel createChannel(cube.auth.AuthToken authToken, String participant,
                String channelCode, cube.common.Language language) {
            return null;
        }

        @Override
        public void generateText(cube.common.entity.AIGCChannel channel, cube.common.entity.AIGCUnit unit,
                String query, String prompt, cube.common.entity.GeneratingOption option,
                java.util.List<cube.common.entity.GeneratingRecord> histories, int maxHistories,
                java.util.List<cube.aigc.complex.attachment.Attachment> attachments,
                java.util.List<String> categories, boolean recordable,
                cube.aigc.listener.GenerateTextListener listener) {
        }

        @Override
        public cube.common.entity.FileLabel performSpeakerDiarization(cube.auth.AuthToken authToken,
                cube.common.entity.FileLabel fileLabel, boolean preprocess, boolean storage, boolean jumpToFirst,
                cube.aigc.listener.VoiceDiarizationListener listener) {
            return null;
        }

        @Override
        public void writeChatHistory(cube.common.entity.AIGCChatHistory history) {
        }

        @Override
        public java.util.List<cube.common.entity.AIGCChatHistory> readChatHistories(long contactId, String domain,
                long startTime, long endTime) {
            return null;
        }

        @Override
        public cube.common.entity.Chart readLastChart(String name) {
            return null;
        }

        @Override
        public boolean insertChart(cube.common.entity.Chart chart) {
            return false;
        }

        @Override
        public boolean writeCounselingRecording(cube.auth.AuthToken authToken, String streamName, long timestamp,
                long duration, cube.aigc.psychology.Attribute attribute,
                cube.aigc.psychology.consultation.ConsultationTheme theme, String fileCode) {
            return false;
        }

        @Override
        public java.io.File getWorkingPath() {
            return null;
        }

        @Override
        public boolean semanticSearch(String query, cube.aigc.SemanticSearchListener listener) {
            return false;
        }

        @Override
        public String generatePersonalKnowledge(String tokenCode, String query, boolean english) {
            return null;
        }

        @Override
        public boolean matchSimilarity(cube.common.entity.FileLabel fileLabel,
                java.util.List<String> templateNames,
                cube.aigc.cv.MatchSimilarityListener listener) {
            return false;
        }

        @Override
        public cube.common.entity.FileLabel downloadFile(cube.auth.AuthToken authToken, String fileUrl) {
            return null;
        }

        @Override
        public cube.common.entity.Contact getContact(String tokenCode) {
            return null;
        }

        @Override
        public cube.common.entity.Membership getMembership(String domain, long contactId, int state) {
            return null;
        }

        @Override
        public int getRemainingUsages(cube.common.entity.User user, cube.common.entity.Membership membership) {
            return 0;
        }

        @Override
        public cube.aigc.psychology.ReportPermission allowPredictPainting(String domain,
                cube.common.entity.User user, long reportSn) {
            return null;
        }

        @Override
        public cube.aigc.psychology.PaintingReport generatePaintingReport(cube.common.entity.AIGCChannel channel,
                cube.aigc.psychology.Attribute attribute, cube.common.entity.FileLabel fileLabel,
                cube.aigc.psychology.Theme theme, int maxIndicators, boolean adjust, int retention,
                String remark, cube.aigc.psychology.listener.PaintingReportListener listener) {
            return null;
        }

        @Override
        public cube.aigc.psychology.ScaleReport generateScaleReport(cube.common.entity.AIGCChannel channel,
                cube.aigc.psychology.composition.Scale scale, cube.common.Language language,
                cube.aigc.psychology.listener.ScaleReportListener listener) {
            return null;
        }

        @Override
        public cube.aigc.psychology.composition.Scale getScale(long sn) {
            return null;
        }

        @Override
        public cube.core.Storage openModuleStorage(String storageName, cube.storage.StorageType type,
                JSONObject config) {
            return null;
        }

        @Override
        public List<cube.aigc.Usage> queryUsages(long contactId) {
            return new ArrayList<>();
        }

        @Override
        public List<cube.aigc.AppEvent> queryAppEvents(long contactId, String eventName, int limit) {
            return new ArrayList<>();
        }

        @Override
        public void schedule(String taskKey, long delayMs, Runnable job) {
        }

        @Override
        public void fireHook(String hookKey, AIGCPluginContextLite context) {
        }

        @Override
        public boolean fireEvent(cube.aigc.AppEvent appEvent) {
            return false;
        }

        @Override
        public void writeUsage(cube.aigc.TaskDescriptor descriptor) {
        }

        @Override
        public <T> T getSiblingModule(String moduleName, Class<T> type) {
            return null;
        }
    }

    }