/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.psychology;

import cube.aigc.spi.AIGCHost;
import cell.core.talk.dialect.ActionDialect;
import cube.aigc.spi.AIGCActionTask;
import cube.aigc.spi.ActionContext;
import cube.common.state.AIGCStateCode;
import org.json.JSONObject;

/**
 * 停止报告生成动作。
 *
 * <p>对应线协议动作 {@code stopGeneratingPsychologyReport}，逐字符等同于既有枚举
 * {@code AIGCAction.StopGeneratingPsychologyReport} 的 {@code name} 字段。</p>
 *
 * <p><b>状态码序列</b>：
 * {@code NoToken → IllegalOperation → InvalidParameter → Ok / Failure}。</p>
 *
 * <p><b>为何 requiresToken 取 false</b>：令牌无效时回的是
 * {@code IllegalOperation}，而宿主动作骨架在 {@code requiresToken=true} 时
 * 会改回 {@code InconsistentToken}——这是线协议可见的语义变更。
 * 故保留 {@code false}，令牌有效性由本处理器自行判定。</p>
 *
 * <p><b>为何停止逻辑走 SPI 而非插件自建</b>：报告生成期间它只存在于宿主的
 * 内存表与任务队列中，尚未入库。插件自建既无从得知「是否仍在队列中」，
 * 也无法真正中断宿主的生成线程。详见
 * {@link AIGCHostImpl#stopReportGeneration} 与对应 SPI 契约。</p>
 */
public final class StopGeneratingReportAction implements AIGCActionTask {

    @Override
    public AIGCStateCode handle(ActionContext ctx) {
        ActionDialect dialect = ctx.getDialect();

        String tokenCode = dialect.containsParam("token") ? dialect.getParamAsString("token") : null;
        if (null == tokenCode) {
            ctx.respondEmpty(AIGCStateCode.NoToken);
            return AIGCStateCode.NoToken;
        }

        if (null == ctx.getHost().resolveToken(tokenCode)) {
            ctx.respondEmpty(AIGCStateCode.IllegalOperation);
            return AIGCStateCode.IllegalOperation;
        }

        long sn = 0;
        try {
            // 刻意不判 data 是否为 null：packet.data 为 null 时此处抛 NPE
            // 并落入 catch 回 InvalidParameter；若改用 ctx.getParams()，它会返回
            // 空对象使 sn 静默为 0，把「参数缺失」变成「停止 sn=0」——语义反转
            sn = ctx.getRequest().data.getLong("sn");
        } catch (Exception e) {
            ctx.respondEmpty(AIGCStateCode.InvalidParameter);
            return AIGCStateCode.InvalidParameter;
        }

        JSONObject report = ((PsychologyModule) ctx.getModule()).stopReportGeneration(ctx.getHost(), sn);

        if (null != report) {
            ctx.respond(AIGCStateCode.Ok, report);
            return AIGCStateCode.Ok;
        }

        // 此处回显原始请求体，不是空对象
        ctx.respond(AIGCStateCode.Failure, ctx.getRequest().data);
        return AIGCStateCode.Failure;
    }
}
