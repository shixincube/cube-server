/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.service.psychology.action;

import cell.core.talk.dialect.ActionDialect;
import cube.aigc.psychology.copilot.CopilotSetting;
import cube.aigc.psychology.copilot.CopilotSheet;
import cube.aigc.spi.AIGCActionTask;
import cube.aigc.spi.ActionContext;
import cube.auth.AuthToken;
import cube.common.state.AIGCStateCode;
import cube.service.psychology.scene.CopilotManager;

/**
 * 提交陪练数据动作。
 *
 * <p>对应线协议动作 {@code submitCopilotSheet}，逐字符等同于既有枚举
 * {@code AIGCAction.SubmitCopilotSheet} 的 {@code name} 字段。</p>
 *
 * <p><b>状态码序列</b>（逐字复刻宿主原任务类）：
 * {@code NoToken → Ok / Failure / InvalidParameter}。</p>
 *
 * <p>应答回的是<b>更新后的设置</b>（含模型给出的深度策略句），
 * 而非提交的原始数据。</p>
 */
public final class SubmitCopilotSheetAction implements AIGCActionTask {

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
            CopilotSheet sheet = new CopilotSheet(ctx.getRequest().data);
            CopilotSetting result = CopilotManager.getInstance().submitContent(authToken, sheet);
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
