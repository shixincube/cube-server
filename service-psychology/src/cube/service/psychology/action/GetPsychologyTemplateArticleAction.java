/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.service.psychology.action;

import cell.core.talk.dialect.ActionDialect;
import cell.util.log.Logger;
import cube.aigc.psychology.composition.ReportArticle;
import cube.aigc.spi.AIGCActionTask;
import cube.aigc.spi.ActionContext;
import cube.auth.AuthToken;
import cube.common.state.AIGCStateCode;
import cube.service.psychology.scene.PsychologyScene;

/**
 * 读取模板文章动作。
 *
 * <p>对应线协议动作 {@code getPsychologyTemplateArticle}，逐字符等同于既有枚举
 * {@code AIGCAction.GetPsychologyTemplateArticle} 的 {@code name} 字段。</p>
 *
 * <p><b>状态码序列</b>（逐字复刻宿主原任务类）：
 * {@code NoToken → NoData / Ok / IllegalOperation}。</p>
 *
 * <p><b>为何 requiresToken 取 false</b>：宿主原任务类对「无 token 参数」与
 * 「令牌解析不出」<b>都回 NoToken</b>；若交由骨架前置校验，后者会变成
 * {@code InconsistentToken}，属线协议可见变更。故保留 {@code false}。</p>
 */
public final class GetPsychologyTemplateArticleAction implements AIGCActionTask {

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
            // ⚠️ 与「无 token 参数」同回 NoToken（宿主原行为），勿改成 InconsistentToken
            ctx.respondEmpty(AIGCStateCode.NoToken);
            return AIGCStateCode.NoToken;
        }

        try {
            long sn = ctx.getRequest().data.getLong("sn");
            ReportArticle article = PsychologyScene.getInstance().getPaintingTemplateArticle(sn);
            if (null == article) {
                ctx.respondEmpty(AIGCStateCode.NoData);
                return AIGCStateCode.NoData;
            }

            ctx.respond(AIGCStateCode.Ok, article.toJSON());
            return AIGCStateCode.Ok;
        } catch (Exception e) {
            Logger.e(this.getClass(), "#handle", e);
            ctx.respondEmpty(AIGCStateCode.IllegalOperation);
            return AIGCStateCode.IllegalOperation;
        }
    }
}
