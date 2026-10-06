/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.psychology;

import cell.util.log.Logger;
import cube.aigc.psychology.*;
import cube.aigc.psychology.algorithm.Attention;
import cube.aigc.psychology.app.ConsultationSchedule;
import cube.aigc.psychology.app.Customer;
import cube.aigc.psychology.composition.*;
import cube.aigc.psychology.listener.PaintingReportListener;
import cube.aigc.psychology.listener.ScaleReportListener;
import cube.aigc.spi.*;
import cube.auth.AuthToken;
import cube.common.Language;
import cube.common.entity.AIGCChannel;
import cube.common.entity.FileLabel;
import cube.common.state.AIGCStateCode;
import cube.service.psychology.action.*;
import cube.service.psychology.scene.CopilotManager;
import cube.service.psychology.scene.CounselingManager;
import cube.service.psychology.scene.PsychologyScene;
import cube.service.psychology.scene.PsychologySpeechListener;
import cube.service.psychology.scene.VoiceStreamService;
import cube.storage.StorageType;
import cube.util.ConfigUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 心理学业务模块。
 *
 * <p><b>提供的动作</b>：</p>
 * <ul>
 *   <li>绘画标签与报告状态（{@code setPaintingReportState}、
 *       {@code getPaintingLabel}、{@code setPaintingLabel}）——只读写
 *       {@link PsychologyStorage} 的标签与状态方法；</li>
 *   <li>8 个：客户与日程 CRUD（{@code appQueryCustomer}、
 *       {@code appNewCustomer}、{@code appUpdateCustomer}、
 *       {@code appDeleteCustomer}、{@code appQuerySchedule}、
 *       {@code appNewSchedule}、{@code appUpdateSchedule}、
 *       {@code appDeleteSchedule}）；</li>
 *   <li>4 个：量表族（{@code listPsychologyScales}、
 *       {@code getPsychologyScale}、{@code generatePsychologyScale}、
 *       {@code submitPsychologyAnswerSheet}）；</li>
 *   <li>7 个：报告链路（{@code getPsychologyReport}、
 *       {@code stopGeneratingPsychologyReport}、{@code resetReportAttention}、
 *       {@code getPsychologyReportPart}、{@code modifyReportRemark}、
 *       {@code getPsychologyPainting}、{@code checkPsychologyPainting}）。
 *       其中报告读取与控制<b>不自建</b>，而是经 {@link AIGCHost} 的
 *       「报告运行态」能力访问宿主——原因见 {@link #queryPaintingReport}。</li>
 * </ul>
 *
 * <p>前 11 个动作<b>只读写本地存储</b>，因此不需要分词器与 TF-IDF 语料；
 * 的 {@code submitPsychologyAnswerSheet} 需要关键词抽取，
 * 但它经 {@link AIGCHost#extractKeywords(String, int)} 委托宿主完成，
 * <b>本模块仍不持有</b>分词器或语料。</p>
 *
 * <p><b>线程约束</b>：本类由宿主在 {@code AIGCCellet#install()} 阶段同步实例化
 * 并调用 {@link #setup(AIGCHost)}。此刻内核尚未启动，
 * {@code AbstractCellet} 执行器与宿主服务级线程池<b>都尚未创建</b>，
 * 因此 {@code setup} 内<b>严禁</b>调用 {@link AIGCHost#schedule}：
 * 该调用取不到执行器，会「记一条 WARN 后把任务丢弃」，既无异常也无失败标记。
 * 建表在 {@code setup} 内一次性完成（{@code storage.open()} 之后、
 * 场景装配之前），不推迟到首次动作派发。</p>
 *
 * <p><b>表结构单一来源</b>：16 张 {@code psychology_} 前缀表的定义随
 * {@link PsychologyStorage} 整体迁入本模块，插件与宿主共用同一份 DDL，
 * 不存在两套表定义漂移的可能。</p>
 *
 * <p><b>令牌校验的分组差异</b>：客户与日程 CRUD 8 个动作为 {@code true}
 * （校验令牌有效性），其余动作为 {@code false}——它们对无效令牌返回
 * {@code IllegalOperation} 或 {@code NoToken}，若交由宿主骨架前置校验
 * 会改成 {@code InconsistentToken}，属应答码变更。
 * 详见 {@link #getActions()} 的注释与各处理器 javadoc。</p>
 *
 * <p><b>包名约束</b>：本类必须与 {@link PsychologyStorage} 同包。若把它放进
 * 子包，动作处理器就需要 {@code import cube.service.psychology.PsychologyStorage;}，
 * 而这本身就是一条宿主模块的 import，违反「插件零 service 依赖」的约束。</p>
 */
public final class PsychologyModule implements ActionModule {

    /**
     * 模块名。全局唯一，同时作为存储命名空间与资源目录前缀。
     */
    public final static String NAME = "psychology";

    /**
     * 心理学 REST 主前缀。
     *
     * <p>与 dispatcher 侧现存的端点路径逐字一致（如
     * {@code /aigc/psychology/scales}）。该前缀下的handler 仍由 dispatcher
     * 侧注册，插件只做声明——插件编译期不含 {@code cube-dispatcher-*.jar}，
     * 无法引用 {@code Manager} 与 Jetty。</p>
     */
    public final static String REST_PREFIX = "/aigc/psychology";

    /**
     * 模块版本。仅用于日志与诊断。
     */
    public final static String VERSION = "1.0.0";

    /**
     * 心理学数据库配置文件名（本地覆盖，优先于模板）。
     *
     * <p>该文件含数据库凭据，<b>不受版本控制</b>，由部署环境提供。
     * 它存在即生效，不存在时退回 {@link #CONFIG_FILE_TEMPLATE}。</p>
     */
    private final static String CONFIG_FILE_LOCAL = "psychology.local.json";

    /**
     * 心理学数据库配置文件名（模板，受版本控制）。
     *
     * <p>只含占位符与默认值，<b>直接使用它即表示配置未按环境定制</b>——
     * 见 {@link #setup(AIGCHost)} 中对占位符的检测。</p>
     */
    private final static String CONFIG_FILE_TEMPLATE = "psychology.json.template";

    /**
     * 模板中存储主机与库名的占位符。
     *
     * <p>命中即视为「读到的是未定制的模板」，按配置缺失处理：
     * 让装载失败并记ERROR，而不是让服务在启动后才因连不上库而异常。</p>
     */
    private final static String PLACEHOLDER = "<your-";

    /**
     * 报告生成队列上限的缺省值（配置未给出 {@code preference.maxQueueLength} 时）。
     */
    private final static int DEFAULT_MAX_QUEUE_LENGTH = 20;

    /**
     * 动作线协议名。取自既有枚举的 {@code name} 字段，逐字符相同，<b>不带模块前缀</b>。
     */
    private final static String ACTION_SET_REPORT_STATE = "setPaintingReportState";

    private final static String ACTION_GET_LABEL = "getPaintingLabel";

    private final static String ACTION_SET_LABEL = "setPaintingLabel";

    private final static String ACTION_QUERY_CUSTOMER = "appQueryCustomer";

    private final static String ACTION_NEW_CUSTOMER = "appNewCustomer";

    private final static String ACTION_UPDATE_CUSTOMER = "appUpdateCustomer";

    private final static String ACTION_DELETE_CUSTOMER = "appDeleteCustomer";

    private final static String ACTION_QUERY_SCHEDULE = "appQuerySchedule";

    private final static String ACTION_NEW_SCHEDULE = "appNewSchedule";

    private final static String ACTION_UPDATE_SCHEDULE = "appUpdateSchedule";

    private final static String ACTION_DELETE_SCHEDULE = "appDeleteSchedule";

    private final static String ACTION_LIST_SCALES = "listPsychologyScales";

    private final static String ACTION_GET_SCALE = "getPsychologyScale";

    private final static String ACTION_GENERATE_SCALE = "generatePsychologyScale";

    private final static String ACTION_SUBMIT_ANSWER_SHEET = "submitPsychologyAnswerSheet";

    private final static String ACTION_STOP_REPORT = "stopGeneratingPsychologyReport";

    private final static String ACTION_GENERATE_REPORT = "generatePsychologyReport";

    private final static String ACTION_GET_REPORT = "getPsychologyReport";

    private final static String ACTION_RESET_ATTENTION = "resetReportAttention";

    private final static String ACTION_REPORT_PART = "getPsychologyReportPart";

    private final static String ACTION_MODIFY_REMARK = "modifyReportRemark";

    private final static String ACTION_PAINTING = "getPsychologyPainting";

    private final static String ACTION_CHECK_PAINTING = "checkPsychologyPainting";

    private final static String ACTION_ANALYSE_VOICE_STREAM = "analyseVoiceStream";

    private final static String ACTION_STOP_VOICE_STREAM = "stopVoiceStream";

    private final static String ACTION_GET_VOICE_STREAM_FILE = "getVoiceStreamFile";

    private final static String ACTION_SPEECH_ANALYSIS = "speechAnalysis";

    private final static String ACTION_GET_TEMPLATE_ARTICLE = "getPsychologyTemplateArticle";

    private final static String ACTION_GENERATE_TEMPLATE_ARTICLE = "generatePsychologyTemplateArticle";

    private final static String ACTION_GET_COMPREHENSIVE = "queryPsychologyComprehensive";

    private final static String ACTION_GENERATE_COMPREHENSIVE = "generatePsychologyComprehensive";

    private final static String ACTION_CONVERSATION = "psychologyConversation";

    private final static String ACTION_APPLY_COPILOT = "applyCopilot";

    private final static String ACTION_DISPOSE_COPILOT = "disposeCopilot";

    private final static String ACTION_SUBMIT_COPILOT_SHEET = "submitCopilotSheet";

    private final static String ACTION_QUERY_COUNSELING_STRATEGY = "queryCounselingStrategy";

    private final static String ACTION_QUERY_COUNSELING_CAPTION = "queryCounselingCaption";

    /**
     * 心理学私有存储器。
     *
     * <p>本模块自建实例而<b>不</b>使用 {@link AIGCHost#openModuleStorage}：
     * 后者返回的是裸 {@code cube.core.Storage}，而本模块需要
     * {@link PsychologyStorage} 提供的领域方法。</p>
     */
    private PsychologyStorage storage;

    /**
     * 实际生效的配置文件名，仅用于日志。
     */
    private String configFileName = "(not loaded)";

    /**
     * 静态声明本模块的动作名。
     *
     * <p>宿主在清单解析出类名之后、实例化之前调用本方法，
     * 因此它必须是静态的且不依赖任何实例状态——这样即使插件 jar 缺失、
     * 模块类加载失败，宿主仍能知道「这些动作属于一个未就绪的模块」，
     * 从而回 {@code AIGCStateCode#ModuleNotLoaded} 而非让请求悬挂。</p>
     *
     * <p>清单里的动作名必须与 {@link #getActions()} 逐字一致；
     * 两者不一致时以 {@code getActions()} 为准（它同时用于绑定），
     * 本方法仅用于「类加载前」的兜底声明。</p>
     *
     * @return 返回本模块提供的全部动作名。
     */
    public static List<String> declareActionNames() {
        return Arrays.asList(
                ACTION_SET_REPORT_STATE,
                ACTION_GET_LABEL,
                ACTION_SET_LABEL,
                ACTION_QUERY_CUSTOMER,
                ACTION_NEW_CUSTOMER,
                ACTION_UPDATE_CUSTOMER,
                ACTION_DELETE_CUSTOMER,
                ACTION_QUERY_SCHEDULE,
                ACTION_NEW_SCHEDULE,
                ACTION_UPDATE_SCHEDULE,
                ACTION_DELETE_SCHEDULE,
                ACTION_LIST_SCALES,
                ACTION_GET_SCALE,
                ACTION_GENERATE_SCALE,
                ACTION_SUBMIT_ANSWER_SHEET,
                ACTION_STOP_REPORT,
                ACTION_GENERATE_REPORT,
                ACTION_GET_REPORT,
                ACTION_RESET_ATTENTION,
                ACTION_REPORT_PART,
                ACTION_MODIFY_REMARK,
                ACTION_PAINTING,
                ACTION_CHECK_PAINTING,
                ACTION_ANALYSE_VOICE_STREAM,
                ACTION_STOP_VOICE_STREAM,
                ACTION_GET_VOICE_STREAM_FILE,
                ACTION_SPEECH_ANALYSIS,
                ACTION_GET_TEMPLATE_ARTICLE,
                ACTION_GENERATE_TEMPLATE_ARTICLE,
                ACTION_GET_COMPREHENSIVE,
                ACTION_GENERATE_COMPREHENSIVE,
                ACTION_CONVERSATION,
                ACTION_APPLY_COPILOT,
                ACTION_DISPOSE_COPILOT,
                ACTION_SUBMIT_COPILOT_SHEET,
                ACTION_QUERY_COUNSELING_STRATEGY,
                ACTION_QUERY_COUNSELING_CAPTION
        );
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public ModuleDescriptor getDescriptor() {
        return new ModuleDescriptor(NAME, VERSION, AIGCSPI.VERSION,
                // 动作命名空间：不参与线协议，仅供冲突检测与日志
                Collections.singletonList(NAME),
                // REST 前缀：与 dispatcher 侧现存的 17 条心理学端点一致。
                // 插件【不】注册 handler（其编译期不含 cube-dispatcher-*.jar），
                // 本声明供 dispatcher 侧做前缀冲突检测与兜底通道的模块定位。
                Arrays.asList(REST_PREFIX, "/aigc/painting", "/aigc/stream"),
                // 上述动作不调用任何模型单元
                Collections.emptyList(),
                // 不依赖兄弟模块
                Collections.emptyList(),
                // setup 内含建表：读配置 + new + open + 逐表建表 + 场景装配。
                // 建表逐表先判断存在再创建（幂等），稳态下只是存在性查询很快；
                // 首次部署要执行十几张表的建表，MySQL 下可能耗时数秒，
                // 故超时给到 30 秒。
                30000L,
                // 非可选：装载失败将使宿主记为「未就绪」
                false);
    }

    @Override
    public void setup(AIGCHost host) throws ModuleException {
        // 严禁在此调用 host.schedule()：install() 阶段宿主线程池尚未创建
        Logger.i(this.getClass(), "\n----------------------------------------" +
                "\n** Module: " + PsychologyModule.NAME +
                "\n** Version: " + PsychologyModule.VERSION +
                "\n----------------------------------------");
        try {
            JSONObject config = this.readConfig();

            JSONObject storageConfig = config.getJSONObject("storage");
            if (null == storageConfig) {
                throw new ModuleException("Config file \"" + this.configFileName
                        + "\" has NO \"storage\" section");
            }

            StorageType type = storageConfig.getString("type").equalsIgnoreCase("SQLite")
                    ? StorageType.SQLite
                    : StorageType.MySQL;

            this.storage = new PsychologyStorage(type, storageConfig);

            // 六维得分描述的生成依赖宿主分词器与 TF-IDF 语料，二者在宿主服务侧，
            // 插件无法直接引用；故以函数式接口注入。
            // ⚠️ 必须在 storage.open() 之前注入：首次回读报告即可能触发描述生成
            this.injectDescriber(host);

            this.storage.open();

            // 建表：在此一次性完成，不做惰性推迟。
            // ⚠️ 必须排在 storage.open() 之后（需要连接），且必须排在
            //    setupScene() 之前 —— 场景装配会加载语料与量表，
            //    而后者可能回读存储，表不存在会直接失败。
            this.storage.execSelfChecking(null);

            // 把存储与宿主能力交给场景，并读入队列上限。
            // 场景是本模块内部的使用者（同一 jar 内引用，不违反零宿主依赖约束），
            // 它持有内存态的报告表与生成队列，插件动作经 SPI 回调它。
            // ⚠️ 场景装配必须排在建表之后：它内部会加载语料与量表，
            //    而后者可能回读存储。
            this.setupScene(host, config);

            // 咨询与陪练两个管理器：注入宿主能力并启动。
            // ⚠️ 排在场景之后：两者都经 SPI 回调宿主，而宿主的线程池在
            //    install() 阶段尚未创建；放在此处可确保它们初始化时
            //    场景与存储均已就绪。
            CounselingManager.getInstance().start(host);
            CopilotManager.getInstance().start(host);

            // 语音流服务：承载分析/停止/归档查询三个入站动作的编排
            VoiceStreamService.getInstance().setup(host, this.storage);

            // 语音监听器：订阅宿主的「说话人分离完成」事件，把说话人映射为
            // 「来访者 / 咨询师」。该角色分类原硬编码在宿主 AudioUnitMeta 里，
            // 属咨询业务收尾操作，故下沉到本模块经监听器完成。
            // ⚠️ 排在最后：此前任何一步失败都会走 rollbackSetup，
            //    注销逻辑与 teardown 共用。
            PsychologySpeechListener.getInstance().setup(host);

            // 只记录配置来源与存储类型，不输出配置内容（该配置文件含数据库凭据）
            Logger.i(this.getClass(), "#setup - Psychology storage opened, type: " + type
                    + ", config: " + this.configFileName);
        } catch (ModuleException e) {
            this.rollbackSetup();
            throw e;
        } catch (Throwable t) {
            // 捕获 Throwable 而非 Exception：模块由第三方维护，
            // 加载期出现的 Error（如类缺失）同样不应让宿主启动失败
            this.rollbackSetup();
            throw new ModuleException("#setup - Open psychology storage failed", t);
        }
    }

    /**
     * 回滚 {@link #setup(AIGCHost)} 中途失败留下的资源。
     *
     * <p>装载失败后宿主会跳过该模块，但已启动的组件仍持有宿主引用与线程，
     * 不清理会留下「装载失败却仍在跑」的孤儿组件。</p>
     *
     * <p>不外抛：回滚自身失败不应掩盖原始的装载异常。</p>
     */
    private void rollbackSetup() {
        try {
            PsychologySpeechListener.getInstance().teardown();
        } catch (Throwable t) {
            Logger.e(this.getClass(), "#rollbackSetup - Speech listener teardown FAILED",
                    (t instanceof Exception) ? (Exception) t : null);
        }

        try {
            CopilotManager.getInstance().stop();
        } catch (Throwable t) {
            Logger.e(this.getClass(), "#rollbackSetup - Copilot manager stop FAILED",
                    (t instanceof Exception) ? (Exception) t : null);
        }

        try {
            CounselingManager.getInstance().stop();
        } catch (Throwable t) {
            Logger.e(this.getClass(), "#rollbackSetup - Counseling manager stop FAILED",
                    (t instanceof Exception) ? (Exception) t : null);
        }

        try {
            VoiceStreamService.getInstance().teardown();
        } catch (Throwable t) {
            Logger.e(this.getClass(), "#rollbackSetup - Voice stream teardown FAILED",
                    (t instanceof Exception) ? (Exception) t : null);
        }

        if (null != this.storage) {
            try {
                this.storage.close();
            } catch (Throwable t) {
                Logger.e(this.getClass(), "#rollbackSetup - Storage close FAILED",
                        (t instanceof Exception) ? (Exception) t : null);
            }
            this.storage = null;
        }
    }

    /**
     * 装配业务场景。
     *
     * <p>场景持有报告内存表、生成队列与各工作器，是本模块报告链路的运行态容器。
     * 存储与宿主能力由本模块创建后注入，场景自身不读配置、不建存储。</p>
     *
     * <p>队列上限取 {@code preference.maxQueueLength}，缺省 20；
     * 配置为 20 时即生效为 20，不使用代码默认值 30。</p>
     *
     * @param host 宿主能力接口。
     * @param config 已生效的模块配置。
     */
    private void setupScene(AIGCHost host, JSONObject config) {
        int maxQueueLength = DEFAULT_MAX_QUEUE_LENGTH;
        JSONObject preference = config.optJSONObject("preference");
        if (null != preference) {
            maxQueueLength = preference.optInt("maxQueueLength", DEFAULT_MAX_QUEUE_LENGTH);
        }

        PsychologyScene.getInstance().setup(host, this.storage, maxQueueLength);
    }

    /**
     * 读取实际生效的数据库配置。
     *
     * <p>查找顺序：{@code psychology.local.json}（本地实际配置，不受版本控制）
     * → {@code psychology.json.template}（模板，受版本控制）。两者都经
     * {@link ConfigUtils#readJsonFile(String)} 定位，会依次尝试工作目录下的同名文件
     * 与 {@code config/} 下的同名文件。</p>
     *
     * <p>命中模板且其中仍为占位符时按<b>配置缺失</b>处理并抛
     * {@link ModuleException}：这比让它「装载成功、连库时才失败」更早暴露问题，
     * 且错误信息直接指出应当创建哪个文件。</p>
     *
     * @return 返回生效配置。
     * @throws ModuleException 配置不存在或仍是未定制的模板时抛出。
     */
    private JSONObject readConfig() throws ModuleException {
        JSONObject local = ConfigUtils.readJsonFile(CONFIG_FILE_LOCAL);
        if (null != local) {
            this.configFileName = CONFIG_FILE_LOCAL;
            return local;
        }

        JSONObject template = ConfigUtils.readJsonFile(CONFIG_FILE_TEMPLATE);
        if (null == template) {
            throw new ModuleException("Config file \"" + CONFIG_FILE_LOCAL
                    + "\" is NOT found, nor is the template \"" + CONFIG_FILE_TEMPLATE
                    + "\"; copy the template to \"" + CONFIG_FILE_LOCAL
                    + "\" and fill in the database settings");
        }

        JSONObject storage = template.optJSONObject("storage");
        String host = (null == storage) ? null : storage.optString("host", "");
        if (null != host && host.startsWith(PLACEHOLDER)) {
            throw new ModuleException("Config file \"" + CONFIG_FILE_TEMPLATE
                    + "\" is NOT customized (storage.host=\"" + host
                    + "\"); copy it to \"" + CONFIG_FILE_LOCAL + "\" and fill in the real values");
        }

        this.configFileName = CONFIG_FILE_TEMPLATE;

        return template;
    }

    /**
     * 取得本模块持有的存储。
     *
     * <p>供冒烟测试验证「场景与模块持有同一个实例」——这是存储唯一化的核心判据。
     * 生产代码不应使用，调用方请直接使用本类已有的领域方法。</p>
     *
     * @return 返回存储；未装配时返回 <code>null</code>。
     */
    public PsychologyStorage getStorageForTest() {
        return this.storage;
    }

    /**
     * 为存储层注入六维描述生成器。
     *
     * <p>未注入时 {@link PsychologyStorage} 会记 WARN 并跳过描述，结果等同于
     * 「描述为空」，因此注入失败不阻断装载。</p>
     *
     * @param host 宿主能力接口。
     */
    private void injectDescriber(AIGCHost host) {
        if (null == host) {
            Logger.w(this.getClass(), "#injectDescriber - Host is NULL, skip description generator");
            return;
        }

        this.storage.setHexagonDescriber((dimensionScore, language) ->
                host.fillHexagonScoreDescription(dimensionScore, language));
    }

    @Override
    public void teardown() {
        // 停机顺序：语音监听器 → 咨询/陪练管理器 → 场景 → 存储。
        // 先注销监听器：宿主的说话人分离事件不再进入本模块，
        // 后续管理器停止期间不会再有新的事件触发角色映射。
        try {
            PsychologySpeechListener.getInstance().teardown();
        } catch (Throwable t) {
            Logger.e(this.getClass(), "#teardown - Speech listener teardown FAILED",
                    (t instanceof Exception) ? (Exception) t : null);
        }

        // 咨询/陪练管理器持有内存态的流与陪练会话，且经 SPI 回调宿主；
        // 若先关存储，它们在停止过程中回读存储会失败。
        try {
            CounselingManager.getInstance().stop();
        } catch (Throwable t) {
            Logger.e(this.getClass(), "#teardown - Counseling manager stop FAILED",
                    (t instanceof Exception) ? (Exception) t : null);
        }

        try {
            CopilotManager.getInstance().stop();
        } catch (Throwable t) {
            Logger.e(this.getClass(), "#teardown - Copilot manager stop FAILED",
                    (t instanceof Exception) ? (Exception) t : null);
        }

        // 语音流服务持有待处理分片表，其中文件需经宿主删除，故排在存储关闭前
        try {
            VoiceStreamService.getInstance().teardown();
        } catch (Throwable t) {
            Logger.e(this.getClass(), "#teardown - Voice stream service teardown FAILED",
                    (t instanceof Exception) ? (Exception) t : null);
        }

        // 再停场景：场景会清空对存储的引用，反序会造成
        // 「场景仍可能被访问而存储已关闭」的窗口
        try {
            PsychologyScene.getInstance().teardown();
        } catch (Throwable t) {
            // 不外抛：停机路径不应因清理异常而中断，存储仍需关闭
            Logger.e(this.getClass(), "#teardown - Scene teardown FAILED",
                    (t instanceof Exception) ? (Exception) t : null);
        }

        if (null != this.storage) {
            this.storage.close();
            this.storage = null;
        }

        this.configFileName = "(not loaded)";

        Logger.i(this.getClass(), "#teardown - Psychology module is stopped");
    }

    @Override
    public void onTick(long now) {
        // 心跳只在存储就绪时有意义：场景的维护动作会回读存储
        if (null == this.storage) {
            return;
        }

        try {
            // 咨询流的心跳：超时关流与过期数据清理，排在场景之前——
            // 它处理的是尚未成为报告的原始音频，与场景无耦合
            CounselingManager.getInstance().onTick(now);
        } catch (Throwable t) {
            Logger.e(this.getClass(), "#onTick - Counseling manager tick FAILED",
                    (t instanceof Exception) ? (Exception) t : null);
        }

        try {
            // 语音流心跳：清理超期未完成的分片（分离在途但迟迟无回调）
            VoiceStreamService.getInstance().onTick(now);
        } catch (Throwable t) {
            Logger.e(this.getClass(), "#onTick - Voice stream service tick FAILED",
                    (t instanceof Exception) ? (Exception) t : null);
        }

        try {
            PsychologyScene.getInstance().onTick(now);
        } catch (Throwable t) {
            // 与 ModuleRegistry#tick 的约定一致：单个模块的心跳异常不外抛
            Logger.e(this.getClass(), "#onTick - Scene tick FAILED",
                    (t instanceof Exception) ? (Exception) t : null);
        }
    }

    @Override
    public List<ActionBinding> getActions() {
        // requiresToken 决定宿主是否在派发前校验令牌有效性，须逐个动作对齐：
        //
        // · true（仅客户与日程 CRUD 8 个）：由骨架前置校验令牌，
        //   无效即回 InvalidParameter + InconsistentToken，处理器内无需重复校验。
        // · false（其余全部）：这些动作对无效令牌有各自约定的应答码
        //   （NoToken 或 IllegalOperation），若交由骨架前置校验会被改写成
        //   InconsistentToken —— 属线协议可见变更，故保留由处理器自判。
        //   逐个动作的具体依据见各处理器 javadoc。
        return Arrays.asList(
                // ── 绘画标签与报告状态 ──
                new ActionBinding(ACTION_SET_REPORT_STATE, new SetPaintingReportStateAction(),
                        false, new AIGCStateCode[] {
                                AIGCStateCode.NoToken, AIGCStateCode.InvalidParameter,
                                AIGCStateCode.Failure, AIGCStateCode.Ok}),
                new ActionBinding(ACTION_GET_LABEL, new GetPaintingLabelAction(),
                        false, new AIGCStateCode[] {
                                AIGCStateCode.NoToken, AIGCStateCode.InvalidParameter,
                                AIGCStateCode.Failure, AIGCStateCode.Ok}),
                new ActionBinding(ACTION_SET_LABEL, new SetPaintingLabelAction(),
                        false, new AIGCStateCode[] {
                                AIGCStateCode.NoToken, AIGCStateCode.InvalidParameter,
                                AIGCStateCode.Failure, AIGCStateCode.Ok}),

                // ── 客户 CRUD（未注册用户回空列表，故白名单无 IllegalOperation）──
                new ActionBinding(ACTION_QUERY_CUSTOMER, new AppQueryCustomerAction(),
                        true, new AIGCStateCode[] {
                                AIGCStateCode.InvalidParameter, AIGCStateCode.InconsistentToken,
                                AIGCStateCode.Ok}),
                new ActionBinding(ACTION_NEW_CUSTOMER, new AppNewCustomerAction(),
                        true, new AIGCStateCode[] {
                                AIGCStateCode.InvalidParameter, AIGCStateCode.InconsistentToken,
                                AIGCStateCode.IllegalOperation, AIGCStateCode.Failure,
                                AIGCStateCode.Ok}),
                new ActionBinding(ACTION_UPDATE_CUSTOMER, new AppUpdateCustomerAction(),
                        true, new AIGCStateCode[] {
                                AIGCStateCode.InvalidParameter, AIGCStateCode.InconsistentToken,
                                AIGCStateCode.IllegalOperation, AIGCStateCode.NoData,
                                AIGCStateCode.Failure, AIGCStateCode.Ok}),
                new ActionBinding(ACTION_DELETE_CUSTOMER, new AppDeleteCustomerAction(),
                        true, new AIGCStateCode[] {
                                AIGCStateCode.InvalidParameter, AIGCStateCode.InconsistentToken,
                                AIGCStateCode.IllegalOperation, AIGCStateCode.NoData,
                                AIGCStateCode.Failure, AIGCStateCode.Ok}),

                // ── 日程 CRUD ──
                new ActionBinding(ACTION_QUERY_SCHEDULE, new AppQueryScheduleAction(),
                        true, new AIGCStateCode[] {
                                AIGCStateCode.InvalidParameter, AIGCStateCode.InconsistentToken,
                                AIGCStateCode.Ok}),
                new ActionBinding(ACTION_NEW_SCHEDULE, new AppNewScheduleAction(),
                        true, new AIGCStateCode[] {
                                AIGCStateCode.InvalidParameter, AIGCStateCode.InconsistentToken,
                                AIGCStateCode.IllegalOperation, AIGCStateCode.Failure,
                                AIGCStateCode.Ok}),
                new ActionBinding(ACTION_UPDATE_SCHEDULE, new AppUpdateScheduleAction(),
                        true, new AIGCStateCode[] {
                                AIGCStateCode.InvalidParameter, AIGCStateCode.InconsistentToken,
                                AIGCStateCode.IllegalOperation, AIGCStateCode.NoData,
                                AIGCStateCode.Failure, AIGCStateCode.Ok}),
                new ActionBinding(ACTION_DELETE_SCHEDULE, new AppDeleteScheduleAction(),
                        true, new AIGCStateCode[] {
                                AIGCStateCode.InvalidParameter, AIGCStateCode.InconsistentToken,
                                AIGCStateCode.IllegalOperation, AIGCStateCode.NoData,
                                AIGCStateCode.Failure, AIGCStateCode.Ok}),

                // ── 量表族 ──
                // requiresToken 为 false：这些动作令牌无效时回的是
                // IllegalOperation（而非骨架在 true 时给出的 InconsistentToken），
                // 交由骨架前置校验会改变线协议可见的应答码，故处理器内自行判定。
                new ActionBinding(ACTION_LIST_SCALES, new ListPsychologyScalesAction(),
                        false, new AIGCStateCode[] {
                                AIGCStateCode.NoToken, AIGCStateCode.IllegalOperation,
                                AIGCStateCode.Failure, AIGCStateCode.Ok}),
                new ActionBinding(ACTION_GET_SCALE, new GetPsychologyScaleAction(),
                        false, new AIGCStateCode[] {
                                AIGCStateCode.NoToken, AIGCStateCode.IllegalOperation,
                                AIGCStateCode.Failure, AIGCStateCode.InvalidParameter,
                                AIGCStateCode.Ok}),
                new ActionBinding(ACTION_GENERATE_SCALE, new GeneratePsychologyScaleAction(),
                        false, new AIGCStateCode[] {
                                AIGCStateCode.NoToken, AIGCStateCode.IllegalOperation,
                                AIGCStateCode.Failure, AIGCStateCode.InvalidParameter,
                                AIGCStateCode.Ok}),
                new ActionBinding(ACTION_SUBMIT_ANSWER_SHEET, new SubmitPsychologyAnswerSheetAction(),
                        false, new AIGCStateCode[] {
                                AIGCStateCode.NoToken, AIGCStateCode.IllegalOperation,
                                AIGCStateCode.Failure, AIGCStateCode.InvalidParameter,
                                AIGCStateCode.Ok}),

                // ── 报告读取与控制 ──
                // 三者均为 false：stopGenerating 的令牌无效回 IllegalOperation，
                // 而 resetReportAttention 根本不校验令牌有效性；
                // 交由骨架前置校验会同时改变这两处的线协议可见行为
                new ActionBinding(ACTION_GENERATE_REPORT, new GeneratePsychologyReportAction(),
                        false, new AIGCStateCode[] {
                                AIGCStateCode.NoToken, AIGCStateCode.InvalidParameter,
                                AIGCStateCode.Failure, AIGCStateCode.Ok}),
                new ActionBinding(ACTION_STOP_REPORT, new StopGeneratingReportAction(),
                        false, new AIGCStateCode[] {
                                AIGCStateCode.NoToken, AIGCStateCode.IllegalOperation,
                                AIGCStateCode.InvalidParameter, AIGCStateCode.Failure,
                                AIGCStateCode.Ok}),
                new ActionBinding(ACTION_GET_REPORT, new GetPsychologyReportAction(),
                        false, new AIGCStateCode[] {
                                AIGCStateCode.NoToken, AIGCStateCode.IllegalOperation,
                                AIGCStateCode.InvalidParameter, AIGCStateCode.Failure,
                                AIGCStateCode.Ok}),
                new ActionBinding(ACTION_RESET_ATTENTION, new ResetReportAttentionAction(),
                        false, new AIGCStateCode[] {
                                AIGCStateCode.NoToken, AIGCStateCode.InvalidParameter,
                                AIGCStateCode.Failure, AIGCStateCode.IllegalOperation,
                                AIGCStateCode.Ok}),

                // ── 报告内容与备注 ──
                // 均为 false：无效令牌回 IllegalOperation
                new ActionBinding(ACTION_REPORT_PART, new GetPsychologyReportPartAction(),
                        false, new AIGCStateCode[] {
                                AIGCStateCode.NoToken, AIGCStateCode.IllegalOperation,
                                AIGCStateCode.Failure, AIGCStateCode.InvalidParameter,
                                AIGCStateCode.Ok}),
                new ActionBinding(ACTION_MODIFY_REMARK, new ModifyReportRemarkAction(),
                        false, new AIGCStateCode[] {
                                AIGCStateCode.NoToken, AIGCStateCode.IllegalOperation,
                                AIGCStateCode.Failure, AIGCStateCode.InvalidParameter,
                                AIGCStateCode.Ok}),

                // ── 绘画读取与校验 ──
                new ActionBinding(ACTION_PAINTING, new GetPsychologyPaintingAction(),
                        false, new AIGCStateCode[] {
                                AIGCStateCode.NoToken, AIGCStateCode.IllegalOperation,
                                AIGCStateCode.Failure, AIGCStateCode.InvalidParameter,
                                AIGCStateCode.Ok}),
                // ⚠️ 本动作第二码也是 NoToken，且永不回 Failure，勿「顺手统一」
                new ActionBinding(ACTION_CHECK_PAINTING, new CheckPsychologyPaintingAction(),
                        false, new AIGCStateCode[] {
                                AIGCStateCode.NoToken, AIGCStateCode.InvalidParameter,
                                AIGCStateCode.Ok}),

                // ── 语音流（录制咨询音频 → 说话人分离 → 归档）──
                // 四者均为 false：宿主原任务类只校验「方言里有没有 token 参数」，
                // 未校验有效性；交由骨架前置校验会把无效令牌改写成
                // InconsistentToken，属线协议可见变更
                new ActionBinding(ACTION_ANALYSE_VOICE_STREAM, new AnalyseVoiceStreamAction(),
                        false, new AIGCStateCode[] {
                                AIGCStateCode.InvalidParameter, AIGCStateCode.Failure,
                                AIGCStateCode.Ok}),
                new ActionBinding(ACTION_STOP_VOICE_STREAM, new StopVoiceStreamAction(),
                        false, new AIGCStateCode[] {
                                AIGCStateCode.InvalidParameter, AIGCStateCode.Failure,
                                AIGCStateCode.Ok}),
                new ActionBinding(ACTION_GET_VOICE_STREAM_FILE, new GetVoiceStreamFileAction(),
                        false, new AIGCStateCode[] {
                                AIGCStateCode.InvalidParameter, AIGCStateCode.Failure,
                                AIGCStateCode.Ok}),
                new ActionBinding(ACTION_SPEECH_ANALYSIS, new SpeechAnalysisAction(),
                        false, new AIGCStateCode[] {
                                AIGCStateCode.InvalidParameter, AIGCStateCode.Failure,
                                AIGCStateCode.Ok}),

                // ── 模板文章 ──
                // requiresToken 均为 false：宿主原任务类自行判令牌且各自动作
                // 的应答码不同（无 token 与令牌无效分别回 NoToken /
                // InvalidParameter），交由骨架前置校验会改写为 InconsistentToken
                new ActionBinding(ACTION_GET_TEMPLATE_ARTICLE, new GetPsychologyTemplateArticleAction(),
                        false, new AIGCStateCode[] {
                                AIGCStateCode.NoToken, AIGCStateCode.NoData,
                                AIGCStateCode.IllegalOperation, AIGCStateCode.Ok}),
                new ActionBinding(ACTION_GENERATE_TEMPLATE_ARTICLE,
                        new GeneratePsychologyTemplateArticleAction(),
                        false, new AIGCStateCode[] {
                                AIGCStateCode.NoToken, AIGCStateCode.InvalidParameter,
                                AIGCStateCode.NotFound, AIGCStateCode.Failure,
                                AIGCStateCode.IllegalOperation, AIGCStateCode.Ok}),

                // ── 心理融合评测 ──
                new ActionBinding(ACTION_GET_COMPREHENSIVE, new GetPsychologyComprehensiveAction(),
                        false, new AIGCStateCode[] {
                                AIGCStateCode.NoToken, AIGCStateCode.IllegalOperation,
                                AIGCStateCode.NoData, AIGCStateCode.InvalidParameter,
                                AIGCStateCode.Ok}),
                new ActionBinding(ACTION_GENERATE_COMPREHENSIVE,
                        new GeneratePsychologyComprehensiveAction(),
                        false, new AIGCStateCode[] {
                                AIGCStateCode.NoToken, AIGCStateCode.InvalidParameter,
                                AIGCStateCode.Failure, AIGCStateCode.IllegalOperation,
                                AIGCStateCode.Ok}),

                // ── 心理学对话（流式）──
                // ⚠️ 唯一声明 streaming=true 的动作：一次请求可能 speak 两次
                //    （先回受理，再回流式内容），故既不能去重也不能补空应答
                new ActionBinding(ACTION_CONVERSATION, new PsychologyConversationAction(),
                        false, new AIGCStateCode[] {
                                AIGCStateCode.NoToken, AIGCStateCode.InvalidParameter,
                                AIGCStateCode.Failure, AIGCStateCode.Ok}, true),

                // ── 陪练 ──
                // 三个动作为 false：宿主原任务类对「无 token 参数」与
                // 「令牌解析不出」都回 NoToken，交由骨架前置校验会把后者
                // 改写为 InconsistentToken，属线协议可见变更
                new ActionBinding(ACTION_APPLY_COPILOT, new ApplyCopilotAction(),
                        false, new AIGCStateCode[] {
                                AIGCStateCode.NoToken, AIGCStateCode.Failure,
                                AIGCStateCode.InvalidParameter, AIGCStateCode.Ok}),
                new ActionBinding(ACTION_DISPOSE_COPILOT, new DisposeCopilotAction(),
                        false, new AIGCStateCode[] {
                                AIGCStateCode.NoToken, AIGCStateCode.Failure,
                                AIGCStateCode.InvalidParameter, AIGCStateCode.Ok}),
                new ActionBinding(ACTION_SUBMIT_COPILOT_SHEET, new SubmitCopilotSheetAction(),
                        false, new AIGCStateCode[] {
                                AIGCStateCode.NoToken, AIGCStateCode.Failure,
                                AIGCStateCode.InvalidParameter, AIGCStateCode.Ok}),

                // ── 咨询策略与字幕 ──
                // 为 false：宿主原任务类把「无 token 参数」与参数缺失
                // 合并判为 InvalidParameter（不是 NoToken 也不是 InconsistentToken）
                new ActionBinding(ACTION_QUERY_COUNSELING_STRATEGY,
                        new QueryCounselingStrategyAction(),
                        false, new AIGCStateCode[] {
                                AIGCStateCode.InvalidParameter, AIGCStateCode.NoData,
                                AIGCStateCode.Failure, AIGCStateCode.Ok}),
                new ActionBinding(ACTION_QUERY_COUNSELING_CAPTION,
                        new QueryCounselingCaptionAction(),
                        false, new AIGCStateCode[] {
                                AIGCStateCode.InvalidParameter, AIGCStateCode.NoData,
                                AIGCStateCode.Failure, AIGCStateCode.Ok}));
    }

    /**
     * 读取绘画标签。
     *
     * @param sn 报告序列号。
     * @return 返回标签列表，无记录时返回空列表。
     */
    public List<PaintingLabel> readPaintingLabels(long sn) {
        return this.storage.readPaintingLabels(sn);
    }

    /**
     * 覆写绘画标签（先清后写）。
     *
     * <p><b>注意该操作不是原子的</b>：删除成功而插入失败时，该报告的标签会全部丢失。
     * 这是既有语义，调用方需知悉此风险。</p>
     *
     * @param sn 报告序列号。
     * @param labels 待写入的标签列表，可为空。
     * @return 写入成功返回 <code>true</code>。
     */
    public boolean writePaintingLabels(long sn, List<PaintingLabel> labels) {
        this.storage.deletePaintingLabel(sn);

        if (labels.isEmpty()) {
            return true;
        }

        return this.storage.writePaintingLabels(labels);
    }

    /**
     * 写入报告管理状态。
     *
     * @param sn 报告序列号。
     * @param state 目标状态。
     * @return 写入成功返回 <code>true</code>。
     */
    public boolean writePaintingReportState(long sn, int state) {
        return this.storage.writePaintingManagementState(sn, state);
    }

    // ───────── 客户与日程 ─────────
    //
    // 联系人 ID 由处理器从 ctx.getToken().getContactId() 取得后传入，
    // 本模块不做任何令牌解析——那是宿主的职责。

    /**
     * 统计联系人下的客户数。
     *
     * @param cid 联系人 ID。
     * @return 返回未删除的客户数量。
     */
    public int countCustomers(long cid) {
        return this.storage.countCustomers(cid);
    }

    /**
     * 读取联系人的全部未删除客户。
     *
     * @param cid 联系人 ID。
     * @return 返回客户列表，无记录时返回空列表。
     */
    public List<Customer> readCustomers(long cid) {
        return this.storage.readCustomers(cid);
    }

    /**
     * 读取单个客户。
     *
     * @param cid 联系人 ID。
     * @param id 客户 ID。
     * @return 返回客户；不存在时返回 <code>null</code>。
     */
    public Customer readCustomer(long cid, long id) {
        return this.storage.readCustomer(cid, id);
    }

    /**
     * 写入客户（存在则更新，不存在则插入）。
     *
     * <p><b>⚠️ 入参 {@code customer.gender} 不可为 null</b>：存储层会取
     * {@code customer.gender.name}。经 JSON 构造器创建时该字段恒非空
     * （{@code Gender.parse} 对无法识别的值回退 {@code Gender.Unknown}），
     * 此处保留提示以防将来绕过 JSON 构造器。</p>
     *
     * @param cid 联系人 ID。
     * @param customer 待写入的客户。
     * @return 写入成功返回 <code>true</code>。
     */
    public boolean writeCustomer(long cid, Customer customer) {
        return this.storage.writeCustomer(cid, customer);
    }

    /**
     * 统计时间窗内的日程数。
     *
     * @param cid 联系人 ID。
     * @param starting 起始时间戳（含）。
     * @param ending 结束时间戳（含）。
     * @return 返回未删除且落在时间窗内的日程数量。
     */
    public int countSchedules(long cid, long starting, long ending) {
        return this.storage.countSchedules(cid, starting, ending);
    }

    /**
     * 读取时间窗内的日程。
     *
     * @param cid 联系人 ID。
     * @param starting 起始时间戳（含）。
     * @param ending 结束时间戳（含）。
     * @return 返回日程列表，无记录时返回空列表。
     */
    public List<ConsultationSchedule> readSchedules(long cid, long starting, long ending) {
        return this.storage.readSchedules(cid, starting, ending);
    }

    /**
     * 读取单个日程。
     *
     * @param cid 联系人 ID。
     * @param id 日程 ID。
     * @return 返回日程；不存在时返回 <code>null</code>。
     */
    public ConsultationSchedule readSchedule(long cid, long id) {
        return this.storage.readSchedule(cid, id);
    }

    /**
     * 写入日程（存在则更新，不存在则插入）。
     *
     * @param cid 联系人 ID。
     * @param schedule 待写入的日程。
     * @return 写入成功返回 <code>true</code>。
     */
    public boolean writeSchedule(long cid, ConsultationSchedule schedule) {
        return this.storage.writeSchedule(cid, schedule);
    }

    // ───────── 量表族 ─────────

    /**
     * 解析令牌对应的联系人 ID。
     *
     * <p>解析失败返回 0，让处理器得以用单一条件判空。</p>
     *
     * <p><b>宿主能力从上下文取而非从模块字段取</b>：模块在 {@code setup(host)}
     * 期间保存的那份引用是<b>装载期</b>注入的，若这里用它解析请求令牌，
     * 则同一模块的所有请求会共用装载期的那一份宿主引用，
     * 无法按请求区分令牌有效性。此处传入 {@code ctx.getHost()}
     * 以保证判定与本次请求一致。</p>
     *
     * @param host 本次请求的宿主能力。
     * @param tokenCode 令牌码。
     * @return 返回联系人 ID；令牌无效时返回 <code>0</code>。
     */
    public long resolveContactId(AIGCHost host, String tokenCode) {
        if (null == tokenCode || null == host) {
            return 0;
        }

        AuthToken authToken = host.resolveToken(tokenCode);
        return (null == authToken) ? 0 : authToken.getContactId();
    }

    /**
     * 列出全部已开放的量表。
     *
     * @param contactId 联系人 ID。
     * @return 返回已开放的量表列表。
     */
    public List<Scale> listScales(long contactId) {
        return ListPsychologyScalesAction.listScales(contactId);
    }

    /**
     * 读取单个量表。
     *
     * @param sn 量表序列号。
     * @return 返回量表；不存在时返回 <code>null</code>。
     */
    public Scale getScale(long sn) {
        return this.storage.readScale(sn);
    }

    /**
     * 生成量表并落库。
     *
     * @param contactId 联系人 ID。
     * @param scaleName 量表名。
     * @param attribute 受测者属性。
     * @return 返回量表；量表定义不存在时返回 <code>null</code>。
     */
    public Scale generateScale(long contactId, String scaleName, Attribute attribute) {
        Scale scale = Resource.getInstance().loadScaleByName(scaleName, contactId);
        if (null == scale) {
            return null;
        }

        scale.setAttribute(attribute);

        this.storage.writeScale(scale);

        return scale;
    }

    /**
     * 提交答题卡：评级、评分并落库。
     *
     * <p>未答完时直接返回当前结果；答完后先为主观题推断答案，
     * 再执行评分脚本。任何异常都归为「评分失败」，返回 {@code null}。</p>
     *
     * @param ctx 动作上下文，用于访问模型单元与关键词抽取。
     * @param answerSheet 答题卡。
     * @return 返回评分结果；量表不存在或评分失败时返回 <code>null</code>。
     */
    public ScaleResult submitAnswerSheet(cube.aigc.spi.ActionContext ctx, AnswerSheet answerSheet) {
        Scale scale = this.getScale(answerSheet.scaleSn);
        if (null == scale) {
            return null;
        }

        scale.submitAnswer(answerSheet);

        this.storage.writeScale(scale);
        this.storage.writeAnswerSheet(answerSheet);

        if (!scale.isComplete()) {
            // 未答完：直接返回当前进度，不触发推断与评分
            return new ScaleResult(scale);
        }

        try {
            // 为主观题推断答案
            SubmitPsychologyAnswerSheetAction.inferScaleAnswers(ctx, scale);

            ScaleResult scaleResult = scale.scoring(Resource.getInstance().getQuestionnairesPath());
            if (null != scaleResult) {
                this.storage.writeScale(scale);
            }

            return scaleResult;
        } catch (Exception e) {
            Logger.e(this.getClass(), "#submitAnswerSheet - Score failed", e);
            return null;
        }
    }

    // ───────── 报告读取与控制 ─────────
    //
    // 这组能力全部落在本模块内：报告的运行态内存表与生成队列由
    // PsychologyScene 持有，而场景与本模块同在一个 jar 内，
    // 因此直接调用即可，无需经宿主能力接口中转。
    //
    // 【为何不再走 SPI】此前这组方法挂在 AIGCHost 上，实现体是
    // 「宿主转发给插件场景」，而调用方是插件自己的 action ——
    // 构成「插件 → SPI → 宿主 → 插件」的环路。环路不带来依赖方向上的
    // 收益，却要求宿主编译期持有插件 jar，是反向依赖的根源。
    // 删除后宿主不再引用任何插件类型，build.xml 得以移除插件 jar。

    /**
     * 查询绘画报告。
     *
     * @param sn 报告序列号。
     * @param format 导出格式：{@code compact} / {@code markdown} / {@code sections}。
     * @return 返回报告 JSON；不存在或导出失败时返回 <code>null</code>。
     */
    public JSONObject queryPaintingReport(long sn, String format) {
        PsychologyScene scene = this.getSceneOrNull();
        if (null == scene) {
            return null;
        }

        PaintingReport report = scene.getPaintingReport(sn);
        if (null == report) {
            return null;
        }

        try {
            JSONObject json;
            if ("markdown".equalsIgnoreCase(format)) {
                json = report.exportMarkdown();
            }
            else if ("sections".equalsIgnoreCase(format)) {
                json = report.exportReportSectionJSON();
            }
            else {
                json = report.toCompactJSON();
                // 所在队列位置：仅摘要形态需要，与迁移前一致
                json.put("queuePosition", scene.getGeneratingQueuePosition(sn));
            }

            return json;
        } catch (Exception e) {
            Logger.e(this.getClass(), "#queryPaintingReport - Export failed, sn: " + sn, e);
            return null;
        }
    }

    /**
     * 查询绘画报告实体。
     *
     * @param sn 报告序列号。
     * @return 返回报告实体；不存在时返回 <code>null</code>。
     */
    public PaintingReport getPaintingReport(long sn) {
        PsychologyScene scene = this.getSceneOrNull();
        return (null == scene) ? null : scene.getPaintingReport(sn);
    }

    /**
     * 查询量表报告。
     *
     * @param sn 报告序列号。
     * @return 返回报告 JSON；不存在时返回 <code>null</code>。
     */
    public JSONObject queryScaleReport(long sn) {
        PsychologyScene scene = this.getSceneOrNull();
        if (null == scene) {
            return null;
        }

        ScaleReport report = scene.getScaleReport(sn);
        return (null == report) ? null : report.toJSON();
    }

    /**
     * 分页查询绘画报告。
     *
     * @param contactId 联系人 ID。
     * @param page 页码，从 0 开始。
     * @param size 每页条数。
     * @param descending 是否倒序。
     * @param state 报告状态；{@code -1} 表示不限。
     * @return 返回含 {@code total} 与 {@code list} 的对象。
     */
    public JSONObject listPaintingReports(long contactId, int page, int size,
            boolean descending, int state) {
        PsychologyScene scene = this.getSceneOrNull();
        if (null == scene) {
            return null;
        }

        int num = (state == -1) ? scene.numPaintingReports(contactId)
                : scene.numPaintingReports(contactId, state);

        List<PaintingReport> list = (state == -1)
                ? scene.getPaintingReports(contactId, page, size, descending)
                : scene.getPaintingReportsWithState(contactId, page, size, descending, state);

        JSONArray array = new JSONArray();
        for (PaintingReport report : list) {
            array.put(report.toCompactJSON());
        }

        JSONObject data = new JSONObject();
        data.put("total", num);
        data.put("page", page);
        data.put("size", size);
        data.put("list", array);

        return data;
    }

    /**
     * 查询量表报告列表。
     *
     * @param contactId 联系人 ID。
     * @param descending 是否倒序。
     * @param state 报告状态；{@code -1} 表示不限。
     * @return 返回含 {@code total} 与 {@code list} 的对象。
     */
    public JSONObject listScaleReports(long contactId, boolean descending, int state) {
        PsychologyScene scene = this.getSceneOrNull();
        if (null == scene) {
            return null;
        }

        int num = (state == -1) ? scene.numScaleReports(contactId)
                : scene.numScaleReports(contactId, state);

        List<ScaleReport> list = (state == -1)
                ? scene.getScaleReports(contactId, descending)
                : scene.getScaleReports(contactId, state, descending);

        JSONArray array = new JSONArray();
        for (ScaleReport report : list) {
            array.put(report.toCompactJSON());
        }

        JSONObject data = new JSONObject();
        data.put("total", num);
        data.put("list", array);

        return data;
    }

    /**
     * 停止报告生成。
     *
     * @param sn 报告序列号。
     * @return 返回被停止的报告 JSON；未生效时返回 <code>null</code>。
     */
    public JSONObject stopReportGeneration(long sn) {
        PsychologyScene scene = this.getSceneOrNull();
        if (null == scene) {
            return null;
        }

        PaintingReport report = scene.stopGenerating(sn);
        return (null == report) ? null : report.toCompactJSON();
    }

    /**
     * 重置报告关注等级。
     *
     * @param sn 报告序列号。
     * @param newAttention 目标关注等级；<code>null</code> 表示回滚到滚动建议。
     * @return 返回重置后的报告 JSON；报告不存在或更新失败时返回 <code>null</code>。
     */
    public JSONObject resetReportAttention(long sn, Integer newAttention) {
        PsychologyScene scene = this.getSceneOrNull();
        if (null == scene) {
            return null;
        }

        Attention attention = (null == newAttention) ? null : Attention.parse(newAttention);
        PaintingReport report = scene.resetReportAttention(sn, attention);
        return (null == report) ? null : report.toCompactJSON();
    }

    /**
     * 查询绘画特征集。
     *
     * @param reportSn 报告序列号。
     * @return 返回特征集；不存在时返回 <code>null</code>。
     */
    public PaintingFeatureSet getPaintingFeatureSet(long reportSn) {
        PsychologyScene scene = this.getSceneOrNull();
        return (null == scene) ? null : scene.getPaintingFeatureSet(reportSn);
    }

    /**
     * 读取绘画推理数据。
     *
     * @param sn 报告序列号。
     * @return 返回推理数据；报告不存在或尚未完成推理时返回 <code>null</code>。
     */
    public JSONObject getPaintingInferenceData(long sn) {
        PsychologyScene scene = this.getSceneOrNull();
        if (null == scene) {
            return null;
        }

        // 绘画推理数据不依赖令牌，故不传
        return scene.getPaintingInferenceData(null, sn);
    }

    /**
     * 预测绘画要素。
     *
     * @param token 访问令牌。
     * @param fileCode 文件码。
     * @return 返回绘画要素；预测失败时返回 <code>null</code>。
     */
    public Painting getPredictedPainting(AuthToken token, String fileCode) {
        PsychologyScene scene = this.getSceneOrNull();
        if (null == scene || null == token || null == fileCode) {
            return null;
        }

        return scene.getPredictedPainting(token, fileCode);
    }

    /**
     * 预测绘画要素并输出带标注的图像。
     *
     * @param token 访问令牌。
     * @param sn 报告序列号。
     * @param boundingBox 是否输出外框。
     * @param visualParam 是否输出视觉参数。
     * @param probability 置信度阈值。
     * @return 返回新生成图像的标签；预测失败时返回 <code>null</code>。
     */
    public FileLabel getPredictedPainting(AuthToken token, long sn, boolean boundingBox,
            boolean visualParam, double probability) {
        PsychologyScene scene = this.getSceneOrNull();
        if (null == scene || null == token) {
            return null;
        }

        return scene.getPredictedPainting(token, sn, boundingBox, visualParam, probability);
    }

    /**
     * 生成绘画报告并入队。
     *
     * @param channel 会话频道。
     * @param attribute 受测人属性。
     * @param fileLabel 绘画文件。
     * @param theme 分析主题。
     * @param maxIndicators 最多输出的指标数。
     * @param adjust 是否校正。
     * @param retention 留存天数。
     * @param remark 备注。
     * @param listener 完成回调。
     * @return 返回已入队的报告；入队失败时返回 <code>null</code>。
     */
    public PaintingReport generatePaintingReport(AIGCChannel channel, Attribute attribute,
            FileLabel fileLabel, Theme theme, int maxIndicators, boolean adjust, int retention,
            String remark, PaintingReportListener listener) {
        PsychologyScene scene = this.getSceneOrNull();
        if (null == scene) {
            return null;
        }
        if (null == channel || null == attribute || null == fileLabel) {
            Logger.w(this.getClass(), "#generatePaintingReport - Channel, attribute or file is NULL");
            return null;
        }

        return scene.generatePaintingReport(channel, attribute, fileLabel, theme, maxIndicators,
                adjust, retention, remark, listener);
    }

    /**
     * 生成量表测验报告并入队。
     *
     * @param channel 会话频道。
     * @param scale 量表。
     * @param language 会话语言。
     * @param listener 完成回调。
     * @return 返回已入队的报告；入队失败时返回 <code>null</code>。
     */
    public ScaleReport generateScaleReport(AIGCChannel channel, Scale scale, Language language,
            ScaleReportListener listener) {
        PsychologyScene scene = this.getSceneOrNull();
        if (null == scene) {
            return null;
        }
        if (null == channel || null == scale) {
            Logger.w(this.getClass(), "#generateScaleReport - Channel or scale is NULL");
            return null;
        }

        return scene.generateScaleReport(channel, scale, language, listener);
    }

    /**
     * 取得已装配的场景。
     *
     * <p>场景由本模块的 {@link #setup(AIGCHost)} 装配，早于任何一次动作派发；
     * 但若模块因配置缺失等原因未装载成功，场景会保持未装配状态。
     * 此处返回 <code>null</code> 让调用方回明确失败，
     * 而不是让空引用异常被动作执行层 catch 成无信息的 {@code Failure}。</p>
     *
     * @return 返回已装配的场景；未装配时返回 <code>null</code>。
     */
    private PsychologyScene getSceneOrNull() {
        PsychologyScene scene = PsychologyScene.getInstance();
        if (!scene.isReady()) {
            Logger.e(this.getClass(), "#getSceneOrNull - Psychology scene is NOT ready, "
                    + "the business module is probably NOT loaded");
            return null;
        }

        return scene;
    }

    // ───────── 报告内容与备注 ─────────

    /**
     * 修改报告备注。
     *
     * <p>逻辑完全在本模块内：更新备注 → 回读报告 → 把备注附加到回读结果上。
     * 两步都已由本模块的 {@link PsychologyStorage} 提供，无需经 SPI。</p>
     *
     * <p>⚠️ 回读报告会触发六维描述生成，而描述器在 {@link #setup} 时注入；
     * 若注入失败，描述为空（与降级结果一致）。</p>
     *
     * @param reportSn 报告序列号。
     * @param remark 新备注。
     * @return 返回更新后的报告；更新失败或报告不存在时返回 <code>null</code>。
     */
    public PaintingReport modifyReportRemark(long reportSn, String remark) {
        if (!this.storage.updatePsychologyReportRemark(reportSn, remark)) {
            return null;
        }

        PaintingReport report = this.storage.readPsychologyReport(reportSn);
        if (null == report) {
            return null;
        }

        report.setRemark(remark);
        return report;
    }
}
