/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc;

import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.util.Utils;
import cell.util.log.Logger;
import cube.aigc.*;
import cube.aigc.app.Notification;
import cube.aigc.complex.attachment.Attachment;
import cube.aigc.complex.widget.Event;
import cube.aigc.complex.widget.EventResult;
import cube.aigc.listener.GenerateTextListener;
import cube.aigc.listener.SpeechModuleListener;
import cube.aigc.listener.VoiceDiarizationListener;
import cube.aigc.spi.AIGCHost;
import cube.auth.AuthConsts;
import cube.auth.AuthToken;
import cube.common.Language;
import cube.common.Packet;
import cube.common.action.AIGCAction;
import cube.common.entity.*;
import cube.common.notice.*;
import cube.common.state.AIGCStateCode;
import cube.core.AbstractModule;
import cube.core.Kernel;
import cube.core.Module;
import cube.file.hook.FileStorageHook;
import cube.service.aigc.channel.ChannelManager;
import cube.service.aigc.event.EventCenter;
import cube.service.aigc.guidance.PromptComposer;
import cube.service.aigc.guidance.SkillRegistry;
import cube.service.aigc.guidance.SkillSessionStore;
import cube.service.aigc.guidance.TokenEstimator;
import cube.service.aigc.knowledge.KnowledgeBase;
import cube.service.aigc.knowledge.KnowledgeFramework;
import cube.service.aigc.listener.*;
import cube.service.aigc.member.MemberCenter;
import cube.service.aigc.plugin.*;
import cube.service.aigc.resource.Relay;
import cube.service.aigc.spi.ModuleRegistry;
import cube.service.aigc.unit.*;
import cube.service.aigc.utils.UserProfileRenderer;
import cube.service.auth.AuthService;
import cube.service.auth.AuthServiceHook;
import cube.service.contact.ContactHook;
import cube.service.contact.ContactManager;
import cube.service.contact.ContactMask;
import cube.service.contact.MembershipSystem;
import cube.storage.StorageType;
import cube.util.*;
import cube.util.tokenizer.Tokenizer;
import cube.util.tokenizer.keyword.TFIDFAnalyzer;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AIGC 服务。
 *
 * <p>本类收敛为<b>门面</b>：对上层暴露业务接口，把三类内聚职责委派给独立组件——</p>
 * <ul>
 *     <li>{@link UnitScheduler}：单元注册、选点与周期维护；</li>
 *     <li>{@link ChannelManager}：频道的创建、查询、保活与回收；</li>
 *     <li>{@link AIGCTaskExecutor}：各能力的任务队列、两层线程池与派发。</li>
 * </ul>
 * <p>本类自身只保留与上述三者无关的业务编排（用户、令牌、会员、知识库、文件、内容理解等）。</p>
 */
public class AIGCService extends AbstractModule implements Generatable {

    public final static String NAME = "AIGC";

    private final AIGCCellet cellet;

    /**
     * 业务模块是否处于降级状态（未装载、装载失败或能力缺失）。
     *
     * <p>降级时宿主<b>不提供</b>该业务域的动作能力，对应请求回
     * {@link AIGCStateCode#ModuleNotLoaded}；平台能力（聊天、知识库、频道、文件等）
     * 不受影响。</p>
     */
    private volatile boolean moduleDegraded = false;

    /**
     * 单元调度器：单元注册、选点与周期维护。
     */
    private final UnitScheduler unitScheduler;

    /**
     * 频道管理器：频道的创建、查询、保活与回收。
     */
    private final ChannelManager channelManager;

    /**
     * 任务执行器：各能力的任务队列、两层线程池与派发。
     */
    private final AIGCTaskExecutor taskExecutor;

    /**
     * Key 是 Stream name
     */
    private final Map<String, List<VoiceStreamSink>> waitingVoiceStreamSinks;

    /**
     * 语音能力模块级监听器，按注册顺序扇出。
     *
     * <p>说话人分离是平台级能力，分离结果上的角色映射等收尾操作由各业务模块
     * 经 {@link SpeechModuleListener} 订阅。宿主侧只做扇出与异常隔离，
     * 不内置任何业务角色分类。</p>
     */
    private final CopyOnWriteArrayList<SpeechModuleListener> speechModuleListeners;

    /**
     * 知识库架构。
     */
    private KnowledgeFramework knowledgeFramework;

    /**
     * 授权服务模块。启动时解析并缓存，避免热路径反复走 Kernel 查找。
     */
    private AuthService authService;

    /**
     * 文件存储模块。该模块可能未部署，因此保持可空语义。
     */
    private AbstractModule fileStorage;

    private AIGCStorage storage;

    private final Tokenizer tokenizer;

    private AIGCPluginSystem pluginSystem;

    /**
     * 工作路径。
     */
    public final File workingPath = new File("storage/tmp/");

    /**
     * 是否通过中继（Relay）访问，仅用于本地测试
     */
    public boolean useRelay = false;

    /**
     * 是否按技能声明的 keywords 自动装载技能（自动引入）。
     */
    private boolean skillAuto = true;

    /**
     * 单次自动装载的技能数量上限。
     */
    private int skillAutoLimit = 2;

    /**
     * 是否在提示词中注入技能目录（可用技能清单）。
     */
    private boolean skillCatalog = true;

    /**
     * 配置文件最后修改时间。
     */
    private long configFileLastModified = 0;
    // 配置文件上一次检测事件
    private long configFileLastTime = 0;

    public AIGCService(AIGCCellet cellet) {
        this.cellet = cellet;
        this.unitScheduler = new UnitScheduler();
        this.taskExecutor = new AIGCTaskExecutor();
        // 频道创建需要把令牌码解析为访问令牌，这里注入解析器，避免频道管理器反向依赖服务
        this.channelManager = new ChannelManager(cellet, token -> this.getAuthService().getToken(token));
        this.waitingVoiceStreamSinks = new ConcurrentHashMap<>();
        this.speechModuleListeners = new CopyOnWriteArrayList<>();
        this.tokenizer = new Tokenizer();
    }

    /**
     * 启动服务。
     *
     * <p>在独立引导线程中依次完成：读取配置、打开存储器并自检、绑定技能注册表与
     * 会话存储、装载 AIGC 业务模块（装载结果决定降级标志 {@link #moduleDegraded}）、
     * 注册授权/联系人/文件服务事件插件、实例化知识框架、启动资源管理器、会员中心
     * 与事件中心。存储器不可用时保持「未就绪」状态，不对外声称可用。</p>
     */
    @Override
    public void start() {
        this.pluginSystem = new AIGCPluginSystem();

        // 读取配置文件
        this.loadConfig();

        // 启动引导线程保持独立：它负责创建线程池本身，且在 start() 返回后仍需继续运行，
        // 不能放在自己将要创建的线程池上执行（停止时会出现「关池」与「引导未完成」的次序问题）。
        (new Thread(new Runnable() {
            @Override
            public void run() {
                if (!workingPath.exists()) {
                    if (!workingPath.mkdirs()) {
                        Logger.w(AIGCService.class, "AI Service - Make working path error: "
                                + workingPath.getAbsolutePath());
                    }
                }
                Logger.i(AIGCService.class, "AI Service - Working path: " + workingPath.getAbsolutePath());

                JSONObject config = ConfigUtils.readStorageConfig();
                if (config.has(AIGCService.NAME)) {
                    config = config.getJSONObject(AIGCService.NAME);
                    if (config.getString("type").equalsIgnoreCase("SQLite")) {
                        storage = new AIGCStorage(StorageType.SQLite, config);
                    }
                    else {
                        storage = new AIGCStorage(StorageType.MySQL, config);
                    }

                    storage.open();
                    storage.execSelfChecking(null);

                    // 绑定技能注册表：技能以存储器为准，多实例共享
                    SkillRegistry.getInstance().setup(storage);

                    // 绑定技能会话存储：会话级技能绑定以存储器为准，多实例一致
                    SkillSessionStore.getInstance().setup(storage);
                }
                else {
                    Logger.e(AIGCService.class, "#start - Can NOT find AIGC storage config");
                }

                // 装载 AIGC 业务模块。必须在宿主存储就绪之后：
                // 模块 setup 可能读取宿主存储配置，提前装载会失败。
                //
                // 业务场景的装配亦由模块的 setup 承接——PsychologyModule
                // 在 setup 里建存储并装配 PsychologyScene（注入 host、storage、
                // 队列上限，并完成分词器与引导流程初始化），故场景的宿主能力
                // 与存储必定有效。
                AIGCCellet theCellet = AIGCService.this.cellet;
                if (null != theCellet) {
                    theCellet.loadModules();

                    // 模块装载结果必须由宿主消费，否则「插件没装」与「动作没命中」
                    // 在运行期无法区分。此处按fail-fast 语义处理：
                    // 模块未就绪时，宿主不提供该业务域的能力，其余平台能力不受影响。
                    ModuleRegistry registry = theCellet.getModuleRegistry();
                    boolean blocked = (null != registry) && registry.hasBlockingFailure();
                    boolean capabilitiesOk = (null == registry) || registry.verifyCapabilities();

                    AIGCService.this.moduleDegraded = blocked || !capabilitiesOk;

                    if (AIGCService.this.moduleDegraded) {
                        Logger.e(AIGCService.class, "#start - 业务模块未就绪（装载失败或能力缺失），"
                                + "宿主不提供该业务域能力；平台能力（聊天/知识库/频道/文件）不受影响");
                    }
                }
                else {
                    Logger.e(AIGCService.class,
                            "#start - Cellet is NULL, NO AIGC module is loaded; "
                            + "the corresponding business capabilities are NOT available");
                }

                // 应用事件
                AppEventPlugin appEventPlugin = new AppEventPlugin(AIGCService.this);
                pluginSystem.register(AIGCHook.AppEvent, appEventPlugin);

                // 知识库事件
                KnowledgeBaseEventPlugin plugin = new KnowledgeBaseEventPlugin(AIGCService.this);
                pluginSystem.register(AIGCHook.ImportKnowledgeDoc, plugin);
                pluginSystem.register(AIGCHook.RemoveKnowledgeDoc, plugin);

                // 监听授权服务事件（解析结果由 getAuthService() 缓存）
                AuthService authService = AIGCService.this.getAuthService();
                awaitModuleReady(authService, AuthService.NAME);
                authService.getPluginSystem().register(AuthServiceHook.InjectToken,
                        new InjectTokenPlugin(AIGCService.this));

                // 监听联系人服务事件
                awaitModuleReady(ContactManager.getInstance(), "ContactManager");
                ContactManager.getInstance().getPluginSystem().register(ContactHook.NewContact,
                        new ActivateKnowledgeBasePlugin(AIGCService.this));
                ContactEventPlugin contactPlugin = new ContactEventPlugin(AIGCService.this);
                ContactManager.getInstance().getPluginSystem().register(ContactHook.SignIn, contactPlugin);
                ContactManager.getInstance().getPluginSystem().register(ContactHook.SignOut, contactPlugin);
                ContactManager.getInstance().getPluginSystem().register(ContactHook.DeviceTimeout, contactPlugin);
                ContactManager.getInstance().getPluginSystem().register(ContactHook.VerifyVerificationCode, contactPlugin);

                // 监听文件服务事件（解析结果由 getFileStorage() 缓存）
                AbstractModule fileStorage = AIGCService.this.getFileStorage();
                if (null != fileStorage) {
                    awaitModuleReady(fileStorage, "FileStorage");
                    fileStorage.getPluginSystem().register(FileStorageHook.SaveFile,
                            new NewFilePlugin(AIGCService.this));
                    fileStorage.getPluginSystem().register(FileStorageHook.DestroyFile,
                            new DeleteFilePlugin(AIGCService.this));
                }
                else {
                    Logger.e(AIGCService.class, "#start - Can NOT find \"FileStorage\" module!");
                }

                // 实例化知识框架
                knowledgeFramework = new KnowledgeFramework(AIGCService.this, authService, fileStorage);

                // 资源管理器
                Explorer.getInstance().setup(AIGCService.this, tokenizer);

                // 会员中心
                MemberCenter.getInstance().start(AIGCService.this);

                // 事件中心
                EventCenter.getInstance().start(AIGCService.this);

                if (null == AIGCService.this.storage) {
                    // 存储器不可用时服务无法提供任何数据能力，明确保持「未就绪」状态。
                    // 旧实现会在日志报错后继续往下执行并置位 ready，导致对外声称可用、
                    // 而所有依赖 storage 的接口出现 NPE。
                    Logger.e(AIGCService.class, "#start - AIGC service is NOT ready: "
                            + "AIGC storage is unavailable");
                    started.set(false);
                    return;
                }

                started.set(true);
                Logger.i(AIGCService.class, "AIGC service is ready");
            }
        })).start();
    }

    /**
     * 销毁服务。
     *
     * <p>卸载全部 AIGC 业务模块（含心理学场景的停止），并停止会员中心与事件中心。
     * 本方法不关闭存储器与线程池，那是 {@link #stop()} 的职责。</p>
     */
    @Override
    public void dispose() {
        Logger.i(this.getClass(), "#dispose - AIGC service dispose");

        // 卸载 AIGC 业务模块（此处停心理学场景）
        AIGCCellet theCellet = AIGCService.this.cellet;
        if (null != theCellet && null != theCellet.getModuleRegistry()) {
            theCellet.getModuleRegistry().teardownAll();
        }

        MemberCenter.getInstance().stop();

        EventCenter.getInstance().stop();
    }

    /**
     * 停止服务。
     *
     * <p>关闭两层线程池（后台短任务池 + 单元排空池）、清理频道索引、
     * 关闭存储器并停止资源管理器。</p>
     */
    @Override
    public void stop() {
        // 关闭两层线程池：后台短任务池 + 单元排空池
        this.taskExecutor.shutdown();

        // 清理频道索引，防止停止后索引残留
        this.channelManager.clear();

        if (null != this.storage) {
            this.storage.close();
            this.storage = null;
        }

        this.started.set(false);

        Explorer.getInstance().teardown();
    }

    /**
     * 获取 AIGC 插件系统。
     *
     * @return 返回插件系统实例。
     */
    @Override
    public AIGCPluginSystem getPluginSystem() {
        return this.pluginSystem;
    }

    /**
     * 周期回调（60 秒一次）。
     *
     * <p>依次执行：配置热加载（每 5 分钟检测一次）、单元维护、频道维护、
     * 知识框架维护、超时语音流接收器清理（4 小时）、资源管理器维护，
     * 以及驱动 AIGC 业务模块心跳。</p>
     *
     * @param module 模块。
     * @param kernel 内核。
     */
    @Override
    public void onTick(Module module, Kernel kernel) {
        // 周期 60 秒
//        Logger.i(AIGCService.class, "#onTick");

        long now = System.currentTimeMillis();

        if (now - this.configFileLastTime > 5 * 60 * 1000) {
            this.configFileLastTime = now;
            this.loadConfig();
        }

        // 单元维护：清理通信上下文失效的单元，并按周期复位单元运行标志
        this.unitScheduler.onTick(now);

        // 频道维护：复位未变化的处理标志，并回收空闲超时的频道
        this.channelManager.onTick(now);

        if (null != this.knowledgeFramework) {
            this.knowledgeFramework.onTick(now);
        }

        Iterator<Map.Entry<String, List<VoiceStreamSink>>> vssListIter = this.waitingVoiceStreamSinks.entrySet().iterator();
        while (vssListIter.hasNext()) {
            Map.Entry<String, List<VoiceStreamSink>> entry = vssListIter.next();
            List<VoiceStreamSink> list = entry.getValue();
            Iterator<VoiceStreamSink> vssIter = list.iterator();
            while (vssIter.hasNext()) {
                VoiceStreamSink sink = vssIter.next();
                if (now - sink.getTimestamp() > 4 * 60 * 60 * 1000) {
                    deleteFile(sink.authToken.getDomain(), sink.getFileCode());
                    vssIter.remove();
                }
            }
            if (list.isEmpty()) {
                vssListIter.remove();
            }
        }

        Explorer.getInstance().onTick(now);

        // 驱动 AIGC 业务模块心跳（此处驱动心理学场景）
        AIGCCellet theCellet = AIGCService.this.cellet;
        if (null != theCellet && null != theCellet.getModuleRegistry()) {
            theCellet.getModuleRegistry().tick(now);
        }
    }

    /**
     * 等待指定模块进入就绪状态。
     *
     * <p>启动期存在模块顺序依赖：AIGC 需要授权、联系人、文件存储等模块先就绪才能注册插件钩子。
     * 原实现在三处各写了一遍「自旋 100ms + {@code printStackTrace}」，此处收敛为一处，
     * 并把异常输出换成结构化日志（等待语义保持不变）。</p>
     *
     * @param module 待等待的模块；为 {@code null} 时视为无需等待。
     * @param name   模块名，仅用于日志。
     */
    private static void awaitModuleReady(AbstractModule module, String name) {
        if (null == module) {
            return;
        }

        while (!module.isStarted()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Logger.w(AIGCService.class, "#awaitModuleReady - Interrupted while waiting for " + name);
            }
        }
    }

    /**
     * 读取并应用服务配置（{@code config/aigc.properties}，支持热加载）。
     *
     * <p>覆盖：线程池规模与类型、各模型上下文长度下限、单元权重（{@code unit.weight.<cid>}）、
     * 网络搜索器、SKILL 技能（经 {@link #loadSkillConfig(Properties)}）、中继（Relay）开关。
     * 配置文件不存在或内容未变时补建默认线程池，保证异步入口可用。</p>
     */
    private void loadConfig() {
        try {
            File file = new File("config/aigc.properties");
            if (!file.exists()) {
                file = new File("aigc.properties");
            }

            if (this.configFileLastModified == file.lastModified()) {
//                if (Logger.isDebugLevel()) {
//                    Logger.d(this.getClass(), "#loadConfig - File not modified");
//                }
                // 配置文件不存在时 lastModified() 与初值同为 0，首次加载会直接走到这里，
                // 线程池便一直为空、所有异步入口静默失效——此处补建默认线程池。
                this.taskExecutor.createExecutor(8, "fixed");
                return;
            }

            this.configFileLastModified = file.lastModified();

            Properties properties = ConfigUtils.readProperties(file.getAbsolutePath());

            // 性能配置
            int max = 8;
            try {
                max = Integer.parseInt(properties.getProperty("threadpool.max", "32"));
            } catch (Exception e) {
                // Nothing
            }
            this.taskExecutor.createExecutor(max, properties.getProperty("threadpool.type", "fixed"));

            // 上下文长度限制
            ModelConfig.EXTRA_LONG_CONTEXT_LIMIT = Math.max(Integer.parseInt(
                        properties.getProperty("context.length",
                                Integer.toString(ModelConfig.EXTRA_LONG_CONTEXT_LIMIT))),
                    ModelConfig.EXTRA_LONG_CONTEXT_LIMIT);
            ModelConfig.BAIZE_CONTEXT_LIMIT = Math.max(Integer.parseInt(
                        properties.getProperty("context.length.baize",
                                Integer.toString(ModelConfig.BAIZE_CONTEXT_LIMIT))),
                    ModelConfig.BAIZE_CONTEXT_LIMIT);
            ModelConfig.BAIZE_2_CONTEXT_LIMIT = Math.max(Integer.parseInt(
                        properties.getProperty("context.length.baize2",
                                Integer.toString(ModelConfig.BAIZE_2_CONTEXT_LIMIT))),
                    ModelConfig.BAIZE_2_CONTEXT_LIMIT);

            // Unit 权重
            Iterator<Object> keyIter = properties.keySet().iterator();
            while (keyIter.hasNext()) {
                String key = keyIter.next().toString();
                if (key.startsWith("unit.weight.")) {
                    String[] seg = key.split("\\.");
                    if (seg.length == 3) {
                        try {
                            long cid = Long.parseLong(seg[2]);
                            double weight = Double.parseDouble(properties.getProperty(key, "1.0"));
                            this.unitScheduler.setWeight(cid, weight);
                            Logger.i(this.getClass(), "AI Service - Unit weight: " + cid + " - " + weight);
                        } catch (Exception e) {
                            // Nothing
                        }
                    }
                }
            }

            // 网络搜索配置
            if (properties.containsKey("page.reader.url") || properties.containsKey("page.searcher")) {
                Explorer.getInstance().config(properties.getProperty("page.searcher", "baidu"));
                Logger.i(this.getClass(), "AI Service - Page searcher: "
                        + Explorer.getInstance().getSearcherName());
            }

            // SKILL 技能
            this.loadSkillConfig(properties);

            // 是否启用中继（Relay）
            this.useRelay = Boolean.parseBoolean(
                    properties.getProperty("relay", "false"));
            if (this.useRelay) {
                Relay.createInstance(properties.getProperty("relay.url", "http://127.0.0.1:7010"),
                        properties.getProperty("relay.token", ""));
                // 添加单元
                this.unitScheduler.fillFromRelay(Relay.getInstance());
            }
        } catch (IOException e) {
            Logger.e(this.getClass(), "#loadConfig - Load config properties error", e);
        }

        Logger.i(this.getClass(), "AI Service - Context length: " + ModelConfig.EXTRA_LONG_CONTEXT_LIMIT);
        Logger.i(this.getClass(), "AI Service - Baize context limit: " + ModelConfig.BAIZE_CONTEXT_LIMIT);
        Logger.i(this.getClass(), "AI Service - Baize2 context limit: " + ModelConfig.BAIZE_2_CONTEXT_LIMIT);
        if (this.useRelay) {
            Logger.i(this.getClass(), "AI Service - Relay URL: " + Relay.getInstance().getUrl());
        }
    }

    /**
     * 业务模块是否处于降级状态。
     *
     * <p>供健康检查与运维查询：降级表示该业务域当前不可用，
     * 但主服务与其余平台能力正常运行。</p>
     *
     * @return 降级时返回 <code>true</code>。
     */
    public boolean isModuleDegraded() {
        return this.moduleDegraded;
    }

    /**
     * 按联系人个人知识库生成补充说明。
     *
     * <p>把该联系人的历史知识条目按问题相关度检索后拼成一段文本，
     * 用于给模型补充个人背景。个人知识库的访问权在宿主，
     * 故该能力由宿主实现，模块经 SPI 转发调用。</p>
     *
     * @param tokenCode 访问令牌码，用于定位联系人档案。
     * @param query 问题。
     * @param english 是否以英文提问（影响人称表述）。
     * @return 返回补充文本；无知识库、无命中或联系人信息缺失时返回 <code>null</code>。
     */
    public String generatePersonalKnowledge(String tokenCode, String query, boolean english) {
        if (null == query) {
            return null;
        }

        User user = this.getUser(tokenCode);
        if (null == user) {
            Logger.d(this.getClass(), "#generatePersonalKnowledge - No user: " + tokenCode);
            return null;
        }

        long contactId = user.getContactId();

        KnowledgeFramework framework = this.getKnowledgeFramework();
        if (null == framework) {
            return null;
        }

        KnowledgeBase base = framework.getKnowledgeBase(contactId, User.KnowledgeBaseName);
        if (null == base) {
            Logger.d(this.getClass(), "#generatePersonalKnowledge - No personal base: " + contactId);
            return null;
        }

        String fixedQuery = english ? "I'm user \"" + user.getName() + "\", " + query
                : "我是用户“" + user.getName() + "”，" + query;
        Knowledge knowledge = base.generateKnowledge(fixedQuery, 3);
        if (null == knowledge) {
            Logger.d(this.getClass(), "#generatePersonalKnowledge - Generates knowledge failed: " + contactId);
            return null;
        }

        StringBuilder buf = new StringBuilder();
        for (Knowledge.Metadata metadata : knowledge.metadataList) {
            buf.append(metadata.getContent());
            buf.append("\n\n");
        }

        return buf.toString();
    }

    /**
     * 加载 SKILL 技能、提示词编排与 Token 估算相关配置。
     *
     * <p>技能相关配置：</p>
     * <ul>
     *     <li>{@code skills.path}：技能种子目录，多个使用逗号分隔，相对工作目录；
     *         技能以存储器（DB）为准，目录仅用于引导与兜底；</li>
     *     <li>{@code skills.cache.ttl}：缓存刷新间隔（毫秒），多实例部署下 DB 变更最长在该间隔后生效；</li>
     *     <li>{@code skills.seed}：启动时是否把种子目录的技能导入存储器（幂等）；</li>
     *     <li>{@code skills.seed.overwrite}：导入时是否覆盖存储器中的同名技能；</li>
     *     <li>{@code skills.auto} / {@code skills.auto.limit}：是否按技能声明的 keywords 自动装载，以及单次上限；</li>
     *     <li>{@code skills.catalog}：是否在提示词中注入技能目录（可用技能清单）；</li>
     *     <li>{@code skills.session} / {@code skills.session.ttl} / {@code skills.session.cache.ttl}：
     *         会话级技能绑定的开关、生存时间与本地缓存时间。</li>
     * </ul>
     *
     * <p>提示词编排与 Token 估算配置：</p>
     * <ul>
     *     <li>{@code prompt.composer}：是否启用分段编排；</li>
     *     <li>{@code prompt.output.reserve.ratio}：为模型输出预留的上下文比例（%）；</li>
     *     <li>{@code prompt.catalog.ratio} / {@code prompt.skill.ratio} / {@code prompt.retrieval.ratio}
     *         / {@code prompt.history.ratio}：各弹性段的预算占比（%）；</li>
     *     <li>{@code token.chars.per.token} / {@code token.calibration.alpha}：Token 估算初值与校准平滑系数。</li>
     * </ul>
     *
     * @param properties 配置文件内容。
     */
    private void loadSkillConfig(Properties properties) {
        List<File> skillPaths = new ArrayList<>();
        String pathValue = properties.getProperty("skills.path", SkillRegistry.DEFAULT_SKILLS_PATH);
        for (String path : pathValue.split(",")) {
            String trimmed = path.trim();
            if (!trimmed.isEmpty()) {
                skillPaths.add(new File(trimmed));
            }
        }

        long cacheTtl = getLong(properties, "skills.cache.ttl", SkillRegistry.DEFAULT_CACHE_TTL);
        boolean seed = Boolean.parseBoolean(properties.getProperty("skills.seed", "false"));
        boolean seedOverwrite = Boolean.parseBoolean(properties.getProperty("skills.seed.overwrite", "false"));

        SkillRegistry.getInstance().configure(skillPaths, cacheTtl, seed, seedOverwrite);

        // 技能的自动装载与目录注入
        this.skillAuto = Boolean.parseBoolean(properties.getProperty("skills.auto", "true"));
        this.skillAutoLimit = getInt(properties, "skills.auto.limit", 2);
        this.skillCatalog = Boolean.parseBoolean(properties.getProperty("skills.catalog", "true"));

        // 会话级技能绑定
        boolean sessionEnabled = Boolean.parseBoolean(properties.getProperty("skills.session", "true"));
        long sessionTtl = getLong(properties, "skills.session.ttl", SkillSessionStore.DEFAULT_TTL);
        long sessionCacheTtl = getLong(properties, "skills.session.cache.ttl",
                SkillSessionStore.DEFAULT_CACHE_TTL);
        SkillSessionStore.getInstance().configure(sessionEnabled, sessionTtl, sessionCacheTtl);

        // 提示词编排
        PromptComposer.Policy policy = new PromptComposer.Policy();
        policy.outputReservePercent = getInt(properties, "prompt.output.reserve.ratio",
                policy.outputReservePercent);
        policy.catalogPercent = getInt(properties, "prompt.catalog.ratio", policy.catalogPercent);
        policy.skillsPercent = getInt(properties, "prompt.skill.ratio", policy.skillsPercent);
        policy.retrievalPercent = getInt(properties, "prompt.retrieval.ratio", policy.retrievalPercent);
        policy.historyPercent = getInt(properties, "prompt.history.ratio", policy.historyPercent);
        boolean composerEnabled = Boolean.parseBoolean(properties.getProperty("prompt.composer", "true"));
        PromptComposer.getInstance().configure(composerEnabled, policy);

        // Token 估算与校准
        double charsPerToken = getDouble(properties, "token.chars.per.token",
                TokenEstimator.DEFAULT_CHARS_PER_TOKEN);
        double alpha = getDouble(properties, "token.calibration.alpha", TokenEstimator.DEFAULT_ALPHA);
        TokenEstimator.getInstance().configure(charsPerToken, alpha);

        Logger.i(this.getClass(), "AI Service - Skills path: " + skillPaths
                + " - cache TTL: " + cacheTtl + "ms"
                + " - seed: " + seed);
        Logger.i(this.getClass(), "AI Service - Skill auto: " + this.skillAuto
                + " (limit " + this.skillAutoLimit + ")"
                + " - catalog: " + this.skillCatalog
                + " - session binding: " + sessionEnabled + " (ttl " + sessionTtl + "ms)");
        Logger.i(this.getClass(), "AI Service - Prompt composer: " + composerEnabled
                + " - ratios(catalog/skill/retrieval/history): "
                + policy.catalogPercent + "/" + policy.skillsPercent + "/"
                + policy.retrievalPercent + "/" + policy.historyPercent
                + " - output reserve: " + policy.outputReservePercent + "%"
                + " - chars/token: " + TokenEstimator.getInstance().getCharsPerToken());
    }

    /**
     * 读取整型配置项。
     *
     * @param properties 配置项集合。
     * @param key 配置键。
     * @param defaultValue 默认值。
     * @return 值缺失或非法时返回默认值。
     */
    private static int getInt(Properties properties, String key, int defaultValue) {
        try {
            String value = properties.getProperty(key);
            return (null != value) ? Integer.parseInt(value.trim()) : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * 读取长整型配置项。
     *
     * @param properties 配置项集合。
     * @param key 配置键。
     * @param defaultValue 默认值。
     * @return 值缺失或非法时返回默认值。
     */
    private static long getLong(Properties properties, String key, long defaultValue) {
        try {
            String value = properties.getProperty(key);
            return (null != value) ? Long.parseLong(value.trim()) : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * 读取浮点型配置项。
     *
     * @param properties 配置项集合。
     * @param key 配置键。
     * @param defaultValue 默认值。
     * @return 值缺失或非法时返回默认值。
     */
    private static double getDouble(Properties properties, String key, double defaultValue) {
        try {
            String value = properties.getProperty(key);
            return (null != value) ? Double.parseDouble(value.trim()) : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * 获取服务所在的 Cellet。
     *
     * @return 返回 Cellet 实例。
     */
    public AIGCCellet getCellet() {
        return this.cellet;
    }

    /**
     * 获取宿主能力接口。
     *
     * <p>报告生成编排（绘画报告、量表报告、量表读取）属心理学业务，实现体在
     * 宿主运行时（需队列与工作线程，模块拿不到这些设施），经
     * {@link AIGCHost} 转发给门面，从而门面不直接引用业务场景单例。</p>
     *
     * <p>⚠️ 每次现取、不缓存为字段：SPI 的 host 实现在 {@code install()} 阶段
     * 构造，而本方法可能在 cellet 装配完成前被调用，缓存会拿到 {@code null}。</p>
     *
     * @return 返回宿主能力接口；未装配时返回 <code>null</code>。
     */
    private AIGCHost getHost() {
        AIGCCellet theCellet = this.cellet;
        if (null == theCellet) {
            return null;
        }

        return theCellet.getAIGCHost();
    }

    /**
     * 从音频处理队列中丢弃指定文件的待处理任务。
     *
     * <p>供插件在停止语音流时调用：队列里尚未开始做说话人分离的分片
     * 若继续执行，会去读已被删除的文件。</p>
     *
     * @param fileCodes 文件码集合。
     */
    public void discardAudio(java.util.Collection<String> fileCodes) {
        this.taskExecutor.discardAudio(fileCodes);
    }

    /**
     * 获取 AIGC 存储器。
     *
     * @return 返回存储器；服务未就绪或存储配置缺失时返回 {@code null}。
     */
    public AIGCStorage getStorage() {
        return this.storage;
    }

    /**
     * 获取文本分词器。
     *
     * @return 返回分词器实例。
     */
    public Tokenizer getTokenizer() {
        return this.tokenizer;
    }

    /**
     * 获取服务级后台任务线程池。
     *
     * <p>该池被场景子任务、DB 写入、回调等约 90 处短任务共用，因此<b>不要</b>向其中提交
     * 长阻塞任务（模型推理等）——单元排空任务由任务执行器的独立池承担。</p>
     *
     * @return 返回线程池，尚未创建时返回 {@code null}。
     */
    public ExecutorService getExecutor() {
        return this.taskExecutor.getExecutor();
    }

    /**
     * 获取授权服务模块。
     *
     * <p>启动时已解析并缓存；若尚未缓存（启动竞态）则回退到 Kernel 查找并补缓存。</p>
     *
     * @return 返回授权服务模块。
     */
    private AuthService getAuthService() {
        AuthService module = this.authService;
        if (null == module) {
            module = (AuthService) this.getKernel().getModule(AuthService.NAME);
            this.authService = module;
        }
        return module;
    }

    /**
     * 获取文件存储模块。
     *
     * <p>该模块允许未部署，因此仅在解析到实例时才缓存，避免把「暂时缺失」固化成永久缺失。</p>
     *
     * @return 返回文件存储模块；未部署时返回 {@code null}。
     */
    private AbstractModule getFileStorage() {
        AbstractModule module = this.fileStorage;
        if (null == module) {
            module = this.getKernel().getModule("FileStorage");
            this.fileStorage = module;
        }
        return module;
    }

    /**
     * 获取 SKILL 技能注册表。技能以存储器（DB）为准，多实例共享同一份定义。
     *
     * @return 返回技能注册表。
     */
    public SkillRegistry getSkillRegistry() {
        return SkillRegistry.getInstance();
    }

    /**
     * 获取会话级技能绑定存储。
     *
     * @return 返回技能会话存储。
     */
    public SkillSessionStore getSkillSessionStore() {
        return SkillSessionStore.getInstance();
    }

    /**
     * 获取提示词编排器。
     *
     * @return 返回提示词编排器。
     */
    public PromptComposer getPromptComposer() {
        return PromptComposer.getInstance();
    }

    /**
     * 获取 Token 估算器。
     *
     * @return 返回 Token 估算器。
     */
    public TokenEstimator getTokenEstimator() {
        return TokenEstimator.getInstance();
    }

    /**
     * 是否按技能声明的 keywords 自动装载技能。
     */
    public boolean isSkillAutoEnabled() {
        return this.skillAuto;
    }

    /**
     * 单次自动装载的技能数量上限。
     */
    public int getSkillAutoLimit() {
        return this.skillAutoLimit;
    }

    /**
     * 是否在提示词中注入技能目录。
     */
    public boolean isSkillCatalogEnabled() {
        return this.skillCatalog;
    }

    /**
     * 获取服务工作路径（{@code storage/tmp/}）。
     *
     * @return 返回工作路径。
     */
    public File getWorkingPath() {
        return this.workingPath;
    }

    /**
     * 注册单元。已存在的单元仅更新其通信上下文。
     *
     * @param contact      联系人。
     * @param capabilities 能力列表。
     * @param context      通信上下文。
     * @return 返回本次涉及的单元列表。
     */
    public List<AIGCUnit> setupUnit(Contact contact, List<AICapability> capabilities, TalkContext context) {
        return this.unitScheduler.setup(contact, capabilities, context);
    }

    /**
     * 注销指定联系人的所有单元。
     *
     * @param contact 联系人。
     * @return 返回被注销的单元列表。
     */
    public List<AIGCUnit> teardownUnit(Contact contact) {
        return this.unitScheduler.teardown(contact);
    }

    /**
     * 获取当前所有单元的快照。
     *
     * @return 返回单元列表。
     */
    public List<AIGCUnit> getAllUnits() {
        return this.unitScheduler.getAll();
    }

    /**
     * 获取当前所有频道的快照。
     *
     * @return 返回频道列表。
     */
    public List<AIGCChannel> getAllChannels() {
        return this.channelManager.getAll();
    }

    /**
     * 统计指定能力名下的有效单元数量。
     *
     * @param unitName 单元能力名。
     * @return 返回数量。
     */
    public int numUnitsByName(String unitName) {
        return this.unitScheduler.numUnitsByName(unitName);
    }

    /**
     * 判断是否存在指定能力名的单元，不区分单元是否有效。
     *
     * @param unitName 单元能力名。
     * @return 存在返回 {@code true}。
     */
    public boolean hasUnit(String unitName) {
        return this.unitScheduler.hasUnit(unitName);
    }

    /**
     * 选择空闲的单元，如果没有空闲单元返回 <code>null</code> 值。
     *
     * @param unitName 单元能力名。
     * @return 找不到空闲单元时返回 {@code null}。
     */
    public AIGCUnit selectIdleUnitByName(String unitName) {
        return this.unitScheduler.selectIdle(unitName);
    }

    /**
     * 按能力名选择单元。优先返回空闲单元，没有空闲单元时返回最久未执行的单元。
     *
     * @param unitName 单元能力名。
     * @return 无可用单元时返回 {@code null}。
     */
    public AIGCUnit selectUnitByName(String unitName) {
        return this.unitScheduler.select(unitName);
    }

    /**
     * 按能力名与联系人 ID 选择单元。
     *
     * @param unitName 单元能力名。
     * @param cid      联系人 ID。
     * @return 无可用单元时返回 {@code null}。
     */
    public AIGCUnit selectUnitByName(String unitName, long cid) {
        return this.unitScheduler.select(unitName, cid);
    }

    /**
     * 按子任务名选择单元。
     *
     * @param subtask 子任务名。
     * @return 无可用单元时返回 {@code null}。
     */
    public AIGCUnit selectUnitBySubtask(String subtask) {
        return this.unitScheduler.selectBySubtask(subtask);
    }

    //-------- App Interface - Start --------

    /**
     * 触发应用事件。
     *
     * <p>先经插件系统分发（{@link AIGCHook#AppEvent} 钩子），再持久化到存储器。</p>
     *
     * @param appEvent 应用事件。
     * @return 插件链处理完成且存储器写入成功返回 {@code true}；
     *         存储器不可用或写入失败返回 {@code false}。
     */
    public boolean fireEvent(AppEvent appEvent) {
        AIGCHook hook = this.pluginSystem.getAppEventHook();
        AIGCPluginContext context = new AIGCPluginContext(appEvent);
        hook.apply(context);

        if (null == this.storage) {
            Logger.w(this.getClass(), "#fireEvent - AIGC storage is NOT available");
            return false;
        }

        return this.storage.writeAppEvent(appEvent);
    }

    /**
     * 按联系人 ID 获取用户（含访问令牌）。
     *
     * @param uid 联系人 ID。
     * @return 返回用户；联系人或令牌不存在返回 {@code null}。
     */
    public User getUser(long uid) {
        Contact contact = ContactManager.getInstance().getContact(AuthConsts.DEFAULT_DOMAIN, uid);
        if (null == contact) {
            Logger.w(this.getClass(), "#getUser - Can NOT find contact: " + uid);
            return null;
        }
        AuthService authService = this.getAuthService();
        AuthToken authToken = authService.getToken(AuthConsts.DEFAULT_DOMAIN, uid);
        if (null == authToken) {
            Logger.w(this.getClass(), "#getUser - Can NOT find token: " + uid);
            return null;
        }
        User user = new User(contact.getContext());
        user.setAuthToken(authToken);
        return user;
    }

    /**
     * 按访问令牌码获取用户。
     *
     * @param token 访问令牌码。
     * @return 返回用户；令牌或联系人不存在返回 {@code null}。
     */
    public User getUser(String token) {
        AuthService authService = this.getAuthService();
        AuthToken authToken = authService.getToken(token);
        if (null == authToken) {
            Logger.w(this.getClass(), "#getUser - Can NOT find token: " + token);
            return null;
        }
        Contact contact = ContactManager.getInstance().getContact(token);
        if (null == contact) {
            Logger.w(this.getClass(), "#getUser - Can NOT find contact: " + token);
            return null;
        }
        User user = new User(contact.getContext());
        user.setAuthToken(authToken);
        return user;
    }

    /**
     * 创建新用户。
     *
     * <p>生成 10 位随机联系人 ID（冲突时重试），签发 10 年有效期的访问令牌，
     * 并以随机名称与「未登录」显示名注册为联系人。</p>
     *
     * @param appAgent 应用代理标识。
     * @param device 设备信息，可为 {@code null}。
     * @param channel 渠道代码。
     * @return 返回新用户（含令牌）。
     */
    public User createUser(String appAgent, Device device, String channel) {
        final String domain = AuthConsts.DEFAULT_DOMAIN;
        final String appKey = AuthConsts.DEFAULT_APP_KEY;
        final long tokenDuration = 10L * 365 * 24 * 60 * 60 * 1000;

        User user = null;

//        long id = Cryptology.getInstance().fastHash(appAgent);
//        try {
//            MessageDigest md5 = MessageDigest.getInstance("MD5");
//            byte[] digest = md5.digest(appAgent.getBytes(StandardCharsets.UTF_8));
//            long hash = Cryptology.getInstance().fastHash(digest);
//            id += hash;
//        } catch (Exception e) {
//            Logger.e(this.getClass(), "#getOrCreateUser", e);
//        }
//        // 处理 ID
//        id = Math.abs(id);

        // 生成10位ID
        long id = Long.parseLong(Utils.randomInt(10000, 99999) + Utils.randomNumberString(5));
        while (ContactManager.getInstance().containsContact(domain, id)) {
            Logger.e(this.getClass(), "#getOrCreateUser - Retry contact id: " + id);
            id = Long.parseLong(Utils.randomInt(10000, 99999) + Utils.randomNumberString(5));
        }

        // 创建令牌
        AuthService authService = this.getAuthService();
        // 10年有效时长
        AuthToken authToken = authService.applyToken(domain, appKey, id, tokenDuration);

        String name = "ME" + Utils.randomNumberString(8);
        user = new User(id, name, appAgent, channel);
        user.setDisplayName("未登录");
        user.setAuthToken(authToken);

        // 新用户
        ContactManager.getInstance().newContact(id, domain, name, user.toJSON(), device);
        return user;
    }

    /**
     * 修改用户信息（当前仅支持显示名）。
     *
     * @param token 访问令牌码。
     * @param modification 修改内容。
     * @return 返回更新后的用户；令牌或联系人无效返回 {@code null}。
     */
    public User modifyUser(String token, UserModification modification) {
        AuthToken authToken = this.getToken(token);
        if (null == authToken) {
            return null;
        }

        Contact contact = ContactManager.getInstance().getContact(authToken.getDomain(), authToken.getContactId());
        if (null == contact) {
            return null;
        }

        User user = new User(contact.getContext());
        if (null != modification.displayName) {
            user.setDisplayName(modification.displayName);
        }
        ContactManager.getInstance().updateContact(contact.getDomain().getName(), contact.getId(), contact.getName(),
                user.toJSON(), null);
        return user;
    }

    /**
     * 手机号验证码登录（注册/登录二合一）。
     *
     * <p>新用户：绑定手机号、更新个人知识记忆、激活 1 年会员；
     * 老用户：删除临时联系人令牌并将令牌码过户给原账号，
     * 临时联系人标记作废并删除其个人知识库。</p>
     *
     * @param contact 当前设备上的临时联系人。
     * @param verificationCode 已验证的验证码（含手机号）。
     * @return 返回登录后的用户（含新令牌）。
     */
    public User checkInUser(Contact contact, VerificationCode verificationCode) {
        // 查找用户
        ContactSearchResult searchResult = ContactManager.getInstance().searchWithContactName(
                contact.getDomain().getName(), verificationCode.phoneNumber);
        if (searchResult.getContactList().isEmpty()) {
            Logger.i(this.getClass(), "#checkInUser - New user: " + contact.getId());

            // 新注册用户
            User user = new User(contact.getContext());
            user.setRegisterTime(System.currentTimeMillis());
            user.setName(verificationCode.phoneNumber);
            user.setDisplayName("ME" + verificationCode.phoneNumber.substring(verificationCode.phoneNumber.length() - 4));
            user.setPhoneNumber(verificationCode.dialCode + "-" + verificationCode.phoneNumber);

            ContactManager.getInstance().updateContact(contact.getDomain().getName(),
                    contact.getId(), verificationCode.phoneNumber, user.toJSON(),
                    contact.getDevice());

            // 更新个人知识记忆
            this.updatePersonalKnowledgeBase(ContactManager.getInstance().getAuthToken(contact.getDomain().getName(),
                    contact.getId()), user);

            // 激活1年会员
            MembershipSystem.InvitationCode invitationCode = ContactManager.getInstance().getMembershipSystem().getIdleInvitationCode(
                    Membership.TYPE_PREMIUM, MembershipSystem.VALIDITY_ANNUAL);
            this.activateMembership(ContactManager.getInstance().getAuthToken(contact.getDomain().getName(),
                    contact.getId()), "MindEcho", invitationCode.code);

            return user;
        }
        else {
            Logger.i(this.getClass(), "#checkInUser - User login: " + contact.getId());

            // 老用户登录
            // 老用户当前使用的令牌删除，但是不删除设备的临时联系人
            Contact userContact = searchResult.getContactList().get(0);
            AuthService authService = this.getAuthService();
            // 当前使用令牌码
            AuthToken currentToken = authService.getToken(contact.getDomain().getName(), contact.getId());
            String tokenCode = null;
            if (null != currentToken) {
                tokenCode = currentToken.getCode();
                // 删除当前临时联系人的令牌
                authService.deleteToken(contact.getDomain().getName(), contact.getId());
            }
            else {
                tokenCode = Utils.randomString(32);
            }

            if (contact.getId().longValue() != userContact.getId().longValue()) {
                // 临时联系人标记为作废
                ContactManager.getInstance().setContactMask(contact.getDomain().getName(), contact.getId(),
                        ContactMask.Deprecated);
                // 删除对应的知识库
                getKnowledgeFramework().deleteKnowledgeBase(contact.getId(), User.KnowledgeBaseName);
            }

            // 更新老用户的令牌
            final long tokenDuration = 5L * 365 * 24 * 60 * 60 * 1000;
            AuthToken authToken = new AuthToken(tokenCode, userContact.getDomain().getName(),
                    AuthConsts.DEFAULT_APP_KEY, userContact.getId(),
                    System.currentTimeMillis(), System.currentTimeMillis() + tokenDuration, false);
            AuthToken newToken = authService.updateAuthTokenCode(authToken);
            // 新令牌
            User user = new User(userContact.getContext());
            user.setAuthToken(newToken);

            ContactManager.getInstance().updateContact(userContact.getDomain().getName(),
                    userContact.getId(), verificationCode.phoneNumber, user.toJSON(),
                    contact.getDevice());
            return user;
        }
    }

    /**
     * 用户名密码登录（注册/登录二合一）。
     *
     * <p>注册：用户名不存在时创建新用户、更新个人知识记忆、激活 1 年会员；
     * 登录：校验用户名密码，通过后令牌码过户给原账号并更新个人知识记忆，
     * 临时联系人标记作废并删除其个人知识库。</p>
     *
     * @param register {@code true} 表示注册语义，{@code false} 表示登录语义。
     * @param userName 用户名（邮箱）。
     * @param password 密码。
     * @param contact 当前设备上的临时联系人。
     * @return 注册时用户已存在、登录时用户不存在或密码错误均返回 {@code null}；
     *         否则返回登录后的用户。
     */
    public User checkInUser(boolean register, String userName, String password, Contact contact) {
        ContactSearchResult searchResult = ContactManager.getInstance()
                .searchWithContactName(contact.getDomain().getName(), userName);
        if (register) {
            if (searchResult.getContactList().isEmpty()) {
                // 注册新用户
                Logger.i(this.getClass(), "#checkInUser - register new user: " + userName);

                // 新用户
                User user = new User(contact.getContext());
                user.setRegisterTime(System.currentTimeMillis());
                user.setName(userName);
                user.setDisplayName(TextUtils.extractEmailAccountName(userName));
                user.setEmail(userName);
                user.setPassword(password);

                ContactManager.getInstance().updateContact(contact.getDomain().getName(),
                        contact.getId(), userName, user.toJSON(),
                        contact.getDevice());

                // 更新个人知识记忆
                this.updatePersonalKnowledgeBase(ContactManager.getInstance().getAuthToken(contact.getDomain().getName(),
                        contact.getId()), user);

                // 激活1年会员
                MembershipSystem.InvitationCode invitationCode = ContactManager.getInstance().getMembershipSystem().getIdleInvitationCode(
                        Membership.TYPE_PREMIUM, MembershipSystem.VALIDITY_ANNUAL);
                this.activateMembership(ContactManager.getInstance().getAuthToken(contact.getDomain().getName(),
                        contact.getId()), "MindEcho", invitationCode.code);

                return user;
            }
            else {
                // 用户已存在
                Logger.w(this.getClass(), "#checkInUser - The user already exists: " + userName);
                return null;
            }
        }
        else {
            if (searchResult.getContactList().isEmpty()) {
                // 用户不存在
                Logger.w(this.getClass(), "#checkInUser - The user is NOT exist.: " + userName);
                return null;
            }
            else {
                // 校验用户名和密码
                Logger.i(this.getClass(), "#checkInUser - User login: " + contact.getId());

                Contact userContact = searchResult.getContactList().get(0);
                User user = new User(userContact.getContext());
                if (user.getName().equals(userName) && user.getPassword().equals(password)) {
                    // 校验通过
                    AuthService authService = this.getAuthService();
                    // 当前使用令牌码，登录的账号继承当前临时账号的令牌码
                    AuthToken currentToken = authService.getToken(contact.getDomain().getName(), contact.getId());
                    String tokenCode = null;
                    if (null != currentToken) {
                        tokenCode = currentToken.getCode();
                        // 删除当前临时联系人的令牌
                        authService.deleteToken(contact.getDomain().getName(), contact.getId());
                    }
                    else {
                        tokenCode = Utils.randomString(32);
                    }

                    if (contact.getId().longValue() != userContact.getId().longValue()) {
                        // 临时联系人标记为作废
                        ContactManager.getInstance().setContactMask(contact.getDomain().getName(), contact.getId(),
                                ContactMask.Deprecated);
                        // 删除对应的知识库
                        getKnowledgeFramework().deleteKnowledgeBase(contact.getId(), User.KnowledgeBaseName);
                    }

                    // 更新老用户的令牌
                    final long tokenDuration = 5L * 365 * 24 * 60 * 60 * 1000;
                    AuthToken authToken = new AuthToken(tokenCode, userContact.getDomain().getName(),
                            AuthConsts.DEFAULT_APP_KEY, userContact.getId(),
                            System.currentTimeMillis(), System.currentTimeMillis() + tokenDuration, false);
                    AuthToken newToken = authService.updateAuthTokenCode(authToken);
                    // 新令牌
                    user.setAuthToken(newToken);

                    ContactManager.getInstance().updateContact(userContact.getDomain().getName(),
                            userContact.getId(), user.getName(), user.toJSON(),
                            contact.getDevice());

                    // 更新个人知识记忆
                    this.updatePersonalKnowledgeBase(ContactManager.getInstance().getAuthToken(userContact.getDomain().getName(),
                            userContact.getId()), user);

                    return user;
                }
                else {
                    Logger.d(this.getClass(), "#checkInUser - Incorrect password: " + contact.getId() + " - "
                            + userName + " - " + password);
                    return null;
                }
            }
        }
    }

    /**
     * 更新用户的个人知识记忆（异步）。
     *
     * <p>把用户档案（含会员信息）渲染为 Markdown，写入其个人知识库中标题为
     * {@link User#KnowledgeTitle} 的文章；不存在则创建并激活，存在则更新后重新激活。
     * 个人知识库不存在时会先创建。</p>
     *
     * @param authToken 访问令牌。
     * @param user 用户。
     */
    private void updatePersonalKnowledgeBase(AuthToken authToken, User user) {
        KnowledgeBase base = this.getKnowledgeFramework().getKnowledgeBase(user.getId(), User.KnowledgeBaseName);
        if (null == base) {
            this.getKnowledgeFramework().newKnowledgeBase(authToken.getCode(), User.KnowledgeBaseName,
                    User.KnowledgeBaseDisplayName, "Profile", KnowledgeScope.Private);

            base = this.getKnowledgeFramework().getKnowledgeBase(user.getId(), User.KnowledgeBaseName);
            if (null == base) {
                Logger.e(this.getClass(), "#updatePersonalKnowledgeBase - Failed: " + user.getId());
                return;
            }

            KnowledgeProfile profile = base.getProfile();
            Logger.d(this.getClass(), "#updatePersonalKnowledgeBase - New personal base: " + user.getId() + " - " +
                    profile.scope.name + "/" + profile.maxSize + "/" + profile.state);
        }

        final KnowledgeBase knowledgeBase = base;
        this.taskExecutor.execute(new Runnable() {
            @Override
            public void run() {
                if (Logger.isDebugLevel()) {
                    Logger.d(this.getClass(), "#updatePersonalKnowledgeBase - " + user.getId() +
                            " - " + knowledgeBase.getName());
                }

                StringBuilder markdown = new StringBuilder();
                markdown.append(user.markdown());
                Membership membership = ContactManager.getInstance().getMembershipSystem().getMembership(
                        authToken.getDomain(), user.getId(), Membership.STATE_NORMAL);
                markdown.append(UserProfileRenderer.makeMembership(user, membership));

                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(System.currentTimeMillis());

                List<KnowledgeArticle> articleList = knowledgeBase.getKnowledgeArticlesByTitle(User.KnowledgeTitle);
                if (articleList.isEmpty()) {
                    if (Logger.isDebugLevel()) {
                        Logger.d(this.getClass(), "#updatePersonalKnowledgeBase - create profile data: "
                                + authToken.getContactId());
                    }

                    KnowledgeArticle article = new KnowledgeArticle(authToken.getDomain(), authToken.getContactId(),
                            User.KnowledgeBaseName,
                            "Profile", User.KnowledgeTitle, markdown.toString(), user.getName(),
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH) + 1,
                            calendar.get(Calendar.DATE),
                            System.currentTimeMillis(), KnowledgeScope.Private);
                    // 添加
                    article = knowledgeBase.appendKnowledgeArticle(article);
                    // 激活
                    knowledgeBase.activateKnowledgeArticles(Collections.singletonList(article.getId()),
                            TextSplitter.None);
                }
                else {
                    if (Logger.isDebugLevel()) {
                        Logger.d(this.getClass(), "#updatePersonalKnowledgeBase - update profile data: "
                                + authToken.getContactId());
                    }

                    KnowledgeArticle article = articleList.get(0);
                    article.content = markdown.toString();
                    // 更新
                    knowledgeBase.updateKnowledgeArticle(article);
                    // 激活
                    knowledgeBase.activateKnowledgeArticles(Collections.singletonList(article.getId()),
                            TextSplitter.None);
                }
            }
        });
    }

    /**
     * 注销用户。
     *
     * <p>联系人标记 SignOut 并在名称后追加掩码、更新联系人上下文、删除访问令牌。</p>
     *
     * @param contact 联系人。
     * @return 返回不带令牌的用户。
     */
    public User signOutUser(Contact contact) {
        Logger.i(this.getClass(), "#signOutUser - User: " + contact.getId());

        ContactManager.getInstance().setContactMask(contact.getDomain().getName(), contact.getId(),
                ContactMask.SignOut);

        User user = new User(contact.getContext());
        user.setAuthToken(null);
        // 更新名称
        ContactManager.getInstance().updateContact(contact.getDomain().getName(), contact.getId(),
                contact.getName() + "-" + ContactMask.SignOut.mask, user.toJSON(),
                contact.getDevice());
        // 删除令牌
        AuthService authService = this.getAuthService();
        authService.deleteToken(contact.getDomain().getName(), contact.getId());
        return user;
    }

    /**
     * 使用邀请码激活会员。
     *
     * <p>特殊码 {@code 941017} 表示强制取消会员。激活成功后绑定邀请码
     * 并更新个人知识记忆中的会员信息。</p>
     *
     * @param token 访问令牌。
     * @param channel 渠道代码。
     * @param invitationCode 邀请码。
     * @return 返回会员；联系人不存在或邀请码无效返回 {@code null}，
     *         特殊码场景返回取消结果。
     */
    public Membership activateMembership(AuthToken token, String channel, String invitationCode) {
        Contact contact = ContactManager.getInstance().getContact(token.getDomain(), token.getContactId());
        if (null == contact) {
            return null;
        }

        // 处理特殊码
        if (invitationCode.equals("941017")) {
            // 强制取消会员身份
            return this.cancelMembership(token);
        }

        // 校验邀请码
        MembershipSystem.InvitationCode mic = ContactManager.getInstance().getMembershipSystem().verifyInvitationCode(
                invitationCode);
        if (null == mic) {
            return null;
        }

        JSONObject context = contact.getContext();
        if (null == context) {
            context = new JSONObject();
        }
        context.put("channel", channel);
        Membership membership = ContactManager.getInstance().getMembershipSystem().activateMembership(token.getDomain(),
                token.getContactId(), "MindEcho", mic, context);
        if (null != membership) {
            // 绑定验证码
            ContactManager.getInstance().getMembershipSystem().bindInvitationCode(mic, token.getContactId());

            // 更新会员信息
            this.updatePersonalKnowledgeBase(token, new User(context));
        }
        return membership;
    }

    /**
     * 取消指定用户的会员。
     *
     * <p>取消成功后同步更新个人知识记忆中的会员信息。</p>
     *
     * @param token 访问令牌。
     * @return 返回被取消的会员；联系人不存在或原本无会员返回 {@code null}。
     */
    public Membership cancelMembership(AuthToken token) {
        Contact contact = ContactManager.getInstance().getContact(token.getDomain(), token.getContactId());
        if (null == contact) {
            return null;
        }

        Membership membership = ContactManager.getInstance().getMembershipSystem().cancelMembership(
                token.getDomain(), token.getContactId());
        if (null != membership) {
            // 更新会员信息
            this.updatePersonalKnowledgeBase(token, new User(contact.getContext()));
        }
        return membership;
    }

    /**
     * 生成用户近一年的聊天词云。
     *
     * <p>读取该联系人近 365 天的历史问答，对问题与回答分别做 TF-IDF 分词
     * （各取前 10 个词）后汇入词云。</p>
     *
     * @param authToken 访问令牌。
     * @return 返回词云。
     */
    public WordCloud createWordCloud(AuthToken authToken) {
        WordCloud wordCloud = new WordCloud();

        long end = System.currentTimeMillis();
        long start = end - (365L * 24 * 60 * 60 * 1000);
        List<AIGCChatHistory> chatHistories = this.storage.readHistoriesByContactId(
                authToken.getContactId(), authToken.getDomain(), start, end);

        TFIDFAnalyzer analyzer = new TFIDFAnalyzer(this.tokenizer);
        for (AIGCChatHistory history : chatHistories) {
            List<String> words = analyzer.analyzeOnlyWords(history.queryContent, 10);
            for (String word : words) {
                wordCloud.addWord(word.trim());
            }
            words = analyzer.analyzeOnlyWords(history.answerContent, 10);
            for (String word : words) {
                wordCloud.addWord(word.trim());
            }
        }

        return wordCloud;
    }

    /**
     * 获取全部模型配置。
     *
     * @return 返回模型配置列表；服务未就绪返回 {@code null}。
     */
    public List<ModelConfig> getModelConfigs() {
        if (!this.isStarted()) {
            return null;
        }

        return this.storage.getModelConfigs();
    }

    /**
     * 按模型名列表获取模型配置。
     *
     * @param modelNames 模型名数组。
     * @return 返回模型配置列表；服务未就绪返回 {@code null}。
     */
    public List<ModelConfig> getModelConfigs(JSONArray modelNames) {
        if (!this.isStarted()) {
            return null;
        }

        return this.storage.getModelConfigs(JSONUtils.toStringList(modelNames));
    }

    /**
     * 获取已启用的应用通知列表。
     *
     * @return 返回通知列表；服务未就绪或读取异常返回空列表。
     */
    public List<Notification> getNotifications() {
        if (!this.isStarted()) {
            return new ArrayList<>();
        }

        try {
            return this.storage.readEnabledNotifications();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * 获取联系人偏好设置。
     *
     * @param contactId 联系人 ID。
     * @return 返回偏好；存储器不可用或无记录返回 {@code null}。
     */
    public ContactPreference getPreference(long contactId) {
        if (null == this.storage) {
            Logger.w(this.getClass(), "#getPreference - AIGC storage is NOT available");
            return null;
        }

        return this.storage.readContactPreference(contactId);
    }

    /**
     * 通过邀请码查询令牌。
     *
     * @param invitationCode 邀请码。
     * @return 返回对应令牌码；存储器不可用或无记录返回 {@code null}。
     */
    public String queryTokenByInvitation(String invitationCode) {
        if (null == this.storage) {
            Logger.w(this.getClass(), "#queryTokenByInvitation - AIGC storage is NOT available");
            return null;
        }

        return this.storage.readTokenByInvitation(invitationCode);
    }

    /**
     * 为令牌创建新的邀请码（6 位数字，异步写入）。
     *
     * @param token 访问令牌码。
     * @return 返回新邀请码；存储器不可用返回 {@code null}，写入失败仍返回邀请码。
     */
    public String newInvitationForToken(String token) {
        if (null == this.storage) {
            Logger.w(this.getClass(), "#newInvitationForToken - AIGC storage is NOT available");
            return null;
        }

        String invitation = Utils.randomNumberString(6);
        if (!this.storage.writeInvitation(invitation, token)) {
            Logger.e(this.getClass(), "#newInvitationForToken - write invitation failed: " + token);
        }
        return invitation;
    }

    /**
     * 检测或注入验证码登录令牌。
     *
     * <p>手机号对应的联系人不存在时，创建联系人并签发 5 年有效期令牌；
     * 已存在时直接返回其现有令牌。</p>
     *
     * @param phoneNumber 手机号。
     * @param userName 用户名，可为 {@code null}。
     * @return 返回访问令牌；手机号格式非法返回 {@code null}。
     */
    public AuthToken getOrInjectAuthToken(String phoneNumber, String userName) {
        long phone = 0;
        try {
            phone = Long.parseLong(phoneNumber);
        } catch (Exception e) {
            Logger.e(this.getClass(), "");
            return null;
        }

        final String domain = AuthConsts.DEFAULT_DOMAIN;
        final String appKey = AuthConsts.DEFAULT_APP_KEY;

        AuthToken authToken = null;

        ContactSearchResult searchResult = ContactManager.getInstance().searchWithContactId(domain, phone);
        if (searchResult.getContactList().isEmpty()) {
            // 没有该联系人
            Contact contact = ContactManager.getInstance().newContact(phone,
                    domain, (null != userName) ? userName : phoneNumber, null, null);
            // 创建令牌
            AuthService authService = this.getAuthService();
            // 5年有效时长
            authToken = authService.applyToken(domain, appKey, contact.getId(), 5L * 365 * 24 * 60 * 60 * 1000);
        }
        else {
            // 有该联系人
            AuthService authService = this.getAuthService();
            authToken = authService.queryAuthTokenByContactId(phone);
        }

        return authToken;
    }

    //-------- App Interface - End --------

    /**
     * 获取令牌。
     *
     * @param tokenCode 令牌码。
     * @return 返回访问令牌；令牌码为 {@code null}、授权服务未就绪或令牌不存在
     *         返回 {@code null}。
     */
    public AuthToken getToken(String tokenCode) {
        if (null == tokenCode) {
            return null;
        }

        AuthService authService = this.getAuthService();
        if (null == authService) {
            return null;
        }

        AuthToken authToken = authService.getToken(tokenCode);
        return authToken;
    }

    /**
     * 获取知识库架构。
     *
     * @return 返回知识框架；启动完成前返回 {@code null}。
     */
    public KnowledgeFramework getKnowledgeFramework() {
        return this.knowledgeFramework;
    }

    /**
     * 获取指定联系人的知识库信息列表。
     *
     * @param tokenCode 访问令牌码。
     * @return 返回知识库信息列表；令牌无效返回 {@code null}。
     */
    public List<KnowledgeBaseInfo> getKnowledgeBaseInfoList(String tokenCode) {
        AuthToken authToken = this.getToken(tokenCode);
        if (null == authToken) {
            return null;
        }

        return this.knowledgeFramework.getKnowledgeBaseInfos(authToken.getContactId());
    }

    /**
     * 按令牌与知识库名获取知识库实例。
     *
     * @param tokenCode 访问令牌码。
     * @param baseName 知识库名。
     * @return 返回知识库；令牌无效或知识库不存在返回 {@code null}。
     */
    public KnowledgeBase getKnowledgeBase(String tokenCode, String baseName) {
        AuthToken authToken = this.getToken(tokenCode);
        if (null == authToken) {
            return null;
        }
        return this.getKnowledgeBase(authToken.getContactId(), baseName);
    }

    /**
     * 按令牌与分类获取知识库实例列表。
     *
     * @param tokenCode 访问令牌码。
     * @param category 分类名。
     * @return 返回知识库列表；令牌无效返回 {@code null}。
     */
    public List<KnowledgeBase> getKnowledgeBaseByCategory(String tokenCode, String category) {
        AuthToken authToken = this.getToken(tokenCode);
        if (null == authToken) {
            return null;
        }
        return this.getKnowledgeBaseByCategory(authToken.getContactId(), category);
    }

    /**
     * 按联系人与知识库名获取知识库实例。
     *
     * @param contactId 联系人 ID。
     * @param baseName 知识库名。
     * @return 返回知识库；知识框架未就绪或知识库不存在返回 {@code null}。
     */
    public synchronized KnowledgeBase getKnowledgeBase(Long contactId, String baseName) {
        return this.knowledgeFramework.getKnowledgeBase(contactId, baseName);
    }

    /**
     * 按联系人与分类获取知识库实例列表。
     *
     * @param contactId 联系人 ID。
     * @param category 分类名。
     * @return 返回知识库列表；知识框架未就绪返回 {@code null}。
     */
    public synchronized List<KnowledgeBase> getKnowledgeBaseByCategory(Long contactId, String category) {
        return this.knowledgeFramework.getKnowledgeBaseByCategory(contactId, category);
    }

    /**
     * 对历史问答进行评价。
     *
     * @param token
     * @param historySN
     * @param feedback
     */
    public void evaluate(String token, long historySN, int feedback) {
        AIGCChannel channel = this.getChannelByToken(token);
        if (null != channel) {
            channel.feedbackRecord(historySN, feedback);
        }

        this.taskExecutor.execute(new Runnable() {
            @Override
            public void run() {
                storage.updateHistoryFeedback(historySN, feedback);
            }
        });
    }

    /**
     * 获取指定频道。
     *
     * @param channelCode 频道码。
     * @return 不存在返回 {@code null}。
     */
    public AIGCChannel getChannel(String channelCode) {
        return this.channelManager.get(channelCode);
    }

    /**
     * 通过访问令牌获取对应的频道。
     *
     * @param tokenCode 访问令牌码。
     * @return 不存在返回 {@code null}。
     */
    public AIGCChannel getChannelByToken(String tokenCode) {
        return this.channelManager.getByToken(tokenCode);
    }

    /**
     * 创建频道。
     *
     * @param token       访问令牌码。
     * @param participant 参与方名称。
     * @param channelCode 频道码。
     * @param language    语言。
     * @return 令牌无效时返回 {@code null}。
     */
    public AIGCChannel createChannel(String token, String participant, String channelCode, Language language) {
        return this.channelManager.create(token, participant, channelCode, language);
    }

    /**
     * 创建频道。
     *
     * @param authToken   访问令牌。
     * @param participant 参与方名称。
     * @param channelCode 频道码。
     * @param language    语言。
     * @return 返回新创建的频道。
     */
    public AIGCChannel createChannel(AuthToken authToken, String participant, String channelCode, Language language) {
        return this.channelManager.create(authToken, participant, channelCode, language);
    }

    /**
     * 申请频道。
     *
     * @param token       访问令牌码。
     * @param participant 参与方名称。
     * @return 被拒绝时返回 {@code null}。
     */
    public AIGCChannel requestChannel(String token, String participant) {
        return this.channelManager.request(token, participant);
    }

    /**
     * 停止频道正在进行的操作。
     *
     * @param channelCode 指定频道码。
     * @return 返回频道，不存在时返回 {@code null}。
     */
    public AIGCChannel stopProcessing(String channelCode) {
        return this.channelManager.stopProcessing(channelCode);
    }

    /**
     * 保活频道。
     *
     * @param token 频道码。
     * @return 频道不存在返回 {@code false}。
     */
    public boolean keepAliveChannel(String token) {
        return this.channelManager.keepAlive(token);
    }

    /**
     * 提交事件到事件中心。
     *
     * @param event 事件。
     * @return 返回事件处理结果。
     */
    public EventResult submitEvent(Event event) {
        return Explorer.getInstance().fireEvent(event);
    }

    /**
     * 生成文本内容（频道异步方式）。
     *
     * <p>校验服务状态、内容长度、频道存在性与处理中状态后，按单元名
     * （找不到时回退到会话子任务）选择单元，构造 {@link GenerateTextUnitMeta}
     * 交由任务执行器排队执行；同一单元上的请求串行消化。</p>
     *
     * @param channelCode 频道代码。
     * @param content 内容。
     * @param unitName 单元名。
     * @param option 生成参数设置。
     * @param histories 历史记录列表。
     * @param maxHistories 最大历史记录数量。
     * @param attachments 附件信息。
     * @param categories 分类信息。
     * @param recordable 是否记录到库。
     * @param networking 是否进行联网操作。
     * @param listener 监听器。
     * @return 任务受理返回 {@code true}；服务未就绪、内容超限、频道不存在或
     *         正在处理、无可用单元返回 {@code false}。
     */
    public boolean generateText(String channelCode, String content, String unitName, GeneratingOption option,
                                List<GeneratingRecord> histories, int maxHistories, List<Attachment> attachments,
                                List<String> categories, boolean recordable, boolean networking,
                                GenerateTextListener listener) {
        if (!this.isStarted()) {
            Logger.w(AIGCService.class, "#generateText - Service is NOT ready");
            return false;
        }

        if (content.length() > ModelConfig.getPromptLengthLimit(unitName)) {
            Logger.w(AIGCService.class, "#generateText - Content length greater than "
                    + ModelConfig.getPromptLengthLimit(unitName));
            return false;
        }

        // 获取频道
        AIGCChannel channel = this.channelManager.get(channelCode);
        if (null == channel) {
            Logger.w(AIGCService.class, "#generateText - Can NOT find AIGC channel: " + channelCode);
            return false;
        }

        // 如果频道正在应答上一次问题，则返回 null
        if (channel.isProcessing()) {
            Logger.w(AIGCService.class, "#generateText - Channel is processing: " + channelCode);
            return false;
        }

        // 设置为正在处理
        channel.setProcessing(true);

        // 查找有该能力的单元
        // 优先按照单元名称进行检索，然后按照描述进行检索
        AIGCUnit unit = null;
        if (this.useRelay) {
            unit = Relay.getInstance().selectUnit(unitName);
        }
        else {
            unit = this.selectUnitByName(unitName);
            if (null == unit) {
                unit = this.selectUnitBySubtask(AICapability.NaturalLanguageProcessing.Conversational);
            }
        }

        if (null == unit) {
            Logger.w(AIGCService.class, "#generateText - No conversational task unit setup in server");
            channel.setProcessing(false);
            return false;
        }

        final GenerateTextUnitMeta meta = new GenerateTextUnitMeta(this, unit, channel, content, option, categories,
                histories, attachments, listener);
        meta.setMaxHistories(maxHistories);
        meta.setRecordHistoryEnabled(recordable);
        meta.setNetworkingEnabled(networking);

        // 取队列 → 起任务 → 收尾：同一单元上的请求串行消化
        this.taskExecutor.submitGenerateText(meta);

        return true;
    }

    /**
     * 返回生成文本单元实时运行计数。
     *
     * @return 返回计数表。
     */
    public Map<String, AtomicInteger> getGenerateTextUnitRealtimeCount() {
        return this.taskExecutor.getUnitTaskCountMap();
    }

    /**
     * 同步方式生成文本。
     *
     * @param authToken 访问令牌。
     * @param unitName 单元名。
     * @param prompt 提示词。
     * @param option 生成参数设置。
     * @return 返回生成记录；无单元、无联系人或生成失败返回 {@code null}。
     */
    public GeneratingRecord syncGenerateText(AuthToken authToken, String unitName, String prompt, GeneratingOption option) {
        AIGCUnit unit = this.selectUnitByName(unitName);
        if (null == unit) {
            return null;
        }

        Contact participant = ContactManager.getInstance().getContact(authToken.getDomain(), authToken.getContactId());
        if (null == participant) {
            Logger.w(this.getClass(), "#syncGenerateText(AuthToken) - Can NOT find participant: " + authToken.getCode());
            return null;
        }

        return this.syncGenerateText(unitName, prompt, option, null, participant);
    }

    /**
     * 同步方式生成文本。
     *
     * @param unitName 单元名。
     * @param prompt 提示词。
     * @param option 生成参数设置。
     * @param history 历史记录列表。
     * @param participantContact 参与方联系人。
     * @return 返回生成记录；无单元或生成失败返回 {@code null}。
     */
    public GeneratingRecord syncGenerateText(String unitName, String prompt, GeneratingOption option,
                                             List<GeneratingRecord> history, Contact participantContact) {
        AIGCUnit unit = this.selectUnitByName(unitName);
        if (null == unit) {
            Logger.w(this.getClass(), "#syncGenerateText - Can NOT find unit: " + unitName);
            return null;
        }
        return this.syncGenerateText(unit, prompt, option, history, participantContact);
    }

    /**
     * 同步方式生成文本（按联系人 ID 选择单元）。
     *
     * @param authToken 访问令牌。
     * @param unitName 单元名。
     * @param prompt 提示词。
     * @param option 生成参数设置。
     * @param history 历史记录列表。
     * @param participantContact 参与方联系人。
     * @return 返回生成记录；无单元或生成失败返回 {@code null}。
     */
    public GeneratingRecord syncGenerateText(AuthToken authToken, String unitName, String prompt, GeneratingOption option,
                                            List<GeneratingRecord> history, Contact participantContact) {
        AIGCUnit unit = this.selectUnitByName(unitName, authToken.getContactId());
        if (null == unit) {
            Logger.w(this.getClass(), "#syncGenerateText - Can NOT find unit: " + unitName);
            return null;
        }
        return this.syncGenerateText(unit, prompt, option, history, participantContact);
    }

    /**
     * 同步方式生成文本（底层实现）。
     *
     * <p>经 {@code cellet.transmit} 向单元发送 {@code TextToText} 请求
     * （超时 8 分钟），中继模式下走中继通道。应答解析出回答与思维链，
     * 并按单元做中文过滤。transmit 抛异常时也会递减实时计数并复位运行标志。</p>
     *
     * @param unit 单元。
     * @param prompt 提示词。
     * @param option 生成参数设置。
     * @param history 历史记录列表。
     * @param participantContact 参与方联系人，为 {@code null} 时使用单元所属联系人。
     * @return 返回生成记录；传输失败、状态码非 Ok 或应答缺字段返回 {@code null}。
     */
    public GeneratingRecord syncGenerateText(AIGCUnit unit, String prompt, GeneratingOption option,
                                   List<GeneratingRecord> history, Contact participantContact) {
        AtomicInteger count = increaseUnitCounter(unit.getCapability().getName());

        Packet request = null;
        ActionDialect dialect = null;
        long sn = Utils.generateSerialNumber();
        unit.setRunning(true);
        try {
            if (this.useRelay) {
                Logger.d(this.getClass(), "#syncGenerateText - Relay - \"" + unit.getCapability().getName() + "\" - history:"
                        + ((null != history) ? history.size() : 0));
                return Relay.getInstance().generateText(Utils.randomString(16),
                        unit.getCapability().getName(), prompt, option, history);
            }

            JSONArray historyArray = new JSONArray();
            if (null != history) {
                for (GeneratingRecord record : history) {
                    historyArray.put(record.toJSON());
                }
            }

            Contact participant = (null == participantContact) ?
                    unit.getContact() : participantContact;

            JSONObject data = new JSONObject();
            data.put("unit", unit.getCapability().getName());
            data.put("content", prompt);
            data.put("participant", participant.toCompactJSON());
            data.put("history", historyArray);
            data.put("option", (null == option) ? (new GeneratingOption()).toJSON() : option.toJSON());

            request = new Packet(AIGCAction.TextToText.name, data);
            dialect = this.cellet.transmit(unit.getContext(), request.toDialect(),
                    8 * 60 * 1000, sn);
            if (null == dialect) {
                Logger.w(AIGCService.class, "#syncGenerateText - transmit failed, sn:" + sn
                        + " - " + unit.getCapability().getName() + "@" + unit.getContact().getId());
                // 记录故障
                unit.markFailure(AIGCStateCode.UnitError.code, System.currentTimeMillis(), participant.getId());
                return null;
            }
        } finally {
            // 实时计数与运行标志同处收口：new Packet / transmit 抛异常时也必须递减，
            // 否则计数会在异常路径上单调上涨。
            count.decrementAndGet();
            unit.setRunning(false);
        }

        Packet response = new Packet(dialect);
        int stateCode = Packet.extractCode(response);
        if (stateCode != AIGCStateCode.Ok.code) {
            Logger.e(AIGCService.class, "#syncGenerateText - failed, state code:" + stateCode
                    + " - " + unit.getCapability().getName() + "@" + unit.getContact().getId());
            return null;
        }
        JSONObject payload = Packet.extractDataPayload(response);

        String responseText = "";
        String thoughtText = "";
        try {
            responseText = payload.getString("response");
            responseText = responseText.trim();
            thoughtText = payload.getString("thought");
            thoughtText = thoughtText.trim();
        } catch (Exception e) {
            Logger.w(AIGCService.class, "#syncGenerateText - failed, sn:" + sn
                    + " - " + unit.getCapability().getName() + "@" + unit.getContact().getId());
            return null;
        }

        // 过滤中文字符
        responseText = TextUtils.filterChinese(unit, responseText);
        return new GeneratingRecord(request.sn, unit.getCapability().getName(), prompt, responseText, thoughtText);
    }

    /**
     * 生成文本（{@link Generatable} 接口实现，同步方式）。
     *
     * @param unitName 单元名。
     * @param prompt 提示词。
     * @param option 生成参数设置。
     * @param history 历史记录列表。
     * @return 返回生成记录；无可用单元或生成失败返回 {@code null}。
     */
    @Override
    public GeneratingRecord generateText(String unitName, String prompt, GeneratingOption option,
                                         List<GeneratingRecord> history) {
        return this.syncGenerateText(unitName, prompt, option, history, null);
    }

    /**
     * 生成文本（频道异步方式，调用方自带单元）。
     *
     * <p>与 {@link #generateText(String, String, String, GeneratingOption, List, int, List, List, boolean, boolean, GenerateTextListener)}
     * 的区别：单元由调用方选定，且不联网；无单元数据时异步回调失败，
     * 避免在调用方线程上回调。</p>
     *
     * @param channel 频道。
     * @param unit 单元；中继模式下会按能力名重新选择。
     * @param query 原始提问（用于记录）。
     * @param prompt 提示词。
     * @param option 生成参数设置。
     * @param histories 历史记录列表。
     * @param maxHistories 最大历史记录数量。
     * @param attachments 附件信息。
     * @param categories 分类信息。
     * @param recordable 是否记录到库。
     * @param listener 监听器。
     */
    public void generateText(AIGCChannel channel, AIGCUnit unit, String query, String prompt, GeneratingOption option,
                             List<GeneratingRecord> histories, int maxHistories, List<Attachment> attachments,
                             List<String> categories, boolean recordable, GenerateTextListener listener) {
        if (this.useRelay) {
            unit = Relay.getInstance().selectUnit(unit.getCapability().getName());
        }

        if (null == unit) {
            // 没有单元数据：异步回告失败，避免在调用方线程上回调
            this.taskExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    listener.onFailed(channel, AIGCStateCode.NotFound);
                }
            });
            return;
        }

        final GenerateTextUnitMeta meta = new GenerateTextUnitMeta(this, unit, channel, prompt, option, categories,
                histories, attachments, listener);
        meta.setMaxHistories(maxHistories);
        meta.setOriginalQuery(query);
        meta.setRecordHistoryEnabled(recordable);
        meta.setNetworkingEnabled(false);

        if (Logger.isDebugLevel()) {
            if (null != histories) {
                Logger.d(this.getClass(), "#generateText - histories size: " + histories.size());
            }
        }

        // 取队列 → 起任务 → 收尾：同一单元上的请求串行消化
        this.taskExecutor.submitGenerateText(meta);
    }

    /**
     * 执行多模态任务。
     *
     * <p>频道不存在时按频道码自动创建；正在处理或无对应能力单元时拒绝。</p>
     *
     * @param tokenCode 访问令牌码。
     * @param channelCode 频道码。
     * @param input 多模态输入。
     * @param listener 监听器。
     * @return 任务受理返回 {@code true}；服务未就绪、频道正在处理或无单元
     *         返回 {@code false}。
     */
    public boolean executeMultimodal(String tokenCode, String channelCode,
                                     MultimodalInput input, MultimodalListener listener) {
        if (!this.isStarted()) {
            Logger.w(AIGCService.class, "#executeMultimodal - Service is NOT ready");
            return false;
        }

        // 获取频道
        AIGCChannel channel = this.channelManager.get(channelCode);
        if (null == channel) {
            Logger.d(AIGCService.class, "#executeMultimodal - Can NOT find channel, create new channel: " + channelCode);
            // 创建频道
            channel = this.createChannel(tokenCode, "User-" + channelCode, channelCode, Language.Chinese);
        }

        // 如果频道正在应答上一次问题，则返回 null
        if (channel.isProcessing()) {
            Logger.w(AIGCService.class, "#executeMultimodal - Channel is processing: " + channelCode);
            return false;
        }

        channel.setProcessing(true);

        // 查找单元
        AIGCUnit unit = this.selectUnitByName(input.unit);
        if (null == unit) {
            Logger.w(AIGCService.class, "#executeMultimodal - No task unit setup in server: " + input.unit);
            channel.setProcessing(false);
            return false;
        }

        final MultimodalUnitMeta meta = new MultimodalUnitMeta(this, unit, channel, input, listener);

        // 取队列 → 起任务 → 收尾，统一由单元任务队列执行器处理
        this.taskExecutor.submitMultimodal(meta);

        return true;
    }

    /**
     * 生成指定内容的摘要（异步）。
     *
     * <p>在后台线程调用白泽单元提取摘要，仅返回摘要文本。</p>
     *
     * @param text 原文。
     * @param listener 监听器。
     * @return 任务受理返回 {@code true}；服务未就绪返回 {@code false}。
     */
    public boolean generateSummarization(String text, SummarizationListener listener) {
        if (!this.isStarted()) {
            return false;
        }

        final SummarizationListener summarizationListener = listener;

        this.taskExecutor.execute(new Runnable() {
            @Override
            public void run() {
                GeneratingRecord result = generateText(ModelConfig.BAIZE_UNIT,
                        "请提取以下内容的摘要信息，只返回摘要：\n\n" + text,
                        null, null);
                if (null == result) {
                    summarizationListener.onFailed(text, AIGCStateCode.Failure);
                    return;
                }

                summarizationListener.onCompleted(text, result.answer);
            }
        });

        return true;
    }

    /**
     * 文本生成图片（按频道码）。
     *
     * @param channelCode 频道码。
     * @param text 提示词。
     * @param unitName 单元名。
     * @param listener 监听器。
     * @return 任务受理返回 {@code true}；服务未就绪、频道不存在、正在处理、
     *         无单元或单元非文生图类型返回 {@code false}。
     */
    public boolean generateImage(String channelCode, String text, String unitName, TextToImageListener listener) {
        if (!this.isStarted()) {
            return false;
        }

        // 获取频道
        AIGCChannel channel = this.channelManager.get(channelCode);
        if (null == channel) {
            Logger.w(AIGCService.class, "#generateImage - Can NOT find AIGC channel: " + channelCode);
            return false;
        }

        return this.generateImage(channel, text, unitName, listener);
    }

    /**
     * 文本生成图片（频道实例方式）。
     *
     * @param channel 频道。
     * @param text 提示词。
     * @param unitName 单元名。
     * @param listener 监听器。
     * @return 任务受理返回 {@code true}；服务未就绪、频道正在处理、无单元或
     *         单元非文生图类型返回 {@code false}。
     */
    public boolean generateImage(AIGCChannel channel, String text, String unitName, TextToImageListener listener) {
        if (!this.isStarted()) {
            return false;
        }

        // 如果频道正在应答上一次问题，则返回 null
        if (channel.isProcessing()) {
            Logger.w(AIGCService.class, "#generateImage - Channel is processing: " + channel.getCode());
            return false;
        }

        // 设置为正在处理
        channel.setProcessing(true);

        // 查找有该能力的单元
        AIGCUnit unit = this.selectUnitByName(unitName);
        if (null == unit || !ModelConfig.isTextToImageUnit(unitName)) {
            Logger.w(AIGCService.class, "No text to image unit: " + unitName);
            channel.setProcessing(false);
            return false;
        }

//        unit = this.selectUnitBySubtask(AICapability.Multimodal.TextToImage);
//        if (null == unit) {
//            Logger.w(AIGCService.class, "No text to image unit setup in server");
//            channel.setProcessing(false);
//            return false;
//        }

        final UnitMeta meta = new TextToImageUnitMeta(this, unit, channel, text, listener);

        // 取队列 → 起任务 → 收尾，统一由单元任务队列执行器处理
        this.taskExecutor.submitTextToImage(meta);

        return true;
    }

    /**
     * 文本生成文件。
     *
     * <p>优先使用白泽 2 单元，回退白泽单元；以附件的查询文件标签为线索生成文件。</p>
     *
     * @param channel 频道。
     * @param text 提示词。
     * @param attachment 参考的生成记录（含查询文件标签）。
     * @param listener 监听器。
     * @return 任务受理返回 {@code true}；服务未就绪或无单元返回 {@code false}。
     */
    public boolean generateFile(AIGCChannel channel, String text, GeneratingRecord attachment, TextToFileListener listener) {
        if (!this.isStarted()) {
            return false;
        }

        AIGCUnit unit = this.selectUnitByName(ModelConfig.BAIZE_2_UNIT);
        if (null == unit) {
            unit = this.selectUnitByName(ModelConfig.BAIZE_UNIT);
            if (null == unit) {
                Logger.e(this.getClass(), "#generateFile - No unit, token: " + channel.getAuthToken().getCode());
                return false;
            }
        }

        final UnitMeta meta = new TextToFileUnitMeta(this, unit, channel, text, attachment.queryFileLabels, listener);

        // 取队列 → 起任务 → 收尾，统一由单元任务队列执行器处理
        this.taskExecutor.submitTextToFile(meta);

        return true;
    }

    /**
     * 提取文本关键词（异步）。
     *
     * <p>在后台线程调用白泽单元提取关键词，按中英文逗号拆分。</p>
     *
     * @param text 原文。
     * @param listener 监听器。
     * @return 任务受理返回 {@code true}；服务未就绪返回 {@code false}。
     */
    public boolean extractKeywords(String text, ExtractKeywordsListener listener) {
        if (!this.isStarted()) {
            return false;
        }

        final ExtractKeywordsListener extractKeywordsListener = listener;

        this.taskExecutor.execute(new Runnable() {
            @Override
            public void run() {
                GeneratingRecord result = generateText(ModelConfig.BAIZE_UNIT,
                        "提取下面文本内容的关键词，仅回复关键词，关键词之间使用逗号分隔：\n\n" + text,
                        null, null);

                if (null == result) {
                    extractKeywordsListener.onFailed(text, AIGCStateCode.Failure);
                    return;
                }

                String[] words = null;
                if (result.answer.contains(",")) {
                    words = result.answer.split(",");
                    extractKeywordsListener.onCompleted(text, Arrays.asList(words));
                }
                else if (result.answer.contains("，")) {
                    words = result.answer.split("，");
                    extractKeywordsListener.onCompleted(text, Arrays.asList(words));
                }
                else {
                    extractKeywordsListener.onCompleted(text, Arrays.asList(new String[]{result.answer}));
                }
            }
        });

        return true;
    }

    /**
     * 语义搜索。
     *
     * @param query 查询语句。
     * @param listener 监听器。
     * @return 任务受理返回 {@code true}；服务未就绪或无语义搜索单元
     *         返回 {@code false}。
     */
    public boolean semanticSearch(String query, SemanticSearchListener listener) {
        if (!this.isStarted()) {
            return false;
        }

        // 查找有该能力的单元
        AIGCUnit unit = this.selectUnitBySubtask(AICapability.NaturalLanguageProcessing.SemanticSearch);
        if (null == unit) {
            Logger.w(AIGCService.class, "No semantic search unit setup in server");
            return false;
        }

        final UnitMeta meta = new SemanticSearchUnitMeta(this, unit, query, listener);

        // 取队列 → 起任务 → 收尾，统一由单元任务队列执行器处理
        this.taskExecutor.submitSemanticSearch(meta);

        return true;
    }

    /**
     * 检索并重排序。
     *
     * @param queries 查询语句列表。
     * @param listener 监听器。
     * @return 任务受理返回 {@code true}；服务未就绪或无重排单元返回 {@code false}。
     */
    public boolean retrieveReRank(List<String> queries, RetrieveReRankListener listener) {
        if (!this.isStarted()) {
            return false;
        }

        // 查找有该能力的单元
        AIGCUnit unit = this.selectUnitBySubtask(AICapability.NaturalLanguageProcessing.RetrieveReRank);
        if (null == unit) {
            Logger.w(AIGCService.class, "No retrieve re-rank unit setup in server");
            return false;
        }

        final UnitMeta meta = new RetrieveReRankUnitMeta(this, unit, queries, listener);

        // 取队列 → 起任务 → 收尾，统一由单元任务队列执行器处理
        this.taskExecutor.submitRetrieveReRank(meta);

        return true;
    }

    /**
     * 同步方式检索并重排序。
     *
     * <p>按文件标签与查询语句做重排，内部通过等待回调结果实现同步语义
     * （最长等待 5 分钟）。</p>
     *
     * @param fileLabels 文件标签列表。
     * @param query 查询语句。
     * @return 返回重排结果；无重排单元返回 {@code null}，超时或失败返回空列表。
     */
    public List<RetrieveReRankResult> syncRetrieveReRank(List<FileLabel> fileLabels, String query) {
        AIGCUnit unit = this.selectUnitBySubtask(AICapability.NaturalLanguageProcessing.RetrieveReRank);
        if (null == unit) {
            Logger.w(this.getClass(), "#syncRetrieveReRank - No retrieve re-rank unit setup in server");
            return null;
        }

        final List<RetrieveReRankResult> result = new ArrayList<>();

        UnitMeta meta = new RetrieveReRankUnitMeta(this, unit, fileLabels, query, new RetrieveReRankListener() {
            @Override
            public void onCompleted(List<RetrieveReRankResult> retrieveReRankResults) {
                result.addAll(retrieveReRankResults);
                synchronized (result) {
                    result.notify();
                }
            }

            @Override
            public void onFailed(List<String> queries, AIGCStateCode stateCode) {
                synchronized (result) {
                    result.notify();
                }
            }
        });

        // 取队列 → 起任务 → 收尾，统一由单元任务队列执行器处理
        this.taskExecutor.submitRetrieveReRank(meta);

        synchronized (result) {
            try {
                result.wait(5 * 60 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return result;
    }

    /**
     * 查询联系人的用量数据。
     *
     * @param contactId 联系人 ID。
     * @return 返回各模型的用量列表（跳过无记录的模型）；服务未就绪返回 {@code null}。
     */
    public List<Usage> queryContactUsages(long contactId) {
        if (!this.isStarted()) {
            return null;
        }

        List<Usage> result = new ArrayList<>();

        List<ModelConfig> models = this.getModelConfigs();
        for (ModelConfig modelConfig : models) {
            Usage usage = this.storage.readUsage(contactId, modelConfig.getModel());
            if (null == usage) {
                // 跳过没有记录的模型
                continue;
            }
            result.add(usage);
        }

        return result;
    }

    /**
     * 自动语音识别。
     *
     * <p>支持文件码或外部 URL（自动下载）；文件经语音识别单元转写。</p>
     *
     * @param authToken 访问令牌。
     * @param fileCodeOrUrl 文件码或外部 URL。
     * @param listener 监听器。
     * @return 任务受理返回 {@code true}；文件获取失败或无语音识别单元
     *         返回 {@code false}。
     */
    public boolean automaticSpeechRecognition(AuthToken authToken, String fileCodeOrUrl,
                                              AutomaticSpeechRecognitionListener listener) {
        FileLabel fileLabel = null;
        if (TextUtils.isURL(fileCodeOrUrl)) {
            // 从外部链接下载
            fileLabel = this.downloadFile(authToken, fileCodeOrUrl);
            fileLabel.externalURL = fileCodeOrUrl;
        }
        else {
            fileLabel = this.getFile(authToken.getDomain(), fileCodeOrUrl);
        }

        if (null == fileLabel) {
            Logger.e(this.getClass(), "#automaticSpeechRecognition - Get file failed: " + fileCodeOrUrl);
            return false;
        }

        // 查找有该能力的单元
        AIGCUnit unit = this.selectUnitBySubtask(AICapability.AudioProcessing.AutomaticSpeechRecognition);
        if (null == unit) {
            Logger.w(this.getClass(), "#automaticSpeechRecognition - No task unit setup in server");
            return false;
        }

        final UnitMeta meta = new SpeechRecognitionUnitMeta(this, unit, authToken, fileLabel, listener);

        // 取队列 → 起任务 → 收尾：共享队列，最多 maxSpeechRecognitionConcurrences 个排空任务并发处理
        this.taskExecutor.submitSpeechRecognition(meta);

        return true;
    }

    /**
     * 说话者分割与分析（文件标签方式）。
     *
     * <p>同一文件的任务可能仍在执行，避免重复提交（直接返回原文件标签）。
     * 音频流允许插队优先处理。</p>
     *
     * @param authToken 访问令牌。
     * @param fileLabel 音频文件标签。
     * @param preprocess 是否预处理。
     * @param storage 是否落库。
     * @param jumpToFirst 是否插队优先处理。
     * @param sentiment 是否分析内容语料的正负面 / 中性指标。
     * @param listener 监听器。
     * @return 返回文件标签；无说话人分离单元返回 {@code null}，任务重复提交时
     *         返回原文件标签。
     */
    public FileLabel performSpeakerDiarization(AuthToken authToken, FileLabel fileLabel, boolean preprocess,
                                             boolean storage, boolean jumpToFirst, boolean sentiment,
                                             VoiceDiarizationListener listener) {
        // 查找有该能力的单元
        AIGCUnit unit = this.selectUnitBySubtask(AICapability.AudioProcessing.SpeakerDiarization);
        if (null == unit) {
            Logger.w(this.getClass(), "#performSpeakerDiarization - No task unit setup in server");
            return null;
        }

        // 同一文件的任务可能仍在执行，避免重复提交
        if (this.taskExecutor.hasPendingAudio(fileLabel.getFileCode())) {
            Logger.w(this.getClass(), "#performSpeakerDiarization - Re-submit the task for file: " +
                    fileLabel.getFileCode());
            return fileLabel;
        }

        final AudioUnitMeta meta = new AudioUnitMeta(this, unit, authToken, AIGCAction.SpeechDiarization,
                fileLabel, preprocess, storage, sentiment);
        meta.voiceDiarizationListener = listener;

        // 取队列 → 起任务 → 收尾：音频流允许插队优先处理
        this.taskExecutor.submitAudio(meta, jumpToFirst);

        return fileLabel;
    }

    /**
     * 说话者分割与分析（文件码或 URL 方式）。
     *
     * @param authToken 访问令牌。
     * @param fileCodeOrUrl 文件码或外部 URL（自动下载）。
     * @param preprocess 是否预处理。
     * @param storage 是否落库。
     * @param sentiment 是否分析内容语料的正负面 / 中性指标。
     * @param listener 监听器。
     * @return 返回文件标签；文件获取失败或无说话人分离单元返回 {@code null}。
     */
    public FileLabel performSpeakerDiarization(AuthToken authToken, String fileCodeOrUrl, boolean preprocess,
                                             boolean storage, boolean sentiment, VoiceDiarizationListener listener) {
        FileLabel fileLabel = null;
        if (TextUtils.isURL(fileCodeOrUrl)) {
            fileLabel = this.downloadFile(authToken, fileCodeOrUrl);
            if (null != fileLabel) {
                fileLabel.externalURL = fileCodeOrUrl;
            }
        }
        else {
            fileLabel = this.getFile(authToken.getDomain(), fileCodeOrUrl);
        }

        if (null == fileLabel) {
            Logger.e(this.getClass(), "#performSpeakerDiarization - Get file failed: " + fileCodeOrUrl);
            return null;
        }

        return this.performSpeakerDiarization(authToken, fileLabel, preprocess, storage, false, sentiment, listener);
    }

    /**
     * 注册语音能力模块级监听器。
     *
     * <p>供 {@code AIGCHost} 转发：业务模块在装载阶段订阅说话人分离完成事件，
     * 在结果上做角色映射等收尾操作。宿主不内置任何业务角色分类。</p>
     *
     * @param listener 监听器。
     * @return 注册成功返回 {@code true}；参数为 {@code null} 或已注册时
     *         返回 {@code false}。
     */
    public boolean registerSpeechListener(SpeechModuleListener listener) {
        if (null == listener) {
            return false;
        }

        boolean added = this.speechModuleListeners.addIfAbsent(listener);
        if (added) {
            Logger.i(this.getClass(), "#registerSpeechListener - Registered \""
                    + listener.getName() + "\", total: " + this.speechModuleListeners.size());
        }
        else {
            Logger.w(this.getClass(), "#registerSpeechListener - Listener \""
                    + listener.getName() + "\" is ALREADY registered");
        }
        return added;
    }

    /**
     * 注销语音能力模块级监听器。
     *
     * @param listener 监听器。
     * @return 注销成功返回 {@code true}；参数为 {@code null} 或未注册时
     *         返回 {@code false}。
     */
    public boolean unregisterSpeechListener(SpeechModuleListener listener) {
        if (null == listener) {
            return false;
        }

        boolean removed = this.speechModuleListeners.remove(listener);
        if (removed) {
            Logger.i(this.getClass(), "#unregisterSpeechListener - Unregistered \""
                    + listener.getName() + "\", total: " + this.speechModuleListeners.size());
        }
        else {
            Logger.w(this.getClass(), "#unregisterSpeechListener - Listener \""
                    + listener.getName() + "\" is NOT registered");
        }
        return removed;
    }

    /**
     * 向全部模块级监听器扇出「说话人分离完成」事件。
     *
     * <p>在请求级 {@link VoiceDiarizationListener} 回调<b>之前</b>调用，
     * 按注册顺序执行；单个监听器抛出异常只记录，不影响其他监听器、
     * 请求级回调与结果落库。</p>
     *
     * @param source 源音频文件标签。
     * @param diarization 分离结果。
     */
    public void notifyDiarizationListeners(FileLabel source, VoiceDiarization diarization) {
        if (this.speechModuleListeners.isEmpty()) {
            return;
        }

        for (SpeechModuleListener listener : this.speechModuleListeners) {
            try {
                listener.onDiarizationCompleted(source, diarization);
            }
            catch (Throwable t) {
                Logger.e(this.getClass(), "#notifyDiarizationListeners - Listener \""
                        + listener.getName() + "\" FAILED", (t instanceof Exception) ? (Exception) t : null);
            }
        }
    }

    /**
     * 获取指定文件的说话人分离结果。
     *
     * @param authToken 访问令牌。
     * @param fileCode 文件码。
     * @return 返回说话人分离结果（含文件信息）；无记录返回 {@code null}。
     */
    public VoiceDiarization getVoiceDiarization(AuthToken authToken, String fileCode) {
        VoiceDiarization voiceDiarization = this.storage.readVoiceDiarization(fileCode);
        if (null == voiceDiarization) {
            Logger.w(this.getClass(), "#getVoiceDiarization - Can NOT find the voice diarization: " + fileCode);
            return null;
        }
        voiceDiarization.file = this.getFile(authToken.getDomain(), voiceDiarization.fileCode);
        return voiceDiarization;
    }

    /**
     * 获取联系人的全部说话人分离结果。
     *
     * @param authToken 访问令牌。
     * @return 返回说话人分离结果列表（含文件信息）。
     */
    public List<VoiceDiarization> getVoiceDiarizations(AuthToken authToken) {
        List<VoiceDiarization> result = this.storage.readVoiceDiarizations(authToken.getContactId());
        for (VoiceDiarization voiceDiarization : result) {
            voiceDiarization.file = this.getFile(authToken.getDomain(), voiceDiarization.fileCode);
        }
        return result;
    }

    /**
     * 删除指定文件的说话人分离记录。
     *
     * @param authToken 访问令牌。
     * @param fileCode 文件码。
     * @return 返回被删除的说话人分离结果（含文件信息）；无记录返回 {@code null}。
     */
    public VoiceDiarization deleteVoiceDiarization(AuthToken authToken, String fileCode) {
        VoiceDiarization voiceDiarization = this.storage.readVoiceDiarization(fileCode);
        if (null == voiceDiarization) {
            Logger.w(this.getClass(), "#deleteVoiceDiarization - Can NOT find the voice diarization: " + fileCode);
            return null;
        }
        voiceDiarization.file = this.getFile(authToken.getDomain(), voiceDiarization.fileCode);
        this.storage.deleteVoiceDiarization(fileCode);
        return voiceDiarization;
    }

    /**
     * 语音情绪识别。
     *
     * <p><b>归属：平台能力，非心理学业务</b>。本方法只做「取音频文件 → 发给
     * 音频分类单元 → 解析情绪 → 落情绪记录」。</p>
     *
     * <p>单元选点按 {@link AICapability.AudioProcessing#AudioClassification} 子任务
     * 匹配，与语音识别、说话人分离同一惯例。</p>
     *
     * @param token
     * @param fileCode
     * @param listener
     * @return
     */
    public boolean speechEmotionRecognition(AuthToken token, String fileCode, SpeechEmotionRecognitionListener listener) {
        final FileLabel fileLabel = this.getFile(token.getDomain(), fileCode);
        if (null == fileLabel) {
            Logger.w(this.getClass(), "#speechEmotionRecognition - Can NOT find file: " + fileCode);
            return false;
        }

        this.taskExecutor.execute(new Runnable() {
            @Override
            public void run() {
                AIGCUnit unit = AIGCService.this.selectUnitBySubtask(
                        AICapability.AudioProcessing.AudioClassification);
                if (null == unit) {
                    Logger.w(AIGCService.class, "#speechEmotionRecognition - No unit");
                    listener.onFailed(fileLabel, AIGCStateCode.UnitNoReady);
                    return;
                }

                // 判断文件类型
                if (fileLabel.getFileType() != FileType.WAV &&
                        fileLabel.getFileType() != FileType.MP3 &&
                        fileLabel.getFileType() != FileType.OGG &&
                        fileLabel.getFileType() != FileType.M4A &&
                        fileLabel.getFileType() != FileType.AMR &&
                        fileLabel.getFileType() != FileType.AAC) {
                    Logger.w(AIGCService.class, "#speechEmotionRecognition - No support file: " +
                            fileLabel.getFileType().getPreferredExtension());
                    listener.onFailed(fileLabel, AIGCStateCode.FileError);
                    return;
                }

                JSONObject payload = new JSONObject();
                payload.put("fileLabel", fileLabel.toJSON());
                Packet request = new Packet(AIGCAction.SpeechEmotionRecognition.name, payload);
                ActionDialect dialect = cellet.transmit(unit.getContext(), request.toDialect(), 3 * 60 * 1000);
                if (null == dialect) {
                    Logger.w(AIGCService.class, "#speechEmotionRecognition - Unit error");
                    // 回调错误
                    listener.onFailed(fileLabel, AIGCStateCode.UnitError);
                    return;
                }

                Packet response = new Packet(dialect);
                JSONObject data = Packet.extractDataPayload(response);
                if (!data.has("result")) {
                    Logger.w(AIGCService.class, "#speechEmotionRecognition - Unit process failed");
                    // 回调错误
                    listener.onFailed(fileLabel, AIGCStateCode.Failure);
                    return;
                }

                SpeechEmotion result = new SpeechEmotion(data.getJSONObject("result"));
                // 回调结束
                listener.onCompleted(fileLabel, result);

                // 写入数据库
                EmotionRecord record = new EmotionRecord(token.getContactId(), result.emotion, EmotionRecord.SOURCE_SPEECH);
                record.sourceData = fileLabel.toCompactJSON();
                if (!storage.writeEmotionRecord(record)) {
                    Logger.e(AIGCService.class, "#speechEmotionRecognition - Write emotion record error: " +
                            fileCode);
                }
            }
        });

        return true;
    }

    /**
     * 面部表情识别。
     *
     * <p>支持 JPEG/PNG/BMP 图片，经面部表情单元识别，可要求可视化结果。</p>
     *
     * @param token 访问令牌。
     * @param fileCode 图片文件码。
     * @param visualize 是否输出可视化结果。
     * @param listener 监听器。
     * @return 任务受理返回 {@code true}；文件获取失败返回 {@code false}，
     *         其余失败经监听器回调。
     */
    public boolean facialExpressionRecognition(AuthToken token, String fileCode, boolean visualize,
                                               FacialExpressionRecognitionListener listener) {
        final FileLabel fileLabel = this.getFile(token.getDomain(), fileCode);
        if (null == fileLabel) {
            Logger.w(this.getClass(), "#facialExpressionRecognition - Can NOT find file: " + fileCode);
            return false;
        }

        this.taskExecutor.execute(new Runnable() {
            @Override
            public void run() {
                AIGCUnit unit = selectUnitByName(ModelConfig.FACIAL_EXPRESSION_UNIT);
                if (null == unit) {
                    Logger.w(AIGCService.class, "#facialExpressionRecognition - No unit");
                    listener.onFailed(fileLabel, AIGCStateCode.UnitNoReady);
                    return;
                }

                // 判断文件类型
                if (fileLabel.getFileType() != FileType.JPEG &&
                        fileLabel.getFileType() != FileType.PNG &&
                        fileLabel.getFileType() != FileType.BMP) {
                    Logger.w(AIGCService.class, "#facialExpressionRecognition - No support file: " +
                            fileLabel.getFileType().getPreferredExtension());
                    listener.onFailed(fileLabel, AIGCStateCode.FileError);
                    return;
                }

                JSONObject payload = new JSONObject();
                payload.put("fileLabel", fileLabel.toJSON());
                payload.put("visualize", visualize);
                Packet request = new Packet(AIGCAction.FacialExpressionRecognition.name, payload);
                ActionDialect dialect = cellet.transmit(unit.getContext(), request.toDialect(), 2 * 60 * 1000);
                if (null == dialect) {
                    Logger.w(AIGCService.class, "#facialExpressionRecognition - Unit error");
                    // 回调错误
                    listener.onFailed(fileLabel, AIGCStateCode.UnitError);
                    return;
                }

                Packet response = new Packet(dialect);
                JSONObject data = Packet.extractDataPayload(response);
                if (!data.has("result")) {
                    Logger.w(AIGCService.class, "#facialExpressionRecognition - Unit process failed");
                    // 回调错误
                    listener.onFailed(fileLabel, AIGCStateCode.Failure);
                    return;
                }

                FacialExpressionResult result = new FacialExpressionResult(data.getJSONObject("result"));
                // 回调结束
                listener.onCompleted(fileLabel, result);
            }
        });

        return true;
    }

    /**
     * 获取联系人的情绪记录列表。
     *
     * @param authToken 访问令牌。
     * @return 返回情绪记录列表。
     */
    public List<EmotionRecord> getEmotionRecords(AuthToken authToken) {
        return this.storage.readEmotionRecords(authToken.getContactId());
    }

    /**
     * 获取指定文件的文件标签。
     *
     * @param domain 域名。
     * @param fileCode 文件码。
     * @return 返回文件标签；文件存储服务未就绪或文件不存在返回 {@code null}。
     */
    public FileLabel getFile(String domain, String fileCode) {
        AbstractModule fileStorage = this.getFileStorage();
        if (null == fileStorage) {
            Logger.e(this.getClass(), "#getFile - File storage service is not ready");
            return null;
        }

        GetFile getFile = new GetFile(domain, fileCode);
        JSONObject fileLabelJson = fileStorage.notify(getFile);
        if (null == fileLabelJson) {
            Logger.e(this.getClass(), "#getFile - Get file failed: " + fileCode);
            return null;
        }

        return new FileLabel(fileLabelJson);
    }

    /**
     * 将指定文件加载到本地并返回文件对象。
     *
     * @param domain 域名。
     * @param fileCode 文件码。
     * @return 返回文件；文件存储服务未就绪或加载失败返回 {@code null}。
     */
    public File loadFile(String domain, String fileCode) {
        AbstractModule fileStorage = this.getFileStorage();
        if (null == fileStorage) {
            Logger.e(this.getClass(), "#loadFile - File storage service is not ready");
            return null;
        }

        LoadFile loadFile = new LoadFile(domain, fileCode);
        try {
            String path = fileStorage.notify(loadFile);
            return new File(path);
        } catch (Exception e) {
            Logger.e(this.getClass(), "#loadFile - File storage service load failed", e);
            return null;
        }
    }

    /**
     * 保存文件到文件存储服务。
     *
     * @param authToken 访问令牌。
     * @param fileCode 文件码。
     * @param file 本地文件。
     * @param filename 保存后的文件名，可为 {@code null}。
     * @param deleteAfterSave 保存成功后是否删除本地文件。
     * @return 返回文件标签；文件存储服务未就绪或保存失败返回 {@code null}。
     */
    public FileLabel saveFile(AuthToken authToken, String fileCode, File file, String filename, boolean deleteAfterSave) {
        return this.saveFile(authToken, fileCode, file, filename, deleteAfterSave, null);
    }

    /**
     * 保存文件到文件存储服务（带上下文）。
     *
     * @param authToken 访问令牌。
     * @param fileCode 文件码。
     * @param file 本地文件。
     * @param filename 保存后的文件名，可为 {@code null}。
     * @param deleteAfterSave 保存成功后是否删除本地文件。
     * @param context 附加上下文，写入文件标签，可为 {@code null}。
     * @return 返回文件标签；文件存储服务未就绪或保存失败返回 {@code null}。
     */
    public FileLabel saveFile(AuthToken authToken, String fileCode, File file, String filename, boolean deleteAfterSave,
                              JSONObject context) {
        AbstractModule fileStorage = this.getFileStorage();
        if (null == fileStorage) {
            Logger.e(this.getClass(), "#saveFile - File storage service is not ready");
            return null;
        }

        // 创建文件标签
        FileLabel fileLabel = FileUtils.makeFileLabel(authToken.getDomain(), fileCode, authToken.getContactId(), file);
        if (null != filename) {
            fileLabel.setFileName(filename);
        }
        if (null != context) {
            fileLabel.setContext(context);
        }

        try {
            JSONObject fileJson = fileStorage.notify(new SaveFile(file.getAbsolutePath(), fileLabel));
            return new FileLabel(fileJson);
        } catch (Exception e) {
            Logger.e(this.getClass(), "#saveFile - File storage service save failed", e);
            return null;
        } finally {
            // 删除临时文件
            if (deleteAfterSave && file.exists()) {
                try {
                    file.delete();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 删除指定文件。
     *
     * @param domain 域名。
     * @param fileCode 文件码。
     * @return 返回被删除文件的标签；文件存储服务未就绪、文件不存在或删除失败
     *         返回 {@code null}。
     */
    public FileLabel deleteFile(String domain, String fileCode) {
        AbstractModule fileStorage = this.getFileStorage();
        if (null == fileStorage) {
            Logger.e(this.getClass(), "#deleteFile - File storage service is not ready");
            return null;
        }

        DeleteFile deleteFile = new DeleteFile(domain, fileCode);
        try {
            JSONObject fileLabelJson = fileStorage.notify(deleteFile);
            if (null != fileLabelJson) {
                return new FileLabel(fileLabelJson);
            }
            else {
                Logger.d(this.getClass(), "#deleteFile - No file: " + fileCode + "@" + domain);
                return null;
            }
        } catch (Exception e) {
            Logger.e(this.getClass(), "#deleteFile - Delete file failed", e);
            return null;
        }
    }

    /**
     * 文本分词。
     *
     * @param text 文本。
     * @return 返回分词结果列表。
     */
    public List<String> segmentText(String text) {
        return this.tokenizer.sentenceProcess(text);
    }

    /**
     * 句子相似度。
     *
     * <p>对两个句子分别做 TF-IDF 分词（各取前 10 个词），按词表交集占比
     * 计算相似度。</p>
     *
     * @param sentenceA 句子 A。
     * @param sentenceB 句子 B。
     * @return 返回 0~1 之间的相似度；无有效词时可能返回 NaN。
     */
    public double sentenceSimilarity(String sentenceA, String sentenceB) {
        TFIDFAnalyzer analyzer = new TFIDFAnalyzer(this.tokenizer);
        List<String> wordsA = analyzer.analyzeOnlyWords(sentenceA, 10);
        List<String> wordsB = analyzer.analyzeOnlyWords(sentenceB, 10);
        List<String> pole = null;
        List<String> monkey = null;
        if (wordsA.size() > wordsB.size()) {
            pole = wordsA;
            monkey = wordsB;
        }
        else {
            pole = wordsB;
            monkey = wordsA;
        }
        double count = 0;
        for (String word : pole) {
            if (monkey.contains(word)) {
                count += 1.0;
            }
        }
        return count / pole.size();
    }

    /**
     * 识别上下文数据（URL 资源提取）。
     *
     * <p>内容包含多个 URL 或整体为一个 URL 时，经 URL 内容提取单元读取
     * 并封装为超链接资源；读取失败时资源标记为失败态。普通文本返回空上下文。</p>
     *
     * @param text 输入文本。
     * @param authToken 访问令牌。
     * @return 返回复杂上下文；无对应单元或单元出错时返回空上下文。
     */
    public ComplexContext recognizeContext(String text, AuthToken authToken) {
        final String content = text.trim();
        ComplexContext result = new ComplexContext();

        List<String> urlList = TextUtils.extractAllURLs(content);
        if (!urlList.isEmpty()) {
            // 内容包含 URL 链接
            AIGCUnit unit = this.selectUnitBySubtask(AICapability.DataProcessing.ExtractURLContent);
            if (null == unit) {
                Logger.w(this.getClass(), "#recognizeContent - Can NOT find extract URL content unit");
                return result;
            }

            // 对 URL 进行数据读取
            JSONArray urlArray = new JSONArray();
            for (String url : urlList) {
                urlArray.put(url);
            }
            JSONObject payload = new JSONObject();
            payload.put("urls", urlArray);
            Packet request = new Packet(AIGCAction.ExtractURLContent.name, payload);
            ActionDialect dialect = this.cellet.transmit(unit.getContext(), request.toDialect(), 60 * 1000);
            if (null == dialect) {
                Logger.w(this.getClass(), "#recognizeContent - Unit is error");
                return result;
            }

            Packet response = new Packet(dialect);
            if (Packet.extractCode(response) != AIGCStateCode.Ok.code) {
                Logger.d(this.getClass(), "#recognizeContent - Process url list failed");
                result = new ComplexContext(false);
                for (String url : urlList) {
                    HyperlinkResource resource = new HyperlinkResource(url, HyperlinkResource.TYPE_FAILURE);
                    resource.fixContent();
                    result.addResource(resource);
                }
            }
            else {
                JSONObject data = Packet.extractDataPayload(response);
                JSONArray list = data.getJSONArray("list");
                result = new ComplexContext(false);
                for (int i = 0; i < list.length(); ++i) {
                    JSONObject resPayload = new JSONObject();
                    resPayload.put("payload", list.getJSONObject(i));
                    HyperlinkResource resource = new HyperlinkResource(resPayload);
                    resource.fixContent();
                    result.addResource(resource);
                }
            }
        }
        else if (TextUtils.isURL(content)) {
            AIGCUnit unit = this.selectUnitBySubtask(AICapability.DataProcessing.ExtractURLContent);
            if (null == unit) {
                Logger.w(this.getClass(), "#recognizeContent - Can NOT find extract URL content unit");
                return result;
            }

            // 对 URL 进行数据读取
            JSONObject payload = new JSONObject();
            payload.put("url", content);
            Packet request = new Packet(AIGCAction.ExtractURLContent.name, payload);
            ActionDialect dialect = this.cellet.transmit(unit.getContext(), request.toDialect(), 60 * 1000);
            if (null == dialect) {
                Logger.w(this.getClass(), "#recognizeContent - Unit is error");
                return result;
            }

            Packet response = new Packet(dialect);
            if (Packet.extractCode(response) != AIGCStateCode.Ok.code) {
                HyperlinkResource resource = new HyperlinkResource(content, HyperlinkResource.TYPE_FAILURE);
                resource.fixContent();
                result = new ComplexContext(false);
                result.addResource(resource);
            }
            else {
                JSONObject data = Packet.extractDataPayload(response);
                JSONArray list = data.getJSONArray("list");
                if (list.isEmpty()) {
                    // 列表没有数据，获取 URL 失败
                    result = new ComplexContext(false);
                    HyperlinkResource resource = new HyperlinkResource(content, HyperlinkResource.TYPE_FAILURE);
                    resource.fixContent();
                    result.addResource(resource);
                }
                else {
                    result = new ComplexContext(false);
                    JSONObject resPayload = new JSONObject();
                    resPayload.put("payload", list.getJSONObject(0));
                    HyperlinkResource resource = new HyperlinkResource(resPayload);
                    resource.fixContent();
                    result.addResource(resource);
                }
            }
        }

        return result;
    }

    /**
     * 按访问令牌获取频道。中继（Relay）模式下允许按需创建频道。
     *
     * @param authToken 访问令牌。
     * @return 返回频道，未找到返回 {@code null}。
     */
    private AIGCChannel getChannel(AuthToken authToken) {
        return this.channelManager.get(authToken, this.useRelay);
    }

    /**
     * 从外部链接下载文件到文件存储服务。
     *
     * @param authToken 访问令牌。
     * @param fileUrl 外部文件 URL。
     * @return 返回文件标签；文件存储服务未就绪或下载失败返回 {@code null}。
     */
    public FileLabel downloadFile(AuthToken authToken, String fileUrl) {
        // 从外部链接下载
        AbstractModule fileStorage = this.getFileStorage();
        if (null == fileStorage) {
            Logger.w(this.getClass(), "#downloadFile - File storage service is not ready");
            return null;
        }

        DownloadFile notice = new DownloadFile(fileUrl, authToken.getDomain(), authToken.getContactId(),
                50 * 1024 * 1024);
        JSONObject fileLabelJson = fileStorage.notify(notice);
        if (null == fileLabelJson) {
            Logger.w(this.getClass(), "#downloadFile - Download failed: " + fileUrl);
            return null;
        }
        return new FileLabel(fileLabelJson);
    }

    /**
     * 增加指定单元的实时运行计数。
     *
     * <p>调用方必须在 <code>finally</code> 中递减返回的计数器。</p>
     *
     * @param unitName 单元能力名称。
     * @return 返回该单元名对应的计数器。
     */
    public AtomicInteger increaseUnitCounter(String unitName) {
        return this.taskExecutor.beginUnitTask(unitName);
    }
}
