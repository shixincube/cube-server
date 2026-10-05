/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.psychology;

import cell.core.talk.dialect.ActionDialect;
import cube.aigc.psychology.composition.PaintingLabel;
import cube.aigc.spi.AIGCActionTask;
import cube.aigc.spi.ActionContext;
import cube.common.state.AIGCStateCode;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 覆写绘画标签动作。
 *
 * <p>对应线协议动作 {@code setPaintingLabel}，逐字符等同于既有枚举
 * {@code AIGCAction.SetPaintingLabel} 的 {@code name} 字段。</p>
 *
 * <p><b>状态码序列</b>：
 * {@code NoToken → InvalidParameter → Failure → Ok}。</p>
 *
 * <p><b>写入语义</b>：与场景方法完全一致——先按报告序列号删除既有标签，
 * 标签列表为空时直接返回成功，否则逐条插入。<b>该操作不是原子的</b>：
 * 删除成功而插入失败时，该报告的标签会全部丢失。此缺陷为既有，
 * 调用方需知悉此风险。</p>
 *
 * <p><b>为何 requiresToken 取 false</b>：同
 * {@link SetPaintingReportStateAction}。</p>
 */
public final class SetPaintingLabelAction implements AIGCActionTask {

    @Override
    public AIGCStateCode handle(ActionContext ctx) {
        ActionDialect dialect = ctx.getDialect();

        String tokenCode = dialect.containsParam("token") ? dialect.getParamAsString("token") : null;
        if (null == tokenCode) {
            ctx.respondEmpty(AIGCStateCode.NoToken);
            return AIGCStateCode.NoToken;
        }

        long sn = 0;
        List<PaintingLabel> labels = null;
        try {
            JSONObject params = ctx.getParams();
            sn = params.getLong("sn");

            labels = new ArrayList<>();
            JSONArray array = params.getJSONArray("labels");
            for (int i = 0; i < array.length(); ++i) {
                labels.add(new PaintingLabel(array.getJSONObject(i)));
            }
        } catch (Exception e) {
            ctx.respondEmpty(AIGCStateCode.InvalidParameter);
            return AIGCStateCode.InvalidParameter;
        }

        boolean written = ((PsychologyModule) ctx.getModule()).writePaintingLabels(sn, labels);
        if (!written) {
            ctx.respondEmpty(AIGCStateCode.Failure);
            return AIGCStateCode.Failure;
        }

        JSONObject responseData = new JSONObject();
        responseData.put("sn", sn);
        ctx.respond(AIGCStateCode.Ok, responseData);

        return AIGCStateCode.Ok;
    }
}
