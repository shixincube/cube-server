/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.psychology.action;

import cell.core.talk.dialect.ActionDialect;
import cell.util.log.Logger;
import cube.aigc.psychology.Attribute;
import cube.aigc.psychology.consultation.ConsultationTheme;
import cube.aigc.spi.AIGCActionTask;
import cube.aigc.spi.ActionContext;
import cube.auth.AuthToken;
import cube.common.entity.CounselingStrategy;
import cube.common.state.AIGCStateCode;
import cube.service.psychology.scene.CounselingManager;

/**
 * 查询咨询策略动作。
 *
 * <p>对应线协议动作 {@code queryCounselingStrategy}，逐字符等同于既有枚举
 * {@code AIGCAction.QueryCounselingStrategy} 的 {@code name} 字段。</p>
 *
 * <p><b>状态码序列</b>（逐字复刻宿主原任务类）：
 * {@code InvalidParameter → Ok / NoData / Failure}。</p>
 *
 * <p><b>注意与 {@code QueryCounselingCaptionAction} 的差异</b>：本动作在
 * 「无 token 参数」时回的是 {@code InvalidParameter}（不是 NoToken），
 * 因为宿主原实现把 token 校验与参数校验合在同一个 if 里。</p>
 */
public final class QueryCounselingStrategyAction implements AIGCActionTask {

    @Override
    public AIGCStateCode handle(ActionContext ctx) {
        ActionDialect dialect = ctx.getDialect();

        String tokenCode = dialect.containsParam("token") ? dialect.getParamAsString("token") : null;
        if (null == tokenCode || !ctx.getRequest().data.has("theme")
                || !ctx.getRequest().data.has("attribute")
                || !ctx.getRequest().data.has("streamName")) {
            ctx.respondEmpty(AIGCStateCode.InvalidParameter);
            return AIGCStateCode.InvalidParameter;
        }

        try {
            AuthToken authToken = ctx.getHost().resolveToken(tokenCode);
            ConsultationTheme theme = ConsultationTheme.parse(ctx.getRequest().data.getString("theme"));
            Attribute attribute = new Attribute(ctx.getRequest().data.getJSONObject("attribute"));
            String streamName = ctx.getRequest().data.getString("streamName");
            int index = ctx.getRequest().data.getInt("index");

            CounselingStrategy result = CounselingManager.getInstance().queryCounselingStrategy(
                    authToken, theme, attribute, streamName, index);
            if (null == result) {
                ctx.respondEmpty(AIGCStateCode.NoData);
                return AIGCStateCode.NoData;
            }

            ctx.respond(AIGCStateCode.Ok, result.toJSON());
            return AIGCStateCode.Ok;
        } catch (Exception e) {
            Logger.w(this.getClass(), "#handle", e);
            ctx.respondEmpty(AIGCStateCode.Failure);
            return AIGCStateCode.Failure;
        }
    }
}
