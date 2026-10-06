/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.service.psychology.action;

import cell.core.talk.dialect.ActionDialect;
import cube.aigc.psychology.copilot.CopilotSetting;
import cube.aigc.spi.AIGCActionTask;
import cube.aigc.spi.ActionContext;
import cube.auth.AuthToken;
import cube.common.state.AIGCStateCode;
import cube.service.psychology.scene.CopilotManager;

/**
 * 申请陪练动作。
 *
 * <p>对应线协议动作 {@code applyCopilot}，逐字符等同于既有枚举
 * {@code AIGCAction.ApplyCopilot} 的 {@code name} 字段。</p>
 *
 * <p><b>状态码序列</b>（逐字复刻宿主原任务类）：
 * {@code NoToken → Ok / Failure / InvalidParameter}。</p>
 *
 * <p><b>InvalidParameter 来自 catch-all</b>：宿主原实现把
 * {@code new CopilotSetting(packet.data)} 放在 try 内，构造异常即回
 * {@code InvalidParameter}。故参数校验不单独判——保持一致。</p>
 */
public final class ApplyCopilotAction implements AIGCActionTask {

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
            CopilotSetting setting = new CopilotSetting(ctx.getRequest().data);
            CopilotSetting result = CopilotManager.getInstance().applyCopilot(authToken, setting);
            if (null != result) {
                ctx.respond(AIGCStateCode.Ok, result.toJSON());
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
