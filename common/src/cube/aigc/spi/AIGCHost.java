/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.spi;

import cell.core.net.Endpoint;
import cell.core.talk.dialect.ActionDialect;
import cube.aigc.AppEvent;
import cube.aigc.TaskDescriptor;
import cube.aigc.Usage;
import cube.aigc.psychology.Attribute;
import cube.aigc.psychology.Painting;
import cube.aigc.psychology.PaintingReport;
import cube.aigc.psychology.ScaleReport;
import cube.aigc.psychology.Theme;
import cube.aigc.psychology.composition.HexagonDimensionScore;
import cube.aigc.psychology.composition.PaintingFeatureSet;
import cube.aigc.psychology.composition.Scale;
import cube.aigc.psychology.listener.PaintingReportListener;
import cube.aigc.psychology.listener.ScaleReportListener;
import cube.auth.AuthToken;
import cube.common.Language;
import cube.common.entity.AIGCChannel;
import cube.common.entity.AIGCUnit;
import cube.common.entity.Contact;
import cube.common.entity.FileLabel;
import cube.common.entity.GeneratingOption;
import cube.common.entity.GeneratingRecord;
import cube.common.entity.ObjectInfo;
import cube.core.Storage;
import cube.storage.StorageFactory;
import cube.storage.StorageType;
import org.json.JSONObject;

import java.io.File;
import java.util.List;

/**
 * 宿主能力接口。
 *
 * <p>业务模块通过本接口访问宿主能力，<b>不得</b>直接引用
 * <code>cube.service.*</code> 下的任何类型，从而保证「插件只依赖
 * <code>cube-common-*.jar</code>」这一依赖方向约束。</p>
 *
 * <p><b>实现由宿主提供</b>（<code>service</code> 模块内的 <code>AIGCHostImpl</code>）。
 * 模块只应持有本接口引用，不得持有实现类。</p>
 *
 * <p>本接口按 11 组能力组织，其中 28 个方法是既有 <code>AIGCService</code>
 * 公共方法的纯委托，可逐字段对拍；另 25 个是宿主需新增的派生能力。</p>
 */
public interface AIGCHost {

    // ───────── ① 身份与文本 ─────────

    /**
     * 解析访问令牌。
     *
     * @param tokenCode 令牌码。
     * @return 返回访问令牌，无效时返回 <code>null</code>。
     */
    AuthToken resolveToken(String tokenCode);

    /**
     * 判断令牌对应的用户是否已注册。
     *
     * <p><b>为何返回布尔值而非 {@code User}</b>：{@code cube.common.entity.User}
     * 虽是 common 类型，但暴露它会让插件拿到「注册时间 / 手机号 / 邮箱 / 头像」
     * 等与本判定无关的字段，从而具备自行推断注册状态的自由度；插件若持有
     * {@code User}，还容易顺势调用其实体方法，把「身份判定」滑向「身份操作」。
     * 收敛为布尔值后，插件只能回答「能不能用」，能力边界保持最小。</p>
     *
     * <p><b>为何不由插件自行实现</b>：原判定链为
     * {@code getUser(tokenCode).isRegistered()}，而 {@code getUser} 需经
     * {@code ContactManager} 取 {@code Contact}——该类位于 service 模块，
     * 插件的编译类路径中不存在（这是插件「零 service 依赖」铁律的直接后果）。</p>
     *
     * <p>本方法内部自行解析令牌，调用方<b>不需要</b>先调
     * {@link #resolveToken}。</p>
     *
     * @param tokenCode 令牌码。
     * @return 令牌有效且对应用户已注册时返回 <code>true</code>；令牌无效、
     *         联系人缺失或用户未注册时返回 <code>false</code>。
     */
    boolean isRegistered(String tokenCode);

    /**
     * 判断宿主是否已就绪。
     *
     * @return 就绪时返回 <code>true</code>。
     */
    boolean isReady();

    /**
     * 分词。
     *
     * <p>宿主内部的分词器位于 service 模块，不能作为 SPI 类型直接暴露，
     * 故在此收敛为词列表返回。</p>
     *
     * @param text 待分词文本。
     * @return 返回词列表。
     */
    List<String> tokenize(String text);

    /**
     * 整句分词。
     *
     * <p><b>与 {@link #tokenize(String)} 的区别（务必按语义选用，不可互换）</b>：
     * 本方法只切出基础词元，而 {@code tokenize} 走索引模式，会在每个长词元
     * 之前额外插入其命中词典的 2-gram 与 3-gram。因此同一个「他的」，
     * 本方法返回 {@code ["他的"]}，而 {@code tokenize} 可能返回
     * {@code ["他的", "他的", "他", "他的"]}（含重复与更细的粒度）。</p>
     *
     * <p>调用方若要「按词元做整词替换」（如把「他 / 她 / 他的」统一替换为
     * 第三方称谓），必须用本方法，否则会被插入的 n-gram 破坏词形边界，
     * 产出重复或错位的文本。</p>
     *
     * @param text 待分词文本。
     * @return 返回基础词元列表。
     */
    List<String> segmentWords(String text);

    /**
     * 抽取文本的 TF-IDF 关键词。
     *
     * <p>与 {@link #tokenize(String)} 的区别：分词返回全部切分结果且无权重，
     * 而本方法按 TF-IDF 权重降序返回前 <code>topN</code> 个关键词，
     * 用于「模型自由文本输出 → 匹配结构化候选项」这类场景
     * （如量表主观题的答案评级）。</p>
     *
     * <p>宿主内部的 TF-IDF 分析器位于 service 模块，且依赖宿主分词器
     * 与一份较大的 IDF 语料，不能作为 SPI 类型直接暴露，故在此收敛为
     * 词列表返回。</p>
     *
     * <p><b>实现约束</b>：语料与停用词表是<b>进程级静态缓存</b>，
     * 宿主实现必须复用同一分析器实例并在首次调用时完成加载，
     * 否则并发首调会重复加载语料。</p>
     *
     * @param content 待分析文本。
     * @param topN 关键词数量上限。
     * @return 返回按 TF-IDF 权重降序的关键词列表。
     */
    List<String> extractKeywords(String content, int topN);

    // ───────── ② 频道 ─────────

    /**
     * 按令牌码查询频道。
     *
     * @param tokenCode 令牌码。
     * @return 返回频道，不存在时返回 <code>null</code>。
     */
    AIGCChannel getChannelByToken(String tokenCode);

    /**
     * 申请频道，已存在时复用。
     *
     * @param token 访问令牌。
     * @param participant 参与者。
     * @param channelCode 频道码，为 <code>null</code> 时由宿主生成。
     * @param language 会话语言。
     * @return 返回频道。
     */
    AIGCChannel acquireChannel(AuthToken token, String participant, String channelCode, Language language);

    /**
     * 申请频道（由宿主按令牌码解析访问令牌）。
     *
     * @param tokenCode 令牌码。
     * @param participant 参与者。
     * @return 返回频道。
     */
    AIGCChannel requestChannel(String tokenCode, String participant);

    /**
     * 停止频道处理。
     *
     * @param channelCode 频道码。
     * @return 返回被停止的频道。
     */
    AIGCChannel stopChannel(String channelCode);

    // ───────── ③ 模型单元 ─────────
    //
    // 单元（{@link AIGCUnit}）是模型推理的执行载体，一个「联系人 + 能力」对应一个单元。
    // 选点由宿主按单元上报的 AICapability 名称匹配，模块只需传入自己的能力名，
    // 宿主不预置任何业务能力名。

    /**
     * 按能力名选择单元。
     *
     * @param capabilityName 单元能力名，等于单元上报的 <code>AICapability.getName()</code>。
     * @return 返回可用单元，无可用单元时返回 <code>null</code>。
     */
    AIGCUnit selectUnit(String capabilityName);

    /**
     * 按子任务名选择单元。
     *
     * @param subtask 子任务名。
     * @return 返回可用单元，无可用单元时返回 <code>null</code>。
     */
    AIGCUnit selectUnitBySubtask(String subtask);

    /**
     * 统计指定能力名下的有效单元数量。
     *
     * @param capabilityName 单元能力名。
     * @return 返回数量。
     */
    int countUnits(String capabilityName);

    /**
     * 判断是否存在指定能力名的单元。
     *
     * @param capabilityName 单元能力名。
     * @return 存在时返回 <code>true</code>。
     */
    boolean hasUnit(String capabilityName);

    /**
     * 增加指定单元的并发计数，用于长任务限流。
     *
     * @param unitName 单元能力名。
     * @return 返回该单元的并发计数器。
     */
    java.util.concurrent.atomic.AtomicInteger increaseUnitCounter(String unitName);

    /**
     * 以指定单元同步生成文本。
     *
     * @param unit 执行单元。
     * @param prompt 提示词。
     * @param option 生成选项。
     * @param history 历史记录。
     * @param participantContact 参与者联系人。
     * @return 返回生成结果。
     */
    GeneratingRecord syncGenerateText(AIGCUnit unit, String prompt, GeneratingOption option,
            List<GeneratingRecord> history, Contact participantContact);

    /**
     * 以指定能力名为指定联系人同步生成文本。
     *
     * <p><b>为何不能用 {@link #selectUnit(String)} + {@link #syncGenerateText} 组合替代</b>：
     * 单元选点有「亲和」与「全局」两种策略。前者会优先复用该联系人已绑定的
     * 单元，从而保证同一联系人的连续会话落在同一台推理机上（上下文缓存命中、
     * 结果风格一致）；后者在全池中按空闲度选点，与联系人无关。</p>
     *
     * <p>报告生成这类「同一联系人连续多段生成」的场景必须走亲和选点，
     * 换成全局选点会改变单元分布，属行为变更而非等价重构。</p>
     *
     * @param token 调用者的访问令牌，用于亲和选点。
     * @param capabilityName 单元能力名。
     * @param prompt 提示词。
     * @param option 生成选项。
     * @param history 历史记录，可为 {@code null}。
     * @param participantContact 参与者联系人，可为 {@code null}。
     * @return 返回生成结果；无可用单元时返回 {@code null}。
     */
    GeneratingRecord syncGenerateText(AuthToken token, String capabilityName, String prompt,
            GeneratingOption option, List<GeneratingRecord> history, Contact participantContact);

    /**
     * 以指定单元同步生成可校验的结构化结果。
     *
     * <p><b>本方法是 SPI 首发版的唯一真实缺口</b>：诸如「巡检地图 + 导航点
     * → 巡检任务计划」这类业务，必须拿到符合 JSON Schema 的结果，
     * 而自由文本接口无法满足。</p>
     *
     * @param unit 执行单元。
     * @param prompt 提示词。
     * @param jsonSchema JSON Schema 描述。
     * @param schemaName 模式名，用于日志与指标。
     * @param timeoutMs 超时（毫秒）。
     * @return 返回解析后的结构化结果。
     */
    JSONObject generateStructured(AIGCUnit unit, String prompt, String jsonSchema,
            String schemaName, long timeoutMs);

    /**
     * 以指定单元执行一次受控调用。
     *
     * <p>宿主负责超时控制、日志、指标与熔断。</p>
     *
     * @param unit 执行单元。
     * @param action 动作名。
     * @param data 动作数据。
     * @param timeoutMs 超时（毫秒）。
     * @return 返回应答方言。
     */
    ActionDialect invokeUnit(AIGCUnit unit, String action, JSONObject data, long timeoutMs);

    // ───────── ④ 文件与资源 ─────────

    /**
     * 获取文件标签。
     *
     * @param domain 域。
     * @param fileCode 文件码。
     * @return 返回文件标签，不存在时返回 <code>null</code>。
     */
    cube.common.entity.FileLabel getFile(String domain, String fileCode);

    /**
     * 加载文件内容。
     *
     * @param domain 域。
     * @param fileCode 文件码。
     * @return 返回文件，不存在时返回 <code>null</code>。
     */
    File loadFile(String domain, String fileCode);

    /**
     * 保存文件。
     *
     * @param token 访问令牌。
     * @param fileCode 文件码。
     * @param file 文件内容。
     * @param filename 文件名。
     * @param deleteAfterSave 保存后是否删除源文件。
     * @return 返回文件标签。
     */
    cube.common.entity.FileLabel saveFile(AuthToken token, String fileCode, File file, String filename,
            boolean deleteAfterSave);

    /**
     * 删除文件。
     *
     * @param domain 域。
     * @param fileCode 文件码。
     * @return 返回被删除的文件标签。
     */
    cube.common.entity.FileLabel deleteFile(String domain, String fileCode);

    /**
     * 获取模块工作目录。
     *
     * <p>目录不存在时由宿主创建，因此模块可直接往其中写文件。</p>
     *
     * @param moduleName 模块名。
     * @return 返回 <code>&lt;workingPath&gt;/modules/&lt;moduleName&gt;/</code>。
     */
    File getModuleWorkingPath(String moduleName);

    /**
     * 读取模块资源。
     *
     * <p>按固定顺序回退，兼容模块迁移前的既有部署：</p>
     * <ol>
     *   <li><code>&lt;workingPath&gt;/assets/modules/&lt;moduleName&gt;/&lt;relativePath&gt;</code></li>
     *   <li><code>assets/&lt;moduleName&gt;/&lt;relativePath&gt;</code>（迁移前的旧路径）</li>
     * </ol>
     *
     * @param moduleName 模块名。
     * @param relativePath 相对路径。
     * @return 返回资源内容，两级路径均不存在时返回 <code>null</code>。
     */
    String readModuleResource(String moduleName, String relativePath);

    /**
     * 读取宿主引导提示词。
     *
     * <p><b>为何不能用模块自带的语料读取替代</b>：宿主提示词存在
     * <code>assets/prompt/</code> 下，而心理学语料存在
     * <code>assets/psychology/corpus.json</code>，两者虽有同名的
     * <code>FORMAT_POLISH</code>，但<b>措辞并不相同</b>（前者以三引号包裹、
     * 后者要求保持原换行结构）。提示词措辞直接影响模型输出，
     * 换用另一份属于行为变更而非等价重构，故须经宿主原样取用。</p>
     *
     * <p>提示词键为裸字符串，宿主不预置业务键名；调用方须自行约定。</p>
     *
     * @param promptName 提示词名。
     * @return 返回中文提示词模板；不存在时返回 <code>null</code>。
     */
    String getGuidePrompt(String promptName);

    // ───────── ⑤ 存储 ─────────

    /**
     * 打开模块私有存储。
     *
     * <p>宿主代管存储的打开与关闭顺序，模块不自行管理存储生命周期。</p>
     *
     * @param storageName 存储名。
     * @param type 存储类型。
     * @param config 存储配置。
     * @return 返回存储实例。
     */
    Storage openModuleStorage(String storageName, StorageType type, JSONObject config);

    /**
     * 查询联系人的用量记录。
     *
     * @param contactId 联系人 ID。
     * @return 返回用量记录列表。
     */
    List<Usage> queryUsages(long contactId);

    /**
     * 查询联系人的最近应用事件。
     *
     * <p>事件名取值等于 {@link AppEvent} 的常量（<code>Session</code> /
     * <code>Chat</code> / <code>Knowledge</code> 等），宿主不预置业务事件名。
     * 事件名常量本身位于 common，故可直接引用。</p>
     *
     * @param contactId 联系人 ID。
     * @param eventName 事件名。
     * @param limit 数量上限。
     * @return 返回事件列表，按时间戳倒序；服务未就绪时返回空列表。
     */
    List<AppEvent> queryAppEvents(long contactId, String eventName, int limit);

    // ───────── ⑥ 调度与横切 ─────────

    /**
     * 提交延迟任务。
     *
     * <p><b>实现须路由到模块独占队列</b>，不得直接暴露宿主服务级共享线程池，
     * 否则长阻塞任务会挤占其他业务的短任务。</p>
     *
     * @param taskKey 任务键，用于日志与去重。
     * @param delayMs 延迟（毫秒）。
     * @param job 任务体。
     */
    void schedule(String taskKey, long delayMs, Runnable job);

    /**
     * 触发横切事件钩子。
     *
     * <p>钩子键为裸字符串（如 <code>"TaskProcessing"</code>），
     * 因为宿主侧的钩子键常量位于 service 模块，不能作为 SPI 类型暴露；
     * 宿主实现负责字符串到常量的映射。</p>
     *
     * <p>上下文须为 {@link AIGCPluginContextLite}：宿主实现会将其翻译成
     * service 侧的上下文再交给钩子消费者，因此插件<b>不能</b>自己构造
     * service 侧上下文（那将破坏依赖方向）。</p>
     *
     * @param hookKey 钩子键。
     * @param context 钩子上下文，类型须为 {@link AIGCPluginContextLite}。
     */
    void fireHook(String hookKey, AIGCPluginContextLite context);

    /**
     * 触发应用事件。
     *
     * @param appEvent 应用事件。
     * @return 处理成功时返回 <code>true</code>。
     */
    boolean fireEvent(AppEvent appEvent);

    /**
     * 写入用量留痕。
     *
     * @param descriptor 任务描述。
     */
    void writeUsage(TaskDescriptor descriptor);

    // ───────── ⑦ 兄弟模块 ─────────

    /**
     * 获取兄弟模块实例。
     *
     * <p>可用性已由 {@link ModuleDescriptor#requires} 在装载时校验，
     * 但本方法仍做类型校验：不匹配时返回 <code>null</code> 而非抛异常。</p>
     *
     * @param <T> 模块类型。
     * @param moduleName 模块名。
     * @param type 模块类型。
     * @return 返回模块实例，不存在或类型不匹配时返回 <code>null</code>。
     */
    <T> T getSiblingModule(String moduleName, Class<T> type);

    // ───────── ⑧ 报告运行态 ─────────

    /**
     * 查询绘画报告。
     *
     * <p><b>为何整条读取走宿主而非插件自建</b>：报告在生成过程中先落在宿主的
     * 内存表中，此刻尚未入库。若模块自行读存储，将查不到「正在生成中」的报告，
     * 使调用方从「查得到（状态为生成中）」变成「查不到」——
     * 这是线协议可见的行为回归。故内存表与队列的归属留在宿主。</p>
     *
     * @param sn 报告序列号。
     * @param format 导出格式：{@code compact} 为摘要、
     *                {@code markdown} 为 Markdown 正文、
     *                {@code sections} 为分节 JSON。
     * @return 返回报告 JSON；不存在时返回 {@code null}。
     */
    JSONObject queryPaintingReport(long sn, String format);

    /**
     * 查询绘画报告实体。
     *
     * <p>与 {@link #queryPaintingReport(long, String)} 的区别：后者返回
     * 已经导出的 JSON（形态固定），本方法返回报告本体，供需要读取
     * 状态、章节、特征集等结构化字段的调用方使用。</p>
     *
     * <p>⚠️ 返回的是<b>宿主内存态中的同一个实例</b>，调用方<b>不得</b>修改它；
     * 如需修改请先复制。</p>
     *
     * @param sn 报告序列号。
     * @return 返回报告实体；不存在时返回 {@code null}。
     */
    PaintingReport getPaintingReport(long sn);

    /**
     * 查询量表报告。
     *
     * @param sn 报告序列号。
     * @return 返回报告 JSON；不存在时返回 {@code null}。
     */
    JSONObject queryScaleReport(long sn);

    /**
     * 分页查询绘画报告。
     *
     * @param contactId 联系人 ID。
     * @param page 页码，从 0 开始。
     * @param size 每页条数。
     * @param descending 是否倒序（按时间）。
     * @param state 报告状态；{@code -1} 表示不限状态。
     * @return 返回含 {@code total} 与 {@code list} 的对象。
     */
    JSONObject listPaintingReports(long contactId, int page, int size, boolean descending, int state);

    /**
     * 查询量表报告列表。
     *
     * @param contactId 联系人 ID。
     * @param descending 是否倒序（按时间）。
     * @param state 报告状态；{@code -1} 表示不限状态。
     * @return 返回含 {@code total} 与 {@code list} 的对象。
     */
    JSONObject listScaleReports(long contactId, boolean descending, int state);

    /**
     * 停止报告生成。
     *
     * <p>仅当报告仍在生成队列中时生效；已完成或不在队列中时返回
     * {@code null}，且<b>不</b>修改任何状态。</p>
     *
     * @param sn 报告序列号。
     * @return 返回被停止的报告 JSON；未生效时返回 {@code null}。
     */
    JSONObject stopReportGeneration(long sn);

    /**
     * 重置报告关注等级。
     *
     * @param sn 报告序列号。
     * @param newAttention 目标关注等级；{@code null} 表示回滚到滚动建议。
     * @return 返回重置后的报告 JSON；报告不存在或更新失败时返回 {@code null}。
     */
    JSONObject resetReportAttention(long sn, Integer newAttention);

    // ───────── ⑨ 报告内容加工 ─────────

    /**
     * 生成六维得分描述。
     *
     * <p>六维描述的算法依赖宿主分词器与 TF-IDF 语料，二者均在 service 模块，
     * 不能作为 SPI 类型暴露，故收敛为一个「就地填充描述」的方法。</p>
     *
     * <p>模块侧典型用法是把它包装成函数式接口注入自己的存储层，
     * 与宿主侧已有的注入方式同构。</p>
     *
     * @param hds 六维得分，将被就地填充。
     * @param language 会话语言。
     */
    void fillHexagonScoreDescription(HexagonDimensionScore hds, Language language);

    /**
     * 提取文本的语义片段。
     *
     * <p>依赖宿主的数据集与 TF-IDF 语料，模块无法自行实现。</p>
     *
     * @param query 待提取的文本。
     * @return 返回语义片段；无匹配时返回 {@code null}。
     */
    String extractContent(String query);

    /**
     * 查询绘画特征集。
     *
     * <p>与 {@link #queryPaintingReport(long, String)} 同理，特征集在报告
     * 生成过程中先落在宿主内存态，此刻尚未入库，模块自行查询会漏掉
     * 「正在生成中」的那一份。</p>
     *
     * @param reportSn 报告序列号。
     * @return 返回特征集；不存在时返回 {@code null}。
     */
    PaintingFeatureSet getPaintingFeatureSet(long reportSn);

    /**
     * 把报告加工为指定形态的文本。
     *
     * @param report 报告。
     * @param summary 是否为摘要形态。
     * @param maxIndicators 最多输出多少个指标。
     * @param personality 是否输出人格描述。
     * @return 返回 Markdown 文本。
     */
    String makeReportContent(PaintingReport report, boolean summary, int maxIndicators, boolean personality);

    /**
     * 生成报告的评级信息文本。
     *
     * @param report 报告。
     * @return 返回 Markdown 文本。
     */
    String makeRatingInformation(PaintingReport report);

    /**
     * 生成报告的页面链接。
     *
     * @param endpoint 访问端点。
     * @param token 访问令牌码。
     * @param report 报告。
     * @param indicatorLink 是否输出指标链接。
     * @param personalityLink 是否输出人格链接。
     * @return 返回 Markdown 文本。
     */
    String makePageLink(Endpoint endpoint, String token, PaintingReport report,
            boolean indicatorLink, boolean personalityLink);

    /**
     * 把绘画特征集加工为文本。
     *
     * @param featureSet 绘画特征集。
     * @return 返回 Markdown 文本。
     */
    String makePaintingFeature(PaintingFeatureSet featureSet);

    // ───────── ⑪ 报告生成编排 ─────────

    /**
     * 生成绘画报告。
     *
     * <p><b>为何编排留在宿主而不在模块内</b>：报告生成需要「排队 + 起工作线程 +
     * 按单元数限并发 + 写回存储」，其中队列与线程管理是宿主运行时职责；模块内
     * 既拿不到这些设施，硬搬过去会要求 SPI 再暴露整套线程池与队列，能力面反而
     * 扩大。因此模块只提供查询与数据能力，编排由宿主实现。</p>
     *
     * <p>该方法使宿主门面无需直接引用业务场景单例，从而消除「平台门面反向
     * 依赖业务实现类」的方向倒置。</p>
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
     * @return 返回已入队的报告；入队失败时返回 {@code null}。
     */
    PaintingReport generatePaintingReport(AIGCChannel channel, Attribute attribute, FileLabel fileLabel,
            Theme theme, int maxIndicators, boolean adjust, int retention, String remark,
            PaintingReportListener listener);

    /**
     * 生成量表测验报告。
     *
     * @param channel 会话频道。
     * @param scale 量表。
     * @param language 会话语言。
     * @param listener 完成回调。
     * @return 返回已入队的报告；入队失败时返回 {@code null}。
     */
    ScaleReport generateScaleReport(AIGCChannel channel, Scale scale, Language language,
            ScaleReportListener listener);

    /**
     * 按序列号读取量表。
     *
     * <p>与 {@link #generateScaleReport} 同属编排入口：门面在生成量表报告前
     * 需先取量表本体，故一并经本接口取，避免门面直连业务场景。</p>
     *
     * @param sn 量表序列号。
     * @return 返回量表；不存在时返回 {@code null}。
     */
    Scale getScale(long sn);

    // ───────── ⑩ 计算机视觉与绘画 ─────────

    /**
     * 检测图像中的物体。
     *
     * <p>该能力由宿主转发至计算机视觉服务处理，模块侧无法自行实现。
     * 返回的 {@link ObjectInfo} 位于 common，模块可直接读取其物体列表。</p>
     *
     * @param domain 存储域。
     * @param fileCode 文件码。
     * @param visualize 是否输出可视化标注。
     * @return 返回检测结果；检测失败时返回 {@code null}。
     */
    ObjectInfo detectObject(String domain, String fileCode, boolean visualize);

    /**
     * 读取绘画推理数据。
     *
     * <p>产出内容为构图元素与关联边构成的图表数据，纯读，无副作用。</p>
     *
     * @param sn 报告序列号。
     * @return 返回图表数据；报告不存在或尚未完成推理时返回 {@code null}。
     */
    JSONObject getPaintingInferenceData(long sn);

    /**
     * 预测绘画要素。
     *
     * @param token 访问令牌。
     * @param fileCode 文件码。
     * @return 返回绘画要素；预测失败时返回 {@code null}。
     */
    Painting getPredictedPainting(AuthToken token, String fileCode);

    /**
     * 预测绘画要素并输出带标注的图像。
     *
     * <p>与 {@link #getPredictedPainting(AuthToken, String)} 的区别：本方法
     * 会把识别到的要素绘制到图上并保存为新文件，因此<b>有写副作用</b>，
     * 且调用方需自行删除临时文件。</p>
     *
     * @param token 访问令牌。
     * @param sn 报告序列号。
     * @param boundingBox 是否输出外框。
     * @param visualParam 是否输出视觉参数。
     * @param probability 置信度阈值。
     * @return 返回新生成图像的标签；预测失败时返回 {@code null}。
     */
    FileLabel getPredictedPainting(AuthToken token, long sn, boolean boundingBox, boolean visualParam,
            double probability);
}
