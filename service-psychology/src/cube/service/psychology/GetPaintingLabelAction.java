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

import java.util.List;

/**
 * 读取绘画标签动作。
 *
 * <p>对应线协议动作 {@code getPaintingLabel}，逐字符等同于既有枚举
 * {@code AIGCAction.GetPaintingLabel} 的 {@code name} 字段。</p>
 *
 * <p><b>状态码序列与迁移前逐项一致</b>：
 * {@code NoToken → InvalidParameter → Ok}。迁移前本动作<b>没有</b>
 * {@code Failure} 分支——若存储层抛出异常，异常会穿透
 * {@code ServiceTask#run} 逃逸到线程池，最终没有应答、调用方超时；
 * 迁移后宿主动作骨架会兜底补一次 {@code Failure} 应答。这属于行为改善而非回归，
 * 但确实与迁移前不同，因此状态码白名单中仍列入 {@code Failure}。</p>
 *
 * <p><b>为何 requiresToken 取 false</b>：同
 * {@link SetPaintingReportStateAction}，迁移前只判令牌存在性，
 * 以保住 {@code NoToken} 应答码并避免新增 {@code InconsistentToken} 拦截。</p>
 */
public final class GetPaintingLabelAction implements AIGCActionTask {

    @Override
    public AIGCStateCode handle(ActionContext ctx) {
        ActionDialect dialect = ctx.getDialect();

        String tokenCode = dialect.containsParam("token") ? dialect.getParamAsString("token") : null;
        if (null == tokenCode) {
            ctx.respondEmpty(AIGCStateCode.NoToken);
            return AIGCStateCode.NoToken;
        }

        long sn = 0;
        try {
            sn = ctx.getParams().getLong("sn");
        } catch (Exception e) {
            ctx.respondEmpty(AIGCStateCode.InvalidParameter);
            return AIGCStateCode.InvalidParameter;
        }

        List<PaintingLabel> labels = ((PsychologyModule) ctx.getModule()).readPaintingLabels(sn);

        JSONArray array = new JSONArray();
        for (PaintingLabel label : labels) {
            array.put(label.toJSON());
        }

        JSONObject responseData = new JSONObject();
        responseData.put("sn", sn);
        responseData.put("labels", array);
        ctx.respond(AIGCStateCode.Ok, responseData);

        return AIGCStateCode.Ok;
    }
}
