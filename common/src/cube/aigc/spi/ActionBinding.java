/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.spi;

import cube.common.state.AIGCStateCode;

/**
 * 动作绑定。
 *
 * <p>把线协议上的 action 字符串与一个无状态任务处理器绑定。
 * {@link #action} 必须与既有枚举的 <code>name</code> 字段逐字符相同
 * （例如 <code>"generatePsychologyReport"</code>，<b>不带模块前缀</b>），
 * 以保证线协议一致。</p>
 */
public final class ActionBinding {

    /**
     * 线协议动作名，精确匹配（大小写敏感）。
     */
    public final String action;

    /**
     * 动作处理器。必须是<b>无状态单例</b>，请求级状态一律放在
     * {@link ActionContext} 中。
     */
    public final AIGCActionTask task;

    /**
     * 是否要求已解析出访问令牌。
     *
     * <p><code>true</code>（默认）时宿主在调用处理器之前完成令牌解析，
     * 解析失败直接以 {@link AIGCStateCode#InconsistentToken} 应答且不进入处理器；
     * <code>false</code> 用于等价于 <code>checkToken</code> 这类本身就要校验令牌的动作。</p>
     */
    public final boolean requiresToken;

    /**
     * 允许本动作回复的状态码白名单，可为 <code>null</code> 表示不限制。
     */
    public final AIGCStateCode[] allowedStates;

    /**
     * 是否为流式动作。
     *
     * <p><code>false</code>（默认，绝大部分动作）时宿主维持「单次应答」契约：
     * {@link ActionContext#respond} 幂等（重复调用只生效第一次），
     * 且处理器返回时若尚未应答，宿主以返回码补一次空应答。</p>
     *
     * <p><code>true</code> 时上述两条约束<b>均被解除</b>：处理器可经回调
     * 多次应答（每次携带不同的流式片段），宿主既不去重、也不补空应答。
     * 适用于「一次请求、多次 speak」的流式协议（如
     * {@code psychologyConversation}）。</p>
     *
     * <p><b>为何不能用幂等开关代替</b>：流式的语义是「一个请求对应多个应答」，
     * 而幂等开关只能「允许多次调用但只有第一次生效」，那会让后续片段全部丢失。
     * 反过来，若对流式动作保留「返回时补空应答」，处理器交回控制权而尚未
     * 产出首段时，客户端会先收到一个空的成功应答，随后的片段就成了 unsolicited。
     * 两者都不是流式，故须显式区分。</p>
     */
    public final boolean streaming;

    /**
     * 构造函数。要求令牌，状态码白名单不限，单次应答。
     *
     * @param action 动作名。
     * @param task 动作处理器。
     */
    public ActionBinding(String action, AIGCActionTask task) {
        this(action, task, true, null, false);
    }

    /**
     * 构造函数。单次应答。
     *
     * @param action 动作名。
     * @param task 动作处理器。
     * @param requiresToken 是否要求已解析令牌。
     * @param allowedStates 允许的状态码白名单，可为 {@code null}。
     */
    public ActionBinding(String action, AIGCActionTask task, boolean requiresToken,
            AIGCStateCode[] allowedStates) {
        this(action, task, requiresToken, allowedStates, false);
    }

    /**
     * 构造函数。
     *
     * @param action 动作名。
     * @param task 动作处理器。
     * @param requiresToken 是否要求已解析令牌。
     * @param allowedStates 允许的状态码白名单，可为 {@code null}。
     * @param streaming 是否为流式动作（允许处理器多次应答且不补空应答）。
     */
    public ActionBinding(String action, AIGCActionTask task, boolean requiresToken,
            AIGCStateCode[] allowedStates, boolean streaming) {
        this.action = action;
        this.task = task;
        this.requiresToken = requiresToken;
        this.allowedStates = allowedStates;
        this.streaming = streaming;
    }

    /**
     * 判断本绑定是否响应对应动作。
     *
     * <p>匹配规则为精确匹配（大小写敏感），与线协议一致。</p>
     *
     * @param name 待匹配的动作名。
     * @return 匹配时返回 <code>true</code>。
     */
    public boolean matches(String name) {
        return null != this.action && this.action.equals(name);
    }
}
