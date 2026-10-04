/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.psychology;

import cell.core.talk.dialect.ActionDialect;
import cube.aigc.psychology.PaintingReport;
import cube.aigc.spi.AIGCActionTask;
import cube.aigc.spi.ActionContext;
import cube.common.state.AIGCStateCode;
import org.json.JSONObject;

/**
 * 修改报告备注动作。
 *
 * <p>对应线协议动作 {@code modifyReportRemark}，逐字符等同于既有枚举
 * {@code AIGCAction.ModifyReportRemark} 的 {@code name} 字段。</p>
 *
 * <p><b>状态码序列</b>：
 * {@code NoToken → IllegalOperation → Ok / Failure → InvalidParameter}。</p>
 *
 * <p>本动作可在插件内<b>完全自建</b>，无需经 SPI：其逻辑仅为
 * 「更新备注 → 回读报告 → 附加备注」，两步都已在本模块的
 * {@link PsychologyStorage} 内。这与报告读取类动作不同——那些动作需要
 * 读取尚在宿主内存态中的运行中报告。</p>
 *
 * <p>⚠️ 但回读报告会触发 {@code makeReport}，其中包含六维得分描述的生成；
 * 该描述依赖宿主分词器与 TF-IDF 语料，需由 {@link PsychologyModule#setup}
 * 注入 {@code hexagonDescriber}，否则描述恒为空（与
 * 「分词器为 null 时抛空指针被 catch 吞掉」的结果一致）。</p>
 *
 * <p><b>为何 requiresToken 取 false</b>：令牌无效回的是
 * {@code IllegalOperation}，而宿主动作骨架在 {@code requiresToken=true} 时
 * 会改回 {@code InconsistentToken}——这是线协议可见的语义变更。</p>
 */
public final class ModifyReportRemarkAction implements AIGCActionTask {

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

        try {
            // 刻意不判 data 是否为 null：packet.data 为 null 时
            // getLong/getString 抛 NPE 并落入 catch 回 InvalidParameter
            JSONObject data = ctx.getRequest().data;
            long reportSn = data.getLong("sn");
            String remark = data.getString("remark");

            PaintingReport report = ((PsychologyModule) ctx.getModule()).modifyReportRemark(reportSn, remark);

            if (null == report) {
                // 此处回空对象，不是回显请求体
                ctx.respondEmpty(AIGCStateCode.Failure);
                return AIGCStateCode.Failure;
            }

            ctx.respond(AIGCStateCode.Ok, report.toCompactJSON());
            return AIGCStateCode.Ok;
        } catch (Exception e) {
            ctx.respondEmpty(AIGCStateCode.InvalidParameter);
            return AIGCStateCode.InvalidParameter;
        }
    }
}
