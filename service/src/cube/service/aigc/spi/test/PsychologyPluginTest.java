/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.spi.test;

import cell.core.talk.dialect.ActionDialect;
import cube.aigc.psychology.app.Customer;
import cube.aigc.spi.AIGCPluginContextLite;
import cube.aigc.spi.AIGCHost;
import cube.aigc.spi.AIGCSPI;
import cube.aigc.spi.ActionBinding;
import cube.aigc.spi.ActionContext;
import cube.aigc.spi.ActionModule;
import cube.aigc.spi.ModuleDescriptor;
import cube.auth.AuthToken;
import cube.common.Language;
import cube.common.Packet;
import cube.common.entity.ObjectInfo;
import cube.common.state.AIGCStateCode;
import cube.core.Storage;
import cube.service.aigc.spi.ModuleRegistry;
import cube.service.psychology.PsychologyModule;
import cube.service.psychology.scene.PsychologyScene;
import cube.storage.StorageType;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 心理学插件的装载与动作契约验证。
 *
 * <p>覆盖绘画标签与报告状态三个动作：{@code setPaintingReportState} /
 * {@code getPaintingLabel} / {@code setPaintingLabel}。重点验证
 * <b>状态码序列</b>——这是「应答逐字节一致」的前提。</p>
 *
 * <p><b>运行前提</b>：模块注册表以相对路径 {@code config/xxx} 定位配置，
 * 而 {@code System.setProperty("user.dir")} 对 {@code File} 的相对路径解析
 * 在部分 JDK 上不生效，因此本用例<b>必须在本进程的工作目录内运行</b>
 * （由启动脚本 cd 到临时目录后再执行 java），且工作目录下须存在
 * {@code config/} 目录。缺目录时以退出码 2 终止，避免产生假失败。</p>
 */
public class PsychologyPluginTest {

    /**
     * 断言计数。
     */
    private static int passed = 0;

    /**
     * 失败计数。
     */
    private static int failed = 0;

    public static void main(String[] args) throws Exception {
        File configDir = new File("config");
        if (!configDir.isDirectory()) {
            System.out.println("[FATAL] Working directory must contain a 'config' directory: "
                    + new File(".").getAbsolutePath());
            System.exit(2);
        }

        File config = new File(configDir, "aigc-modules.properties");
        // 与 PsychologyModule#readConfig 的查找顺序一致：本地实际配置优先于模板。
        // 写local 而不是已废弃的 psychology.json，否则模块会读到未定制的模板而拒绝装载。
        File storage = new File(configDir, "psychology.local.json");

        try {
            // 用 SQLite 避免依赖真实 MySQL
            write(storage, "{\"storage\":{\"type\":\"SQLite\",\"file\":\"./psychology-test.db\"}}");

            // 场景1：零模块（出厂状态）——动作必须全部落回既有 else-if 分支
            write(config, "# zero module");

            ActionRouterFixture router = new ActionRouterFixture();
            ModuleRegistry registry = new ModuleRegistry(router.router);
            registry.setHost(new NoopHost());
            int count = registry.load();
            assertTrue("S1 零模块时不装载任何插件", 0 == count);
            assertTrue("S1 路由表为空", 0 == router.router.size());
            assertTrue("S1 三个动作均未绑定",
                    !router.router.isBound("setPaintingReportState")
                            && !router.router.isBound("getPaintingLabel")
                            && !router.router.isBound("setPaintingLabel"));
            assertTrue("S1 无阻塞失败", !registry.hasBlockingFailure());

            // 场景2：启用插件——装载成功且绑定三个动作
            write(config, "module.1.class=cube.service.psychology.PsychologyModule\nmodule.1.enabled=true");

            router = new ActionRouterFixture();
            registry = new ModuleRegistry(router.router);
            // 注入带令牌的宿主：量表族的 listScales 经模块持有的 host 解析联系人，
            // 若此处给的是「令牌恒为 null」的宿主，它会一律走 IllegalOperation 分支
            registry.setHost(newRegistryHost());
            count = registry.load();
            assertTrue("S2 成功装载1 个插件", 1 == count);
            // 动作总数可扩充，此处只断言「全部动作均已绑定」
            assertTrue("S2 绑定数至少为 22", router.router.size() >= 22);
            assertTrue("S2 setPaintingReportState 已绑定", router.router.isBound("setPaintingReportState"));
            assertTrue("S2 getPaintingLabel 已绑定", router.router.isBound("getPaintingLabel"));
            assertTrue("S2 setPaintingLabel 已绑定", router.router.isBound("setPaintingLabel"));
            assertTrue("S2 appQueryCustomer 已绑定", router.router.isBound("appQueryCustomer"));
            assertTrue("S2 无阻塞失败", !registry.hasBlockingFailure());

            ActionModule module = registry.findModule("psychology");
            assertTrue("S2 可按名查找模块", null != module);
            if (null == module) {
                // 装载失败时后续断言无意义
                report();
                return;
            }

            // 场景3：描述符合规性
            ModuleDescriptor descriptor = module.getDescriptor();
            assertTrue("S3 模块名与描述符名一致", module.getName().equals(descriptor.name));
            assertTrue("S3 SPI 版本一致", descriptor.spiVersion == AIGCSPI.VERSION);
            assertTrue("S3 不声明必需单元能力", descriptor.requiredCapabilities.isEmpty());
            assertTrue("S3 声明为非可选模块", !descriptor.optional);

            // 场景4：三个绑定的 requiresToken 必须为 false
            //（取 true 会把「无令牌」应答码从 NoToken 改成 InvalidParameter，并新增
            //  InconsistentToken 拦截，属线协议可见的语义变更）
            // 标签与状态类绑定的 requiresToken 必须为 false
            //（取 true 会把「无令牌」应答码从 NoToken 改成 InvalidParameter，并新增
            //  InconsistentToken 拦截，属线协议可见的语义变更）
            // CRUD 8 个刻意为 true：这些动作本就校验令牌有效性。
            // 此处按 action 名前缀判定，不写死数量，以便动作扩充时本断言依然有效。
            boolean firstBatchOk = true;
            for (ActionBinding binding : module.getActions()) {
                boolean isPainting = binding.action.startsWith("set") || binding.action.startsWith("get");
                if (isPainting && binding.requiresToken) {
                    firstBatchOk = false;
                }
            }
            assertTrue("S4 首批（绘画标签/报告状态）绑定的 requiresToken 均为 false", firstBatchOk);
            assertTrue("S4 绑定数量至少为 22", module.getActions().size() >= 22);

            // 场景5：状态码契约
            ActionDialect setState = dialect("setPaintingReportState", "t1", "{\"sn\":1,\"state\":1}");
            RecordingContext ctx = new RecordingContext(setState, module);
            AIGCStateCode code = router.router.lookup("setPaintingReportState").getBinding().task.handle(ctx);
            assertTrue("S5 正常写入返回 Ok", AIGCStateCode.Ok == code);
            assertTrue("S5 应答含 sn", 1 == ctx.data.getLong("sn"));
            assertTrue("S5 应答含 state", 1 == ctx.data.getInt("state"));

            // 缺 state 参数 → InvalidParameter
            ctx = new RecordingContext(dialect("setPaintingReportState", "t1", "{\"sn\":1}"), module);
            code = router.router.lookup("setPaintingReportState").getBinding().task.handle(ctx);
            assertTrue("S5 缺 state 时返回 InvalidParameter", AIGCStateCode.InvalidParameter == code);

            // 无 token 参数 → NoToken（而非 InvalidParameter，这是 requiresToken=false 的关键）
            ctx = new RecordingContext(dialect("setPaintingReportState", null, "{\"sn\":1,\"state\":1}"), module);
            code = router.router.lookup("setPaintingReportState").getBinding().task.handle(ctx);
            assertTrue("S5 无 token 时返回 NoToken", AIGCStateCode.NoToken == code);

            // 无 data → InvalidParameter
            ctx = new RecordingContext(dialect("setPaintingReportState", "t1", null), module);
            code = router.router.lookup("setPaintingReportState").getBinding().task.handle(ctx);
            assertTrue("S5 无 data 时返回 InvalidParameter", AIGCStateCode.InvalidParameter == code);

            // getPaintingLabel：无 data → InvalidParameter
            ctx = new RecordingContext(dialect("getPaintingLabel", "t1", null), module);
            code = router.router.lookup("getPaintingLabel").getBinding().task.handle(ctx);
            assertTrue("S5 getPaintingLabel 无 data 时返回 InvalidParameter",
                    AIGCStateCode.InvalidParameter == code);

            // setPaintingLabel：labels 为空数组 → Ok（清空语义）
            ctx = new RecordingContext(dialect("setPaintingLabel", "t1", "{\"sn\":1,\"labels\":[]}"), module);
            code = router.router.lookup("setPaintingLabel").getBinding().task.handle(ctx);
            assertTrue("S5 setPaintingLabel 空列表返回 Ok", AIGCStateCode.Ok == code);
            assertTrue("S5 setPaintingLabel 应答含 sn", 1 == ctx.data.getLong("sn"));

            // setPaintingLabel：labels 元素缺必填字段 → InvalidParameter
            ctx = new RecordingContext(dialect("setPaintingLabel", "t1",
                    "{\"sn\":1,\"labels\":[{\"timestamp\":1}]}"), module);
            code = router.router.lookup("setPaintingLabel").getBinding().task.handle(ctx);
            assertTrue("S5 setPaintingLabel 元素字段缺失返回 InvalidParameter",
                    AIGCStateCode.InvalidParameter == code);

            // 场景6：无效 token 不被拦截（锁定「只校验存在性」这一既有语义）
            // 若未来有人把 requiresToken 改成 true，此断言会失败
            ctx = new RecordingContext(
                    dialect("setPaintingReportState", "an-invalid-token", "{\"sn\":1,\"state\":0}"), module);
            code = router.router.lookup("setPaintingReportState").getBinding().task.handle(ctx);
            assertTrue("S6 无效 token 不被拦截（不返回 InconsistentToken）",
                    AIGCStateCode.InconsistentToken != code);
            assertTrue("S6 无效 token 时正常执行", AIGCStateCode.Ok == code);

            // ── 客户/日程 CRUD ──
            // 注：本段必须放在「场景7 卸载」之前——卸载会清空路由表与模块注册表，
            //     之后的 findModule 与 isBound 断言都将失去被测对象。

            // S10 绑定元数据：11 个绑定、令牌要求分批、action 名与线协议一致
            List<cube.aigc.spi.ActionBinding> bindings = module.getActions();
            assertTrue("S10 绑定总数至少为 22", bindings.size() >= 22);

            int trueCount = 0;
            int falseCount = 0;
            for (cube.aigc.spi.ActionBinding binding : bindings) {
                if (binding.requiresToken) {
                    ++trueCount;
                }
                else {
                    ++falseCount;
                }
            }
            // true 固定为 8（仅 CRUD），其余为 false
            assertTrue("S10 其中 8 个requiresToken=true", 8 == trueCount);
            assertTrue("S10 其中 15 个 requiresToken=false", 15 == falseCount);

            for (String name : new String[] {"appQueryCustomer", "appNewCustomer",
                    "appUpdateCustomer", "appDeleteCustomer", "appQuerySchedule",
                    "appNewSchedule", "appUpdateSchedule", "appDeleteSchedule"}) {
                assertTrue("S10 " + name + " 已绑定", router.router.isBound(name));
            }

            // action 名与线协议枚举逐字符比对（防手写拼写漂移）
            assertTrue("S10 appQueryCustomer 与枚举一致",
                    cube.common.action.AIGCAction.AppQueryCustomer.name.equals("appQueryCustomer"));
            assertTrue("S10 appNewCustomer 与枚举一致",
                    cube.common.action.AIGCAction.AppNewCustomer.name.equals("appNewCustomer"));
            assertTrue("S10 appUpdateCustomer 与枚举一致",
                    cube.common.action.AIGCAction.AppUpdateCustomer.name.equals("appUpdateCustomer"));
            assertTrue("S10 appDeleteCustomer 与枚举一致",
                    cube.common.action.AIGCAction.AppDeleteCustomer.name.equals("appDeleteCustomer"));
            assertTrue("S10 appQuerySchedule 与枚举一致",
                    cube.common.action.AIGCAction.AppQuerySchedule.name.equals("appQuerySchedule"));
            assertTrue("S10 appNewSchedule 与枚举一致",
                    cube.common.action.AIGCAction.AppNewSchedule.name.equals("appNewSchedule"));
            assertTrue("S10 appUpdateSchedule 与枚举一致",
                    cube.common.action.AIGCAction.AppUpdateSchedule.name.equals("appUpdateSchedule"));
            assertTrue("S10 appDeleteSchedule 与枚举一致",
                    cube.common.action.AIGCAction.AppDeleteSchedule.name.equals("appDeleteSchedule"));

            // 准备两种身份的上下文
            AuthToken regToken = token("t-reg", 1001L);
            AuthToken unregToken = token("t-unreg", 2002L);
        NoopHost regHost = new NoopHost();
        regHost.registered = true;
        regHost.resolvedToken = regToken;
        NoopHost unregHost = new NoopHost();
        unregHost.registered = false;
        unregHost.resolvedToken = unregToken;

            // S13 未注册用户：两个 Query 的字段名不同（本批最易错点）
            // 客户回page/size
            RecordingContext c = ctx(module, "appQueryCustomer", unregToken, unregHost,
                    "{\"page\":0,\"size\":0}");
            code = handle(router, "appQueryCustomer", c);

            // 场景9：并发复用安全（handler 为无状态单例）
            final AtomicInteger nonOk = new AtomicInteger(0);
            final cube.aigc.spi.ActionRouter target = router.router;
            final ActionModule targetModule = module;
            List<Thread> threads = new ArrayList<>();
            for (int i = 0; i < 8; ++i) {
                threads.add(new Thread(() -> {
                    for (int j = 0; j < 50; ++j) {
                        RecordingContext concurrencyCtx = new RecordingContext(
                                dialect("setPaintingReportState", "t1", "{\"sn\":1,\"state\":1}"),
                                targetModule);
                        AIGCStateCode r = target.lookup("setPaintingReportState")
                                .getBinding().task.handle(concurrencyCtx);
                        if (AIGCStateCode.Ok != r) {
                            nonOk.incrementAndGet();
                        }
                    }
                }));
            }

            for (Thread t : threads) {
                t.start();
            }
            for (Thread t : threads) {
                try {
                    t.join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            assertTrue("S9 并发 400 次调用结果恒为 Ok", 0 == nonOk.get());

            // ── 量表 ──
            // S21 绑定元数据：4 个量表族动作已绑定，且 requiresToken 均为 false
            assertTrue("S21 绑定总数为 23", 23 == router.router.size());
            assertTrue("S21 listPsychologyScales 已绑定", router.router.isBound("listPsychologyScales"));
            assertTrue("S21 getPsychologyScale 已绑定", router.router.isBound("getPsychologyScale"));
            assertTrue("S21 generatePsychologyScale 已绑定", router.router.isBound("generatePsychologyScale"));
            assertTrue("S21 submitPsychologyAnswerSheet 已绑定", router.router.isBound("submitPsychologyAnswerSheet"));
            assertTrue("S21 四个量表绑定的 requiresToken 均为 false",
                    !router.router.lookup("listPsychologyScales").getBinding().requiresToken
                            && !router.router.lookup("getPsychologyScale").getBinding().requiresToken
                            && !router.router.lookup("generatePsychologyScale").getBinding().requiresToken
                            && !router.router.lookup("submitPsychologyAnswerSheet").getBinding().requiresToken);

            // S22 令牌分支：4 个动作均以 NoToken 起手（而非 InvalidParameter）
            AIGCStateCode r = handle(router, "listPsychologyScales",
                    ctx(module, "listPsychologyScales", null, null, null));
            assertTrue("S22 listScales 无令牌回 NoToken", AIGCStateCode.NoToken == r);

            r = handle(router, "getPsychologyScale",
                    ctx(module, "getPsychologyScale", null, null, "{\"sn\":1}"));
            assertTrue("S22 getScale 无令牌回 NoToken", AIGCStateCode.NoToken == r);

            r = handle(router, "generatePsychologyScale",
                    ctx(module, "generatePsychologyScale", null, null, "{\"name\":\"x\"}"));
            assertTrue("S22 generateScale 无令牌回 NoToken", AIGCStateCode.NoToken == r);

            r = handle(router, "submitPsychologyAnswerSheet",
                    ctx(module, "submitPsychologyAnswerSheet", null, null, "{\"scaleSn\":1}"));
            assertTrue("S22 submitSheet 无令牌回 NoToken", AIGCStateCode.NoToken == r);

            // S23 令牌无效：4 个动作均回 IllegalOperation
            AIGCHost nilHost = new NilTokenHost();
            r = handle(router, "listPsychologyScales",
                    ctx(module, "listPsychologyScales", regToken, nilHost, null));
            assertTrue("S23 listScales 令牌无效回 IllegalOperation", AIGCStateCode.IllegalOperation == r);

            r = handle(router, "getPsychologyScale",
                    ctx(module, "getPsychologyScale", regToken, nilHost, "{\"sn\":1}"));
            assertTrue("S23 getScale 令牌无效回 IllegalOperation", AIGCStateCode.IllegalOperation == r);

            r = handle(router, "generatePsychologyScale",
                    ctx(module, "generatePsychologyScale", regToken, nilHost, "{\"name\":\"x\"}"));
            assertTrue("S23 generateScale 令牌无效回 IllegalOperation", AIGCStateCode.IllegalOperation == r);

            r = handle(router, "submitPsychologyAnswerSheet",
                    ctx(module, "submitPsychologyAnswerSheet", regToken, nilHost, "{\"scaleSn\":1}"));
            assertTrue("S23 submitSheet 令牌无效回 IllegalOperation", AIGCStateCode.IllegalOperation == r);

            // S24 getScale 读参数：sn 不存在时回 Failure 且回显原始请求体
            RecordingContext c24 = ctx(module, "getPsychologyScale", regToken, regHost, "{\"sn\":999999}");
            r = handle(router, "getPsychologyScale", c24);
            assertTrue("S24 getScale 量表不存在回 Failure", AIGCStateCode.Failure == r);
            assertTrue("S24 getScale Failure 回显原始请求体", 999999 == c24.data.getLong("sn"));

            // S24 getScale 走 name 分支：name 不存在同样回 Failure
            c24 = ctx(module, "getPsychologyScale", regToken, regHost, "{\"name\":\"no-such-scale\"}");
            r = handle(router, "getPsychologyScale", c24);
            assertTrue("S24 getScale 按名查不到回 Failure", AIGCStateCode.Failure == r);

            // S24 getScale 无 sn 无 name：scale 保持 null，回 Failure
            c24 = ctx(module, "getPsychologyScale", regToken, regHost, "{}");
            r = handle(router, "getPsychologyScale", c24);
            assertTrue("S24 getScale 无 sn 无 name 回 Failure", AIGCStateCode.Failure == r);

            // S25 generateScale 缺必填参数：name 缺失触发异常，落入 InvalidParameter
            r = handle(router, "generatePsychologyScale",
                    ctx(module, "generatePsychologyScale", regToken, regHost, "{\"gender\":\"male\",\"age\":20}"));
            assertTrue("S25 generateScale 缺 name 回 InvalidParameter",
                    AIGCStateCode.InvalidParameter == r);

            // S25 generateScale 缺 gender：为无默认值必填项，同样落 InvalidParameter
            r = handle(router, "generatePsychologyScale",
                    ctx(module, "generatePsychologyScale", regToken, regHost, "{\"name\":\"x\",\"age\":20}"));
            assertTrue("S25 generateScale 缺 gender 回 InvalidParameter",
                    AIGCStateCode.InvalidParameter == r);

            // S25 generateScale 量表定义不存在：回 Failure 且回显原始请求体
            c24 = ctx(module, "generatePsychologyScale", regToken, regHost,
                    "{\"name\":\"no-such-scale\",\"gender\":\"male\",\"age\":20}");
            r = handle(router, "generatePsychologyScale", c24);
            assertTrue("S25 generateScale 量表不存在回 Failure", AIGCStateCode.Failure == r);
            assertTrue("S25 generateScale Failure 回显原始请求体",
                    "no-such-scale".equals(c24.data.getString("name")));

            // S26 submitSheet 量表不存在：回 Failure 且回显原始请求体
            // 注意 AnswerSheet 的 JSON 字段名是 sn（不是 scaleSn）
            c24 = ctx(module, "submitPsychologyAnswerSheet", regToken, regHost,
                    "{\"sn\":999999,\"answers\":[]}");
            r = handle(router, "submitPsychologyAnswerSheet", c24);
            assertTrue("S26 submitSheet 量表不存在回 Failure", AIGCStateCode.Failure == r);
            assertTrue("S26 submitSheet Failure 回显原始请求体", 999999 == c24.data.getLong("sn"));

            // S26 submitSheet 请求体非法：缺 sn 时 AnswerSheet 构造抛异常，落 InvalidParameter
            r = handle(router, "submitPsychologyAnswerSheet",
                    ctx(module, "submitPsychologyAnswerSheet", regToken, regHost, "{\"bad\":1}"));
            assertTrue("S26 submitSheet 非法请求体回 InvalidParameter",
                    AIGCStateCode.InvalidParameter == r);

            // S27 extractKeywords 边界：空文本与非法 topN 均返回空列表而非抛异常
            AIGCHost kwHost = new KeywordHost();
            assertTrue("S27 extractKeywords 空文本返回空列表",
                    kwHost.extractKeywords(null, 3).isEmpty());
            assertTrue("S27 extractKeywords 空串返回空列表",
                    kwHost.extractKeywords("", 3).isEmpty());
            assertTrue("S27 extractKeywords topN<=0 返回空列表",
                    kwHost.extractKeywords("测试文本", 0).isEmpty());
            assertTrue("S27 extractKeywords 正常文本返回关键词",
                    !kwHost.extractKeywords("测试文本内容", 3).isEmpty());

            // S28 四个量表动作并发复用安全（处理器为无状态单例）
            final AtomicInteger scaleNonOk = new AtomicInteger(0);
            final ActionModule scaleModule = module;
            final ActionRouterFixture scaleRouter = router;
            List<Thread> scaleThreads = new ArrayList<>();
            for (int i = 0; i < 4; ++i) {
                scaleThreads.add(new Thread(() -> {
                    for (int j = 0; j < 30; ++j) {
                        RecordingContext sc = new RecordingContext(
                                dialect("getPsychologyScale", "t-reg", "{\"sn\":999999}"),
                                scaleModule, regToken, regHost);
                        AIGCStateCode rr = scaleRouter.router.lookup("getPsychologyScale")
                                .getBinding().task.handle(sc);
                        if (AIGCStateCode.Failure != rr) {
                            scaleNonOk.incrementAndGet();
                        }
                    }
                }));
            }
            for (Thread thread : scaleThreads) {
                thread.start();
            }
            for (Thread thread : scaleThreads) {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            assertTrue("S28 量表动作并发 120 次调用结果恒为 Failure", 0 == scaleNonOk.get());

            // ── 报告读取与控制 ──
            // S29 绑定元数据：3 个报告动作已绑定，requiresToken 均为 false
            assertTrue("S29 stopGeneratingPsychologyReport 已绑定",
                    router.router.isBound("stopGeneratingPsychologyReport"));
            assertTrue("S29 getPsychologyReport 已绑定", router.router.isBound("getPsychologyReport"));
            assertTrue("S29 resetReportAttention 已绑定", router.router.isBound("resetReportAttention"));
            assertTrue("S29 三个报告绑定的 requiresToken 均为 false",
                    !router.router.lookup("stopGeneratingPsychologyReport").getBinding().requiresToken
                            && !router.router.lookup("getPsychologyReport").getBinding().requiresToken
                            && !router.router.lookup("resetReportAttention").getBinding().requiresToken);

            // S30 三者均以 NoToken 起手（只判方言里有无 token 参数）
            assertTrue("S30 stopReport 无令牌回 NoToken",
                    AIGCStateCode.NoToken == handle(router, "stopGeneratingPsychologyReport",
                            ctx(module, "stopGeneratingPsychologyReport", null, null, "{\"sn\":1}")));
            assertTrue("S30 getReport 无令牌回 NoToken",
                    AIGCStateCode.NoToken == handle(router, "getPsychologyReport",
                            ctx(module, "getPsychologyReport", null, null, "{\"sn\":1}")));
            assertTrue("S30 resetAttention 无令牌回 NoToken",
                    AIGCStateCode.NoToken == handle(router, "resetReportAttention",
                            ctx(module, "resetReportAttention", null, null, "{\"sn\":1}")));

            // S31 令牌无效：stopReport 与 getReport 回 IllegalOperation；
            //     但 resetAttention 不校验令牌有效性，绝不能被拦截。
            //     用 ReportHost（保留报告数据能力）但把 resolvedToken 置空来模拟「令牌无效」，
            //     不可用 NilTokenHost —— 那会把报告数据能力一并抹掉，测的就不是令牌分支了
            ReportHost rh = new ReportHost();
            ReportHost invalidButWithReports = new ReportHost();
            invalidButWithReports.resolvedToken = null;
            //rh.paintingSn 供后续 S32-S34 复用

            assertTrue("S31 stopReport 令牌无效回 IllegalOperation",
                    AIGCStateCode.IllegalOperation == handle(router, "stopGeneratingPsychologyReport",
                            ctx(module, "stopGeneratingPsychologyReport", regToken, new NilTokenHost(),
                                    "{\"sn\":" + rh.paintingSn + "}")));
            assertTrue("S31 getReport 令牌无效回 IllegalOperation",
                    AIGCStateCode.IllegalOperation == handle(router, "getPsychologyReport",
                            ctx(module, "getPsychologyReport", regToken, new NilTokenHost(), "{\"sn\":1}")));
            // 这一条是本批最易错点：若处理器擅自加令牌校验，此处会回 IllegalOperation
            RecordingContext c31 = ctx(module, "resetReportAttention", regToken, invalidButWithReports,
                    "{\"sn\":" + rh.paintingSn + "}");
            assertTrue("S31 resetAttention 不校验令牌有效性（仍回 Ok）",
                    AIGCStateCode.Ok == handle(router, "resetReportAttention", c31));

            // S32 stopReport：命中回 Ok，未命中回 Failure 且回显原始请求体
            RecordingContext c32 = ctx(module, "stopGeneratingPsychologyReport", regToken, rh,
                    "{\"sn\":" + rh.paintingSn + "}");
            assertTrue("S32 stopReport 命中回 Ok",
                    AIGCStateCode.Ok == handle(router, "stopGeneratingPsychologyReport", c32));
            assertTrue("S32 stopReport Ok 应答含 sn", rh.paintingSn == c32.data.getLong("sn"));

            c32 = ctx(module, "stopGeneratingPsychologyReport", regToken, rh, "{\"sn\":999}");
            assertTrue("S32 stopReport 未命中回 Failure",
                    AIGCStateCode.Failure == handle(router, "stopGeneratingPsychologyReport", c32));
            assertTrue("S32 stopReport Failure 回显原始请求体", 999 == c32.data.getLong("sn"));

            // S32 stopReport 缺 sn：packet.data.getLong 抛异常 → InvalidParameter
            assertTrue("S32 stopReport 缺 sn 回 InvalidParameter",
                    AIGCStateCode.InvalidParameter == handle(router, "stopGeneratingPsychologyReport",
                            ctx(module, "stopGeneratingPsychologyReport", regToken, rh, "{}")));

            // S33 getReport 四分支
            // ① sn 命中绘画报告，默认 compact 形态应带 queuePosition
            RecordingContext c33 = ctx(module, "getPsychologyReport", regToken, rh,
                    "{\"sn\":" + rh.paintingSn + "}");
            assertTrue("S33 getReport sn 命中回 Ok",
                    AIGCStateCode.Ok == handle(router, "getPsychologyReport", c33));
            assertTrue("S33 compact 形态带 queuePosition", 2 == c33.data.getInt("queuePosition"));

            // ② markdown 形态不应带 queuePosition，且 format 已正确透传
            c33 = ctx(module, "getPsychologyReport", regToken, rh,
                    "{\"sn\":" + rh.paintingSn + ",\"markdown\":true}");
            assertTrue("S33 markdown 形态回 Ok",
                    AIGCStateCode.Ok == handle(router, "getPsychologyReport", c33));
            assertTrue("S33 markdown 形态已透传 format", "markdown".equals(c33.data.getString("format")));
            assertTrue("S33 markdown 形态不带 queuePosition", !c33.data.has("queuePosition"));

            // ③ sn 未命中绘画报告 → 回退命中量表报告
            c33 = ctx(module, "getPsychologyReport", regToken, rh, "{\"sn\":" + rh.scaleSn + "}");
            assertTrue("S33 getReport 回退命中量表报告回 Ok",
                    AIGCStateCode.Ok == handle(router, "getPsychologyReport", c33));
            assertTrue("S33 量表报告应答含 sn", rh.scaleSn == c33.data.getLong("sn"));

            // ④ 两者都未命中 → Failure 且回显请求体
            c33 = ctx(module, "getPsychologyReport", regToken, rh, "{\"sn\":999}");
            assertTrue("S33 getReport 全未命中回 Failure",
                    AIGCStateCode.Failure == handle(router, "getPsychologyReport", c33));
            assertTrue("S33 getReport Failure 回显原始请求体", 999 == c33.data.getLong("sn"));

            // S34 getReport 列表分支：painting 填 pageSize，scale 填总条数（既有缺陷）
            c33 = ctx(module, "getPsychologyReport", regToken, rh, "{\"size\":5,\"page\":2}");
            assertTrue("S33 painting 列表分支回 Ok",
                    AIGCStateCode.Ok == handle(router, "getPsychologyReport", c33));
            assertTrue("S33 painting 列表 size 为 pageSize", 5 == c33.data.getInt("size"));
            assertTrue("S33 painting 列表 page 已回填", 2 == c33.data.getInt("page"));
            assertTrue("S33 painting 列表 type 已回填", "painting".equals(c33.data.getString("type")));

            c33 = ctx(module, "getPsychologyReport", regToken, rh, "{\"size\":5,\"type\":\"scale\"}");
            assertTrue("S33 scale 列表分支回 Ok",
                    AIGCStateCode.Ok == handle(router, "getPsychologyReport", c33));
            // ⚠️ 锁既有缺陷：L166 填的是 num（总条数）而非 pageSize，勿「顺手修正」
            assertTrue("S33 scale 列表 size 为总条数（既有缺陷，勿修）",
                    1 == c33.data.getInt("size"));

            // S33 sn==0 且 size==0 → InvalidParameter 且回显请求体
            c33 = ctx(module, "getPsychologyReport", regToken, rh, "{\"size\":0}");
            assertTrue("S33 sn与size 均为 0 回 InvalidParameter",
                    AIGCStateCode.InvalidParameter == handle(router, "getPsychologyReport", c33));
            assertTrue("S33 该分支回显原始请求体", 0 == c33.data.getInt("size"));

            // S34 resetAttention：Failure 回空对象（不回显），catch 回 IllegalOperation
            RecordingContext c34 = ctx(module, "resetReportAttention", regToken, rh, "{\"sn\":999}");
            assertTrue("S34 resetAttention 报告不存在回 Failure",
                    AIGCStateCode.Failure == handle(router, "resetReportAttention", c34));
            assertTrue("S34 resetAttention Failure 回空对象", null != c34.data && c34.data.isEmpty());

            // 缺 sn → InvalidParameter（用 has("sn") 判定，与参数类型非法区分）
            assertTrue("S34 resetAttention 缺 sn 回 InvalidParameter",
                    AIGCStateCode.InvalidParameter == handle(router, "resetReportAttention",
                            ctx(module, "resetReportAttention", regToken, rh, "{}")));

            // sn 类型非法 → JSONException 落入 catch → IllegalOperation（本组唯一）
            assertTrue("S34 resetAttention sn 类型非法回 IllegalOperation",
                    AIGCStateCode.IllegalOperation == handle(router, "resetReportAttention",
                            ctx(module, "resetReportAttention", regToken, rh, "{\"sn\":\"abc\"}")));

            // 缺 attention 时传 null，宿主据此回滚到滚动建议
            c34 = ctx(module, "resetReportAttention", regToken, rh, "{\"sn\":" + rh.paintingSn + "}");
            assertTrue("S34 resetAttention 缺 attention 回 Ok",
                    AIGCStateCode.Ok == handle(router, "resetReportAttention", c34));
            assertTrue("S34 缺 attention 时以 null 下传（回滚语义）", -1 == c34.data.getInt("attention"));

            // 显式指定 attention 时应原样透传
            c34 = ctx(module, "resetReportAttention", regToken, rh,
                    "{\"sn\":" + rh.paintingSn + ",\"attention\":2}");
            assertTrue("S34 resetAttention 指定 attention 回 Ok",
                    AIGCStateCode.Ok == handle(router, "resetReportAttention", c34));
            assertTrue("S34 指定 attention 已透传", 2 == c34.data.getInt("attention"));

            // ── 报告内容与备注 ──
            // S35 绑定元数据
            assertTrue("S35 getPsychologyReportPart 已绑定", router.router.isBound("getPsychologyReportPart"));
            assertTrue("S35 modifyReportRemark 已绑定", router.router.isBound("modifyReportRemark"));
            assertTrue("S35 两者 requiresToken 均为 false",
                    !router.router.lookup("getPsychologyReportPart").getBinding().requiresToken
                            && !router.router.lookup("modifyReportRemark").getBinding().requiresToken);

            // S36 NoToken 起手 + 令牌无效回 IllegalOperation
            assertTrue("S36 reportPart 无令牌回 NoToken",
                    AIGCStateCode.NoToken == handle(router, "getPsychologyReportPart",
                            ctx(module, "getPsychologyReportPart", null, null, "{\"sn\":1}")));
            assertTrue("S36 modifyRemark 无令牌回 NoToken",
                    AIGCStateCode.NoToken == handle(router, "modifyReportRemark",
                            ctx(module, "modifyReportRemark", null, null, "{\"sn\":1}")));
            assertTrue("S36 reportPart 令牌无效回 IllegalOperation",
                    AIGCStateCode.IllegalOperation == handle(router, "getPsychologyReportPart",
                            ctx(module, "getPsychologyReportPart", regToken, new NilTokenHost(), "{\"sn\":1}")));
            assertTrue("S36 modifyRemark 令牌无效回 IllegalOperation",
                    AIGCStateCode.IllegalOperation == handle(router, "modifyReportRemark",
                            ctx(module, "modifyReportRemark", regToken, new NilTokenHost(), "{\"sn\":1}")));

            // S37 reportPart：报告不存在回 Failure 且**回空对象**（与 getPsychologyReport 的回显不同）
            RecordingContext c37 = ctx(module, "getPsychologyReportPart", regToken, rh, "{\"sn\":999}");
            assertTrue("S37 reportPart 报告不存在回 Failure",
                    AIGCStateCode.Failure == handle(router, "getPsychologyReportPart", c37));
            assertTrue("S37 reportPart Failure 回空对象（非回显）",
                    null != c37.data && c37.data.isEmpty());

            // S37 reportPart：sn 缺失落入 catch-all → InvalidParameter（无独立校验分支）
            assertTrue("S37 reportPart 缺 sn 回 InvalidParameter",
                    AIGCStateCode.InvalidParameter == handle(router, "getPsychologyReportPart",
                            ctx(module, "getPsychologyReportPart", regToken, rh, "{}")));

            // S37 reportPart：处理中报告只回 thought 且仍回 Ok
            RecordingContext c37b = ctx(module, "getPsychologyReportPart", regToken,
                    new ProcessingReportHost(), "{\"sn\":" + rh.paintingSn + ",\"thought\":true}");
            assertTrue("S37 reportPart 处理中回 Ok",
                    AIGCStateCode.Ok == handle(router, "getPsychologyReportPart", c37b));
            assertTrue("S37 处理中仅回 thought 字段", c37b.data.has("thought"));
            assertTrue("S37 处理中不回 content（未要求）", !c37b.data.has("content"));

            // S37 reportPart：summary 开关在 content 为真时被忽略（L80-82）
            RecordingContext c37c = ctx(module, "getPsychologyReportPart", regToken, rh,
                    "{\"sn\":" + rh.paintingSn + ",\"content\":true,\"summary\":true}");
            assertTrue("S37 reportPart content 优先于 summary 回 Ok",
                    AIGCStateCode.Ok == handle(router, "getPsychologyReportPart", c37c));
            assertTrue("S37 content 为真时 summary 被忽略", !c37c.data.has("summary"));
            assertTrue("S37 content 字段已回填", c37c.data.has("content"));

            // S38 modifyRemark：可迁入插件自建（不需 SPI），走 storage 更新
            RecordingContext c38 = ctx(module, "modifyReportRemark", regToken, regHost,
                    "{\"sn\":" + rh.paintingSn + ",\"remark\":\"测试备注\"}");
            AIGCStateCode c38code = handle(router, "modifyReportRemark", c38);
            // 报告不存在（该 sn 未入库）时回 Failure，且回空对象
            assertTrue("S38 modifyRemark 报告不存在回 Failure", AIGCStateCode.Failure == c38code);
            assertTrue("S38 modifyRemark Failure 回空对象", null != c38.data && c38.data.isEmpty());

            // S38 modifyRemark：缺 sn 或 remark → InvalidParameter
            assertTrue("S38 modifyRemark 缺 sn 回 InvalidParameter",
                    AIGCStateCode.InvalidParameter == handle(router, "modifyReportRemark",
                            ctx(module, "modifyReportRemark", regToken, regHost, "{\"remark\":\"x\"}")));
            assertTrue("S38 modifyRemark 缺 remark 回 InvalidParameter",
                    AIGCStateCode.InvalidParameter == handle(router, "modifyReportRemark",
                            ctx(module, "modifyReportRemark", regToken, regHost, "{\"sn\":1}")));

            // ── 绘画读取与校验 ──
            // S39 绑定元数据
            assertTrue("S39 getPsychologyPainting 已绑定", router.router.isBound("getPsychologyPainting"));
            assertTrue("S39 checkPsychologyPainting 已绑定", router.router.isBound("checkPsychologyPainting"));
            assertTrue("S39 两者 requiresToken 均为 false",
                    !router.router.lookup("getPsychologyPainting").getBinding().requiresToken
                            && !router.router.lookup("checkPsychologyPainting").getBinding().requiresToken);

            // S40 令牌分支：painting 第二码 IllegalOperation，而 check 第二码是 **NoToken**（本组唯一）
            assertTrue("S40 painting 无令牌回 NoToken",
                    AIGCStateCode.NoToken == handle(router, "getPsychologyPainting",
                            ctx(module, "getPsychologyPainting", null, null, "{}")));
            assertTrue("S40 check 无令牌回 NoToken",
                    AIGCStateCode.NoToken == handle(router, "checkPsychologyPainting",
                            ctx(module, "checkPsychologyPainting", null, null, "{\"fileCode\":\"f\"}")));
            assertTrue("S40 painting 令牌无效回 IllegalOperation",
                    AIGCStateCode.IllegalOperation == handle(router, "getPsychologyPainting",
                            ctx(module, "getPsychologyPainting", regToken, new NilTokenHost(), "{}")));
            // 这一条是本批最易错点：若处理器改回 IllegalOperation，此断言会红
            assertTrue("S40 check 令牌无效回 NoToken（非常规）",
                    AIGCStateCode.NoToken == handle(router, "checkPsychologyPainting",
                            ctx(module, "checkPsychologyPainting", regToken, new NilTokenHost(),
                                    "{\"fileCode\":\"f\"}")));

            // S41 painting 三分支：宿主无数据时一律 Failure 且回显请求体
            RecordingContext c41 = ctx(module, "getPsychologyPainting", regToken, regHost, "{\"chart\":true,\"sn\":1}");
            assertTrue("S41 chart 分支无数据回 Failure",
                    AIGCStateCode.Failure == handle(router, "getPsychologyPainting", c41));
            assertTrue("S41 chart 分支回显原始请求体", c41.data.getBoolean("chart"));

            c41 = ctx(module, "getPsychologyPainting", regToken, regHost, "{\"fileCode\":\"f1\"}");
            assertTrue("S41 fileCode 分支无数据回 Failure",
                    AIGCStateCode.Failure == handle(router, "getPsychologyPainting", c41));
            assertTrue("S41 fileCode 分支回显原始请求体", "f1".equals(c41.data.getString("fileCode")));

            c41 = ctx(module, "getPsychologyPainting", regToken, regHost, "{\"sn\":7}");
            assertTrue("S41 sn 分支无数据回 Failure",
                    AIGCStateCode.Failure == handle(router, "getPsychologyPainting", c41));
            assertTrue("S41 sn 分支回显原始请求体", 7 == c41.data.getLong("sn"));

            // S42 check：缺 fileCode → InvalidParameter
            assertTrue("S42 check 缺 fileCode 回 InvalidParameter",
                    AIGCStateCode.InvalidParameter == handle(router, "checkPsychologyPainting",
                            ctx(module, "checkPsychologyPainting", regToken, regHost, "{}")));

            // S42 check：无论判定结果如何都回 Ok，且 data 恒含 result 字段
            RecordingContext c42 = ctx(module, "checkPsychologyPainting", regToken, regHost,
                    "{\"fileCode\":\"f1\"}");
            AIGCStateCode c42code = handle(router, "checkPsychologyPainting", c42);
            assertTrue("S42 check 恒回 Ok（永不回 Failure）", AIGCStateCode.Ok == c42code);
            assertTrue("S42 check 应答恒含 result 字段", c42.data.has("result"));
            // 宿主无 CV 能力、无绘画单元 → 判定为非绘画
            assertTrue("S42 无能力时判为非绘画", !c42.data.getBoolean("result"));

            // S43 场景生命周期：存储唯一化 + 宿主能力注入 + 队列上限。
            // 场景未装配时 storage 与 host 恒为 null，41 处存储访问全部会 NPE。
            PsychologyScene scene = PsychologyScene.getInstance();
            assertTrue("S43 场景已装配（isReady）", scene.isReady());
            assertTrue("S43 场景与模块持有同一个 PsychologyStorage 实例",
                    scene.getStorage() == ((PsychologyModule) module).getStorageForTest());
            assertTrue("S43 队列上限取自 preference.maxQueueLength = 20",
                    20 == scene.getMaxQueueLength());

            // S28 停机卸载：放在全部动作测试之后，因为卸载会清空路由表与模块注册表
            registry.teardownAll();
            assertTrue("S7 卸载后绑定清空", 0 == router.router.size());
            assertTrue("S7 卸载后按名查找返回 null", null == registry.findModule("psychology"));
            assertTrue("S43 卸载后场景回到未装配状态", !scene.isReady());
            assertTrue("S43 卸载后场景不再持有存储", null == scene.getStorage());
        }
        finally {
            config.delete();
            storage.delete();

            File db = new File("psychology-test.db");
            if (db.exists()) {
                db.delete();
            }
        }

        report();
    }

    /**
     * 构造访问令牌。
     *
     * @param code 令牌码。
     * @param cid 联系人 ID。
     * @return 返回令牌。
     */
    private static AuthToken token(String code, long cid) {
        long now = System.currentTimeMillis();
        return new AuthToken(code, "default", "app", cid, now, now + 86400000L, false);
    }

    /**
     * 构造装载期注入的宿主能力。
     *
     * <p>模块在 {@code setup(host)} 期间保存该引用，量表族的
     * {@code listScales} 正是经它把令牌解析为联系人 ID，因此这里必须给出一个
     * <b>能解析出令牌</b>的宿主，否则该动作会一律落入
     * {@code IllegalOperation} 分支。</p>
     *
     * @return 返回带令牌的宿主能力。
     */
    private static AIGCHost newRegistryHost() {
        NoopHost host = new NoopHost();
        host.registered = true;
        host.resolvedToken = token("t-reg", 1001L);
        return host;
    }

    /**
     * 构造记录式动作上下文。
     *
     * <p><b>令牌同时注入两处</b>：<b>方言参数</b>与 <b>已解析令牌</b>。
     * 原因有两类动作：</p>
     * <ul>
     *   <li>{@code requiresToken=true} 的动作（CRUD）由宿主动作骨架
     *       前置校验令牌，处理器只读 {@code ctx.getToken()}；</li>
     *   <li>{@code requiresToken=false} 的动作在处理器内
     *       自行读方言里的令牌字符串，宿主的 {@code ActionRunner} 不参与解析。
     *       若这里不注入，CRUD 用例照旧通过，而量表类动作会一律走
     *       {@code NoToken} 分支——测试就会假绿。</li>
     * </ul>
     *
     * @param module 动作所属模块。
     * @param action 动作名。
     * @param token 已解析的令牌；传 {@code null} 表示令牌无效。
     * @param host 宿主能力接口。
     * @param data 请求体 JSON，可为 {@code null} 表示不带该参数。
     * @return 返回上下文。
     */
    private static RecordingContext ctx(ActionModule module, String action, AuthToken token,
            AIGCHost host, String data) {
        String tokenCode = (null == token) ? null : token.getCode();
        return new RecordingContext(dialect(action, tokenCode, data), module, token, host);
    }

    /**
     * 调用指定动作的处理器。
     *
     * @param router 路由器夹具。
     * @param action 动作名。
     * @param ctx 动作上下文。
     * @return 返回状态码。
     */
    private static AIGCStateCode handle(ActionRouterFixture router, String action, RecordingContext ctx) {
        return router.router.lookup(action).getBinding().task.handle(ctx);
    }

    /**
     * 输出统计并设置退出码。
     */
    private static void report() {
        System.out.println("");
        System.out.println("passed: " + passed + ", failed: " + failed);
        System.out.println(0 == failed ? "ALL PASSED" : "HAS FAILURE");

        if (0 != failed) {
            System.exit(1);
        }
    }

    /**
     * 构造请求方言。
     *
     * <p>直接用 {@code addParam} 逐个塞入参数，不走 wire 格式解析，
     * 以便精确控制「带/不带某个参数」这两类输入——而这正是本用例
     * 要验证的差异（无 token 时应答必须是 {@code NoToken}）。</p>
     *
     * @param action 动作名。
     * @param token 令牌，可为 {@code null} 表示不带该参数。
     * @param data 参数 JSON 字符串，可为 {@code null} 表示不带该参数。
     * @return 返回方言。
     */
    private static ActionDialect dialect(String action, String token, String data) {
        ActionDialect dialect = new ActionDialect(new Packet(action, new JSONObject()).toDialect());
        dialect.setName(action);
        if (null != token) {
            dialect.addParam("token", token);
        }

        if (null != data) {
            dialect.addParam("data", new JSONObject(data));
        }

        return dialect;
    }

    /**
     * 写入配置文件。
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
     * 路由器夹具，便于同时持有 registry 与 router 两个引用。
     */
    static final class ActionRouterFixture {
        final cube.aigc.spi.ActionRouter router = new cube.aigc.spi.ActionRouter();
    }

    /**
     * 记录式动作上下文：逐字模拟宿主动作骨架的应答封装，用于断言处理器行为。
     */
    static final class RecordingContext implements ActionContext {

        private final ActionDialect dialect;

        private final JSONObject params;

        private final ActionModule module;

        /**
         * 原始请求封包。缓存以保证同一次 handle() 内多次取用得到同一实例。
         */
        private final Packet request;

        /**
         * 已解析的访问令牌。requiresToken=false 的动作处理器不读它，为 null。
         */
        private final AuthToken token;

        private final AIGCHost host;

        AIGCStateCode code = null;

        JSONObject data = null;

        RecordingContext(ActionDialect dialect, ActionModule module) {
            this(dialect, module, null, new NoopHost());
        }

        /**
         * 构造函数。
         *
         * @param dialect 请求方言。
         * @param module 所属模块。
         * @param token 已解析的令牌；传 null 表示未解析（requiresToken=false 的场景）。
         * @param host 宿主能力接口。
         */
        RecordingContext(ActionDialect dialect, ActionModule module, AuthToken token, AIGCHost host) {
            this.dialect = dialect;
            this.module = module;
            this.params = dialect.getParamAsJson("data");
            this.request = new Packet(dialect);
            this.token = token;
            this.host = host;
        }

        @Override
        public String getAction() {
            return this.dialect.getName();
        }

        @Override
        public AuthToken getToken() {
            // requiresToken=false 的动作处理器不读它；为 null
            return this.token;
        }

        @Override
        public JSONObject getParams() {
            return (null != this.params) ? this.params : new JSONObject();
        }

        @Override
        public Packet getRequest() {
            return this.request;
        }

        @Override
        public ActionDialect getDialect() {
            return this.dialect;
        }

        @Override
        public ActionModule getModule() {
            return this.module;
        }

        @Override
        public AIGCHost getHost() {
            return this.host;
        }

        @Override
        public void respond(AIGCStateCode c, JSONObject d) {
            this.code = c;
            this.data = d;
        }

        @Override
        public void respondEmpty(AIGCStateCode c) {
            this.code = c;
            this.data = new JSONObject();
        }

        @Override
        public boolean isResponded() {
            return null != this.code;
        }

        @Override
        public void async(String taskKey, Runnable job) {
            throw new UnsupportedOperationException("Not needed in this test");
        }

        @Override
        public boolean isCancelled() {
            return false;
        }
    }

    /**
     * 全部方法返回空值的宿主，用于在不启动真实服务的前提下驱动装载流程。
     *
     * <p>刻意<b>不</b>声明为 final：测试用宿主以其为基类派生子类，
     * 只覆写需要变化的那一两个方法，避免重复实现全部 SPI 方法。</p>
     */
    static class NoopHost implements AIGCHost {

        /**
         * 模拟用户是否已注册，供客户/日程类动作切换。
         */
        boolean registered = false;

        /**
         * 模拟令牌解析结果。
         *
         * <p>为 {@code null} 时 {@link #resolveToken(String)} 恒返回 {@code null}，
         * 即「令牌无效」——量表族动作据此走 {@code IllegalOperation} 分支。
         * 非空时返回该令牌，供需要联系人的动作使用。</p>
         */
        AuthToken resolvedToken = null;

        @Override
        public AuthToken resolveToken(String tokenCode) {
            return this.resolvedToken;
        }

        @Override
        public boolean isRegistered(String tokenCode) {
            return this.registered;
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
        public cube.common.entity.AIGCChannel acquireChannel(AuthToken token, String participant,
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
            return 0;
        }

        @Override
        public boolean hasUnit(String capabilityName) {
            return false;
        }

        @Override
        public AtomicInteger increaseUnitCounter(String unitName) {
            return new AtomicInteger(0);
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
        public cube.common.entity.FileLabel saveFile(AuthToken token, String fileCode, File file,
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
        public Storage openModuleStorage(String storageName, StorageType type, JSONObject config) {
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

    /**
     * 恒定返回「令牌无效」的宿主能力。
     *
     * <p>用于验证量表族动作的令牌失效分支：这些动作在
     * {@code getToken()} 返回 {@code null} 时回 {@code IllegalOperation}，
     * 而非宿主动作骨架在 {@code requiresToken=true} 时给出的
     * {@code InconsistentToken}。本类只覆写令牌解析，其余行为沿用
     * {@link NoopHost}。</p>
     */
    static final class NilTokenHost extends NoopHost {

        @Override
        public AuthToken resolveToken(String tokenCode) {
            return null;
        }

        @Override
        public boolean isRegistered(String tokenCode) {
            return false;
        }
    }

    /**
     * 可返回关键词的宿主能力。
     *
     * <p>{@code extractKeywords} 是 SPI v3 新增的能力，真实实现依赖宿主分词器
     * 与一份较大的 IDF 语料，无法在用例中复现，因此以固定词表替代，
     * 只验证<b>调用链是否接通</b>与<b>边界入参的处理</b>。</p>
     */
    static final class KeywordHost extends NoopHost {

        @Override
        public List<String> extractKeywords(String content, int topN) {
            if (null == content || content.isEmpty() || topN <= 0) {
                return Collections.emptyList();
            }

            return Collections.singletonList("关键词");
        }
    }

    /**
     * 返回「处理中」状态报告的宿主能力。
     *
     * <p>用于验证报告链路里最容易被忽略的一条分支：报告状态为
     * {@code Processing} 或 {@code Inferencing} 时，只填
     * {@code thought} 一个字段（原始特征描述，<b>不</b>调模型），
     * 其余字段全部跳过，但仍然回 {@code Ok}。</p>
     */
    static final class ProcessingReportHost extends ReportHost {

        @Override
        public cube.aigc.psychology.composition.PaintingFeatureSet getPaintingFeatureSet(long reportSn) {
            // 处理中分支依赖特征集：宿主侧查不到时会记 WARN 且不回 thought
            return new cube.aigc.psychology.composition.PaintingFeatureSet(
                    new ArrayList<>(), new ArrayList<>());
        }

        @Override
        public cube.aigc.psychology.PaintingReport getPaintingReport(long sn) {
            if (sn != this.paintingSn) {
                return null;
            }

            long now = System.currentTimeMillis();
            cube.aigc.psychology.PaintingReport report = new cube.aigc.psychology.PaintingReport(
                    sn, 3003L, now, "测试报告",
                    new cube.aigc.psychology.Attribute("male", 28, "", Language.Chinese, false),
                    "file-code", cube.aigc.psychology.Theme.Generic, now);
            report.setState(AIGCStateCode.Processing);

            return report;
        }
    }

    /**
     * 可返回报告数据的宿主能力。
     *
     * <p>报告运行态（生成中报告的内存表、任务队列）由宿主持有，
     * 真实实现依赖 {@code PsychologyScene} 的单例状态与数据库，
     * 在用例中无法复现。本类以固定数据替代，用于验证
     * <b>处理器是否把参数正确透传、是否按状态码契约应答</b>，
     * 而不验证宿主自身的查询逻辑。</p>
     */
    static class ReportHost extends NoopHost {

        /**
         * 构造时默认能解析出令牌，使「报告数据可得」与「令牌有效」两个条件
         * 在多数用例中同时成立；令牌无效的用例再显式把
         * {@link NoopHost#resolvedToken} 置空。
         */
        ReportHost() {
            this.resolvedToken = token("t-report", 3003L);
        }

        /**
         * 存在的绘画报告序列号。
         */
        long paintingSn = 1001L;

        /**
         * 最近一次 {@code makeReportContent} 收到的参数编码。
         */
        String lastContentArgs = null;

        /**
         * 存在的量表报告序列号。
         */
        long scaleSn = 2002L;

        /**
         * 最近一次 {@code listScaleReports} 收到的参数。
         */
        long lastListCid = 0;

        @Override
        public cube.aigc.psychology.PaintingReport getPaintingReport(long sn) {
            if (sn != this.paintingSn) {
                return null;
            }

            // 默认给一份「已完成」的报告，供需要实体的动作（reportPart）使用
            long now = System.currentTimeMillis();
            cube.aigc.psychology.PaintingReport report = new cube.aigc.psychology.PaintingReport(
                    sn, 3003L, now, "测试报告",
                    new cube.aigc.psychology.Attribute("male", 28, "", Language.Chinese, false),
                    "file-code", cube.aigc.psychology.Theme.Generic, now);
            report.setState(AIGCStateCode.Ok);

            return report;
        }

        @Override
        public JSONObject queryPaintingReport(long sn, String format) {
            if (sn != this.paintingSn) {
                return null;
            }

            JSONObject json = new JSONObject();
            json.put("sn", sn);
            json.put("format", format);
            // 摘要形态才带队列位置，与既有行为一致
            if ("compact".equals(format)) {
                json.put("queuePosition", 2);
            }

            return json;
        }

        @Override
        public JSONObject queryScaleReport(long sn) {
            if (sn != this.scaleSn) {
                return null;
            }

            JSONObject json = new JSONObject();
            json.put("sn", sn);
            return json;
        }

        @Override
        public JSONObject listPaintingReports(long contactId, int page, int size,
                boolean descending, int state) {
            JSONObject data = new JSONObject();
            data.put("total", 1);
            data.put("page", page);
            data.put("size", size);
            data.put("list", new org.json.JSONArray().put(new JSONObject().put("sn", this.paintingSn)));
            return data;
        }

        @Override
        public JSONObject listScaleReports(long contactId, boolean descending, int state) {
            this.lastListCid = contactId;

            JSONObject data = new JSONObject();
            data.put("total", 1);
            data.put("list", new org.json.JSONArray().put(new JSONObject().put("sn", this.scaleSn)));
            return data;
        }

        @Override
        public JSONObject stopReportGeneration(long sn) {
            // 仅 paintingSn 可被停止，其余返回 null 以覆盖「未生效」分支
            if (sn != this.paintingSn) {
                return null;
            }

            JSONObject json = new JSONObject();
            json.put("sn", sn);
            json.put("state", 21);
            return json;
        }

        @Override
        public JSONObject resetReportAttention(long sn, Integer newAttention) {
            if (sn != this.paintingSn) {
                return null;
            }

            JSONObject json = new JSONObject();
            json.put("sn", sn);
            json.put("attention", (null == newAttention) ? -1 : newAttention);
            return json;
        }
    }
}
