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
     * 构造函数。要求令牌，状态码白名单不限。
     *
     * @param action 动作名。
     * @param task 动作处理器。
     */
    public ActionBinding(String action, AIGCActionTask task) {
        this(action, task, true, null);
    }

    /**
     * 构造函数。
     *
     * @param action 动作名。
     * @param task 动作处理器。
     * @param requiresToken 是否要求已解析令牌。
     * @param allowedStates 允许的状态码白名单，可为 <code>null</code>。
     */
    public ActionBinding(String action, AIGCActionTask task, boolean requiresToken,
            AIGCStateCode[] allowedStates) {
        this.action = action;
        this.task = task;
        this.requiresToken = requiresToken;
        this.allowedStates = allowedStates;
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
