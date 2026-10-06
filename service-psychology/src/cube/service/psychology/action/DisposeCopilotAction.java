/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.service.psychology.action;

import cell.core.talk.dialect.ActionDialect;
import cube.aigc.spi.AIGCActionTask;
import cube.aigc.spi.ActionContext;
import cube.auth.AuthToken;
import cube.common.state.AIGCStateCode;
import cube.service.psychology.scene.CopilotManager;

/**
 * 完成陪练动作。
 *
 * <p>对应线协议动作 {@code disposeCopilot}，逐字符等同于既有枚举
 * {@code AIGCAction.DisposeCopilot} 的 {@code name} 字段。</p>
 *
 * <p><b>状态码序列</b>（逐字复刻宿主原任务类）：
 * {@code NoToken → Ok / Failure / InvalidParameter}。</p>
 *
 * <p>{@code sn} 缺失时 {@code getLong} 抛异常，落 catch 回
 * {@code InvalidParameter}（宿主原行为）。</p>
 */
public final class DisposeCopilotAction implements AIGCActionTask {

    @Override
    public AIGCStateCode handle(ActionContext ctx) {
        ActionDialect dialect = ctx.getDialect();

        String tokenCode = dialect.containsParam("token") ? dialect.getParamAsString("token") : null;
        if (null == tokenCode) {
            ctx.respondEmpty(AIGCStateCode.NoToken);
            return AIGCStateCode.NoToken;
        }

        AuthToken authToken = ctx.getHost().resolveToken(tokenCode);
        if (null == authToken) {
            ctx.respondEmpty(AIGCStateCode.NoToken);
            return AIGCStateCode.NoToken;
        }

        try {
            long sn = ctx.getRequest().data.getLong("sn");
            CopilotManager.Copilot copilot = CopilotManager.getInstance().disposeCopilot(authToken, sn);
            if (null != copilot) {
                ctx.respond(AIGCStateCode.Ok, copilot.toCompactJSON());
                return AIGCStateCode.Ok;
            }

            ctx.respondEmpty(AIGCStateCode.Failure);
            return AIGCStateCode.Failure;
        } catch (Exception e) {
            ctx.respondEmpty(AIGCStateCode.InvalidParameter);
            return AIGCStateCode.InvalidParameter;
        }
    }
}
