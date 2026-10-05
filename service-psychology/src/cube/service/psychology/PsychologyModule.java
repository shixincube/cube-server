/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.psychology;

import cell.util.log.Logger;
import cube.aigc.psychology.Attribute;
import cube.aigc.psychology.PaintingReport;
import cube.aigc.psychology.Resource;
import cube.aigc.psychology.app.ConsultationSchedule;
import cube.aigc.psychology.app.Customer;
import cube.aigc.psychology.composition.AnswerSheet;
import cube.aigc.psychology.composition.PaintingLabel;
import cube.aigc.psychology.composition.Scale;
import cube.aigc.psychology.composition.ScaleResult;
import cube.aigc.spi.AIGCHost;
import cube.aigc.spi.AIGCSPI;
import cube.aigc.spi.ActionBinding;
import cube.aigc.spi.ActionContext;
import cube.aigc.spi.ActionModule;
import cube.aigc.spi.ModuleDescriptor;
import cube.aigc.spi.ModuleException;
import cube.auth.AuthToken;
import cube.service.psychology.scene.PsychologyScene;
import cube.common.state.AIGCStateCode;
import cube.storage.StorageType;
import cube.util.ConfigUtils;
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
 * 建表因此改为首次动作派发时惰性执行，见 {@link #ensureSelfChecked()}。</p>
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
     * 建表是否已完成。首次动作派发时惰性执行，见 {@link #ensureSelfChecked()}。
     */
    private volatile boolean selfChecked = false;

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
                // setup 内只做「读配置 + new + open」，不含建表，实际耗时在百毫秒级
                30000L,
                // 非可选：装载失败将使宿主记为「未就绪」
                false);
    }

    @Override
    public void setup(AIGCHost host) throws ModuleException {
        // 严禁在此调用 host.schedule()：install() 阶段宿主线程池尚未创建
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

            // 把存储与宿主能力交给场景，并读入队列上限。
            // 场景是本模块内部的使用者（同一 jar 内引用，不违反零宿主依赖约束），
            // 它持有内存态的报告表与生成队列，插件动作经 SPI 回调它。
            // ⚠️ 场景装配必须排在 storage.open() 之后：它内部会加载语料与量表，
            //    而后者可能回读存储。
            this.setupScene(host, config);

            // 只记录配置来源与存储类型，不输出配置内容（该配置文件含数据库凭据）
            Logger.i(this.getClass(), "#setup - Psychology storage opened, type: " + type
                    + ", config: " + this.configFileName);
        } catch (ModuleException e) {
            throw e;
        } catch (Throwable t) {
            // 捕获 Throwable 而非 Exception：模块由第三方维护，
            // 加载期出现的 Error（如类缺失）同样不应让宿主启动失败
            throw new ModuleException("#setup - Open psychology storage failed", t);
        }
    }

    /**
     * 装配业务场景。
     *
     * <p>场景持有报告内存表、生成队列与各工作器，是本模块报告链路的运行态容器。
     * 存储与宿主能力由本模块创建后注入，场景自身不读配置、不建存储。</p>
     *
     * <p>队列上限取 {@code preference.maxQueueLength}，缺省 20。旧实现里
     * 这一项由场景在启动时读取，而那条启动路径已经不存在，导致配置写20
     * 实际用的是代码默认 30。</p>
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
        // 先停场景再关存储：场景会清空对存储的引用，反序会造成
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

        this.selfChecked = false;
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
                                AIGCStateCode.Ok}));
    }

    /**
     * 惰性建表。
     *
     * <p>{@code execSelfChecking()} 在表已存在时只是逐表存在性查询，耗时很短；
     * 但首次部署时要执行 16 次建表与若干改表，MySQL 下可能耗时数秒。
     * 由于 {@code setup()} 运行在单元安装阶段（该阶段阻塞会顺延内核启动），
     * 因此把建表推迟到首次动作派发。宿主场景启动时仍会执行同一份建表逻辑，
     * 两者都是幂等的（逐表先判断存在再创建），重复调用无害。</p>
     */
    private void ensureSelfChecked() {
        if (this.selfChecked) {
            return;
        }

        synchronized (this) {
            if (this.selfChecked) {
                return;
            }

            this.storage.execSelfChecking(null);
            this.selfChecked = true;
        }
    }

    /**
     * 读取绘画标签。
     *
     * @param sn 报告序列号。
     * @return 返回标签列表，无记录时返回空列表。
     */
    List<PaintingLabel> readPaintingLabels(long sn) {
        this.ensureSelfChecked();

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
    boolean writePaintingLabels(long sn, List<PaintingLabel> labels) {
        this.ensureSelfChecked();

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
    boolean writePaintingReportState(long sn, int state) {
        this.ensureSelfChecked();

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
    int countCustomers(long cid) {
        this.ensureSelfChecked();

        return this.storage.countCustomers(cid);
    }

    /**
     * 读取联系人的全部未删除客户。
     *
     * @param cid 联系人 ID。
     * @return 返回客户列表，无记录时返回空列表。
     */
    List<Customer> readCustomers(long cid) {
        this.ensureSelfChecked();

        return this.storage.readCustomers(cid);
    }

    /**
     * 读取单个客户。
     *
     * @param cid 联系人 ID。
     * @param id 客户 ID。
     * @return 返回客户；不存在时返回 <code>null</code>。
     */
    Customer readCustomer(long cid, long id) {
        this.ensureSelfChecked();

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
    boolean writeCustomer(long cid, Customer customer) {
        this.ensureSelfChecked();

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
    int countSchedules(long cid, long starting, long ending) {
        this.ensureSelfChecked();

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
    List<ConsultationSchedule> readSchedules(long cid, long starting, long ending) {
        this.ensureSelfChecked();

        return this.storage.readSchedules(cid, starting, ending);
    }

    /**
     * 读取单个日程。
     *
     * @param cid 联系人 ID。
     * @param id 日程 ID。
     * @return 返回日程；不存在时返回 <code>null</code>。
     */
    ConsultationSchedule readSchedule(long cid, long id) {
        this.ensureSelfChecked();

        return this.storage.readSchedule(cid, id);
    }

    /**
     * 写入日程（存在则更新，不存在则插入）。
     *
     * @param cid 联系人 ID。
     * @param schedule 待写入的日程。
     * @return 写入成功返回 <code>true</code>。
     */
    boolean writeSchedule(long cid, ConsultationSchedule schedule) {
        this.ensureSelfChecked();

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
    long resolveContactId(AIGCHost host, String tokenCode) {
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
    List<Scale> listScales(long contactId) {
        return ListPsychologyScalesAction.listScales(contactId);
    }

    /**
     * 读取单个量表。
     *
     * @param sn 量表序列号。
     * @return 返回量表；不存在时返回 <code>null</code>。
     */
    Scale getScale(long sn) {
        this.ensureSelfChecked();

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
    Scale generateScale(long contactId, String scaleName, Attribute attribute) {
        Scale scale = Resource.getInstance().loadScaleByName(scaleName, contactId);
        if (null == scale) {
            return null;
        }

        scale.setAttribute(attribute);

        this.ensureSelfChecked();
        this.storage.writeScale(scale);

        return scale;
    }

    /**
     * 提交答题卡：评级、评分并落库。
     *
     * <p>逐字复刻宿主 {@code PsychologyScene#submitAnswerSheet}：未答完时
     * 直接返回当前结果；答完后先为主观题推断答案，再执行评分脚本。
     * 任何异常都归为「评分失败」，返回 {@code null}。</p>
     *
     * @param ctx 动作上下文，用于访问模型单元与关键词抽取。
     * @param answerSheet 答题卡。
     * @return 返回评分结果；量表不存在或评分失败时返回 <code>null</code>。
     */
    ScaleResult submitAnswerSheet(cube.aigc.spi.ActionContext ctx, AnswerSheet answerSheet) {
        Scale scale = this.getScale(answerSheet.scaleSn);
        if (null == scale) {
            return null;
        }

        scale.submitAnswer(answerSheet);

        this.ensureSelfChecked();
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

    /**
     * 查询绘画报告。
     *
     * <p><b>为何不自建</b>：报告在生成过程中先落在宿主的内存表里，
     * 此刻尚未入库；若本模块只查 {@link PsychologyStorage}，
     * 调用方查「正在生成中」的报告会得到「不存在」，
     * 而经由宿主接口能查到（并附带队列位置）。这是线协议可见的行为回归。
     * 故整条读取经宿主完成，宿主持有内存表与队列的归属。</p>
     *
     * @param ctx 动作上下文。
     * @param sn 报告序列号。
     * @param format 导出格式：{@code compact} / {@code markdown} / {@code sections}。
     * @return 返回报告 JSON；不存在或导出失败时返回 <code>null</code>。
     */
    JSONObject queryPaintingReport(AIGCHost host, long sn, String format) {
        return host.queryPaintingReport(sn, format);
    }

    /**
     * 查询量表报告。
     *
     * @param ctx 动作上下文。
     * @param sn 报告序列号。
     * @return 返回报告 JSON；不存在时返回 <code>null</code>。
     */
    JSONObject queryScaleReport(AIGCHost host, long sn) {
        return host.queryScaleReport(sn);
    }

    /**
     * 分页查询绘画报告。
     *
     * @param ctx 动作上下文。
     * @param contactId 联系人 ID。
     * @param page 页码，从 0 开始。
     * @param size 每页条数。
     * @param descending 是否倒序。
     * @param state 报告状态；{@code -1} 表示不限。
     * @return 返回含 {@code total} 与 {@code list} 的对象。
     */
    JSONObject listPaintingReports(AIGCHost host, long contactId, int page, int size,
            boolean descending, int state) {
        return host.listPaintingReports(contactId, page, size, descending, state);
    }

    /**
     * 查询量表报告列表。
     *
     * @param ctx 动作上下文。
     * @param contactId 联系人 ID。
     * @param descending 是否倒序。
     * @param state 报告状态；{@code -1} 表示不限。
     * @return 返回含 {@code total} 与 {@code list} 的对象。
     */
    JSONObject listScaleReports(AIGCHost host, long contactId, boolean descending, int state) {
        return host.listScaleReports(contactId, descending, state);
    }

    /**
     * 停止报告生成。
     *
     * @param ctx 动作上下文。
     * @param sn 报告序列号。
     * @return 返回被停止的报告 JSON；未生效时返回 <code>null</code>。
     */
    JSONObject stopReportGeneration(AIGCHost host, long sn) {
        return host.stopReportGeneration(sn);
    }

    /**
     * 重置报告关注等级。
     *
     * @param ctx 动作上下文。
     * @param sn 报告序列号。
     * @param attention 目标关注等级；<code>null</code> 表示回滚到滚动建议。
     * @return 返回重置后的报告 JSON；报告不存在或更新失败时返回 <code>null</code>。
     */
    JSONObject resetReportAttention(AIGCHost host, long sn, Integer attention) {
        return host.resetReportAttention(sn, attention);
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
    PaintingReport modifyReportRemark(long reportSn, String remark) {
        this.ensureSelfChecked();

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
