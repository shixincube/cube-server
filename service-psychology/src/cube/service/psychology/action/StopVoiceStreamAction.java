/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.psychology.action;

import cell.core.talk.dialect.ActionDialect;
import cube.aigc.spi.AIGCActionTask;
import cube.aigc.spi.ActionContext;
import cube.auth.AuthToken;
import cube.common.state.AIGCStateCode;
import cube.service.psychology.scene.VoiceStreamService;

/**
 * 停止语音流动作。
 *
 * <p>对应线协议动作 {@code stopVoiceStream}，逐字符等同于既有枚举
 * {@code AIGCAction.StopVoiceStream} 的 {@code name} 字段。</p>
 *
 * <p><b>状态码序列</b>：{@code InvalidParameter → Ok / Failure}。</p>
 *
 * <p><b>Ok 与 Failure 的判据</b>：{@link VoiceStreamService#stop} 在该流
 * <b>已有归档记录</b>时返回 {@code false}（重复停止），否则返回 {@code true}。
 * 这与宿主原实现一致：先查库，已有记录即视为已停止。</p>
 *
 * <p><b>为何 requiresToken 取 false</b>：宿主原任务类只校验「方言里有没有
 * token 参数」，未校验令牌有效性。交由骨架前置校验会把无效令牌变成
 * {@code InconsistentToken}，属线协议可见变更。</p>
 */
public final class StopVoiceStreamAction implements AIGCActionTask {

    @Override
    public AIGCStateCode handle(ActionContext ctx) {
        ActionDialect dialect = ctx.getDialect();

        String tokenCode = dialect.containsParam("token") ? dialect.getParamAsString("token") : null;
        if (null == tokenCode || !ctx.getRequest().data.has("streamName")) {
            ctx.respond(AIGCStateCode.InvalidParameter, ctx.getRequest().data);
            return AIGCStateCode.InvalidParameter;
        }

        String streamName = ctx.getRequest().data.getString("streamName");
        AuthToken authToken = ctx.getHost().resolveToken(tokenCode);

        boolean success = VoiceStreamService.getInstance().stop(authToken, streamName);

        if (success) {
            ctx.respond(AIGCStateCode.Ok, ctx.getRequest().data);
            return AIGCStateCode.Ok;
        }

        ctx.respond(AIGCStateCode.Failure, ctx.getRequest().data);
        return AIGCStateCode.Failure;
    }
}
