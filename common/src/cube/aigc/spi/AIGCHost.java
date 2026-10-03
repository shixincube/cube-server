/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.spi;

import cell.core.talk.dialect.ActionDialect;
import cube.aigc.AppEvent;
import cube.aigc.TaskDescriptor;
import cube.aigc.Usage;
import cube.auth.AuthToken;
import cube.common.Language;
import cube.common.entity.AIGCChannel;
import cube.common.entity.AIGCUnit;
import cube.common.entity.Contact;
import cube.common.entity.GeneratingOption;
import cube.common.entity.GeneratingRecord;
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
 * <p>本接口按 8 组能力组织，其中 24 个方法是既有 <code>AIGCService</code>
 * 公共方法的纯委托，可逐字段对拍；另 13 个是宿主需新增的派生能力。</p>
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
}
