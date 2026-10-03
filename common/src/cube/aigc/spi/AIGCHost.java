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
import cube.plugin.PluginContext;
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
 * <p><b>Phase 1 提供实现</b>（<code>AIGCHostImpl</code>）。本阶段只冻结接口签名，
 * 因此模块若在本阶段被注册，其 {@link ActionContext#async} 会因宿主实现缺席
 * 而丢弃任务并记录 WARN。</p>
 *
 * <p>本接口按 7 组能力组织，其中 24 个方法是既有 <code>AIGCService</code>
 * 公共方法的纯委托，可逐字段对拍；另 5 个是宿主需新增的派生能力。</p>
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
     * @return 返回 <code>&lt;workingPath&gt;/modules/&lt;moduleName&gt;/</code>。
     */
    File getModuleWorkingPath();

    /**
     * 读取模块资源。
     *
     * <p>优先读取 <code>&lt;workingPath&gt;/assets/modules/&lt;moduleName&gt;/</code>，
     * 回退到模块迁移前的旧资源路径，以兼容既有部署。</p>
     *
     * @param relativePath 相对路径。
     * @return 返回资源内容，不存在时返回 <code>null</code>。
     */
    String readModuleResource(String relativePath);

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
     * 查询联系人最近的应用事件。
     *
     * @param contactId 联系人 ID。
     * @param limit 数量上限。
     * @return 返回事件列表。
     */
    List<AppEvent> queryAppEvents(long contactId, int limit);

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
     * <p>钩子键为裸字符串（如 <code>"AppEvent"</code>），
     * 因为宿主侧的钩子键常量位于 service 模块，不能作为 SPI 类型暴露；
     * 宿主实现负责字符串到常量的映射。</p>
     *
     * @param hookKey 钩子键。
     * @param context 钩子上下文。
     */
    void fireHook(String hookKey, PluginContext context);

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
}
