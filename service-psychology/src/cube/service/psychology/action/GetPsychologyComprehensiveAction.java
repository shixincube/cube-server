/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.psychology.action;

import cell.core.talk.dialect.ActionDialect;
import cube.aigc.psychology.ComprehensiveReport;
import cube.aigc.spi.AIGCActionTask;
import cube.aigc.spi.ActionContext;
import cube.auth.AuthToken;
import cube.common.state.AIGCStateCode;
import cube.service.psychology.scene.PsychologyScene;

/**
 * 读取心理融合报告动作。
 *
 * <p>对应线协议动作 {@code queryPsychologyComprehensive}，逐字符等同于既有枚举
 * {@code AIGCAction.QueryPsychologyComprehensive} 的 {@code name} 字段。</p>
 *
 * <p><b>状态码序列</b>（逐字复刻宿主原任务类）：
 * {@code NoToken → IllegalOperation → NoData / Ok / InvalidParameter}。
 * 其中「令牌解析不出」回的是 {@code IllegalOperation}（不是 NoToken，
 * 也不是 InconsistentToken），异常兜底回 {@code InvalidParameter}
 * （不是 IllegalOperation）——两处都勿「顺手统一」。</p>
 *
 * <p><b>sn 缺省为 0</b>：宿主原实现读 {@code has("sn") ? getLong : 0}，
 * 缺 sn 时按 0 查库（必然查不到，回 {@code NoData}），
 * 而非回 {@code InvalidParameter}。</p>
 */
public final class GetPsychologyComprehensiveAction implements AIGCActionTask {

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
            // ⚠️ 此处回 IllegalOperation（宿主原行为）
            ctx.respondEmpty(AIGCStateCode.IllegalOperation);
            return AIGCStateCode.IllegalOperation;
        }

        try {
            long sn = ctx.getRequest().data.has("sn") ? ctx.getRequest().data.getLong("sn") : 0;
            ComprehensiveReport report = PsychologyScene.getInstance().getComprehensiveReport(sn);
            if (null == report) {
                ctx.respondEmpty(AIGCStateCode.NoData);
                return AIGCStateCode.NoData;
            }

            ctx.respond(AIGCStateCode.Ok, report.toJSON());
            return AIGCStateCode.Ok;
        } catch (Exception e) {
            // ⚠️ 此处回 InvalidParameter（宿主原行为，与上面两处都不同）
            ctx.respondEmpty(AIGCStateCode.InvalidParameter);
            return AIGCStateCode.InvalidParameter;
        }
    }
}
