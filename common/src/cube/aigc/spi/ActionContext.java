/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.spi;

import cell.core.talk.dialect.ActionDialect;
import cube.auth.AuthToken;
import cube.common.Packet;
import cube.common.state.AIGCStateCode;
import org.json.JSONObject;

/**
 * 动作上下文。
 *
 * <p>由宿主在派发时创建，模块只读使用。上下文内部持有
 * <code>cell.core.talk</code> 类型是安全的：<code>common</code> 的编译类路径本就包含
 * <code>cell-3.0.jar</code>，且common 已有多个文件导入
 * <code>cell.core.talk.TalkContext</code>。</p>
 */
public interface ActionContext {

    /**
     * 获取本次请求的线协议动作名。
     *
     * @return 返回动作名。
     */
    String getAction();

    /**
     * 获取已解析的访问令牌。
     *
     * <p>当 {@link ActionBinding#requiresToken} 为 <code>true</code> 时本方法一定
     * 返回非 <code>null</code>（否则宿主已先行应答且不会进入处理器）；
     * 为 <code>false</code> 时可能返回 <code>null</code>。</p>
     *
     * @return 返回访问令牌，或 <code>null</code>。
     */
    AuthToken getToken();

    /**
     * 获取请求参数。
     *
     * @return 返回参数 JSON，永不为 <code>null</code>；请求未带 <code>data</code> 时返回空对象。
     */
    JSONObject getParams();

    /**
     * 获取原始请求封包。
     *
     * @return 返回请求封包。
     */
    Packet getRequest();

    /**
     * 获取原始请求方言。
     *
     * @return 返回请求方言。
     */
    ActionDialect getDialect();

    /**
     * 获取当前动作所属的模块实例。
     *
     * @return 返回模块实例，由路由器注入，模块无需单例自查。
     */
    ActionModule getModule();

    /**
     * 获取宿主能力接口。
     *
     * @return 返回宿主能力接口。
     */
    AIGCHost getHost();

    /**
     * 应答。
     *
     * <p><b>非流式动作</b>（{@link ActionBinding#streaming} 为 {@code false}）：
     * 幂等，重复调用只生效第一次，第二次记录 WARN。
     * 调用方无需为每个分支都应答——处理器返回后若尚未应答，
     * 宿主会以返回的状态码补一次空应答。</p>
     *
     * <p><b>流式动作</b>（{@link ActionBinding#streaming} 为 {@code true}）：
     * 可多次调用，每次产出一段，宿主不去重、返回后也不补空应答。
     * 此时处理器<b>必须</b>自行保证「无论成功失败都至少应答一次」，
     * 否则调用方会一直等待。</p>
     *
     * @param code 状态码。
     * @param data 应答数据，可为 {@code null}。
     */
    void respond(AIGCStateCode code, JSONObject data);

    /**
     * 空应答。
     *
     * @param code 状态码。
     */
    void respondEmpty(AIGCStateCode code);

    /**
     * 判断本上下文是否已应答。
     *
     * @return 已应答时返回 <code>true</code>。
     */
    boolean isResponded();

    /**
     * 提交异步任务。
     *
     * <p><b>长耗时任务必须走本方法</b>：其实现固定路由到模块独占队列，
     * 禁止直接使用宿主服务级共享线程池。</p>
     *
     * @param taskKey 任务键，用于日志与去重。
     * @param job 任务体。
     */
    void async(String taskKey, Runnable job);

    /**
     * 判断本请求是否已被取消。
     *
     * @return 已取消时返回 <code>true</code>。
     */
    boolean isCancelled();
}
