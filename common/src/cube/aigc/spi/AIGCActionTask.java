/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.spi;

import cube.common.state.AIGCStateCode;

/**
 * AIGC 动作处理器。
 *
 * <p>实现必须是<b>无状态单例</b>：所有请求级数据从参数 {@link ActionContext}
 * 取得，实现内部不得持有可变的请求状态。这样同一个处理器实例可以被任意
 * 数量的并发请求安全复用。</p>
 */
public interface AIGCActionTask {

    /**
     * 处理一个动作请求。
     *
     * <p>宿主已代办的样板（令牌解析、异常兜底、应答封装、应答计时）不再重复提供，
     * 实现只需「取参数 → 调 {@link AIGCHost} → 应答」。</p>
     *
     * @param ctx 动作上下文。
     * @return 本次处理的状态码，仅用于日志与指标；返回 <code>null</code> 视为
     *         {@link AIGCStateCode#Ok}。
     */
    AIGCStateCode handle(ActionContext ctx);
}
