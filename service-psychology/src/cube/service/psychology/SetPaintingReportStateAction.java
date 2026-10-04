/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.psychology;

import cell.core.talk.dialect.ActionDialect;
import cube.aigc.spi.AIGCActionTask;
import cube.aigc.spi.ActionContext;
import cube.common.state.AIGCStateCode;
import org.json.JSONObject;

/**
 * 设置绘画报告状态动作。
 *
 * <p>对应线协议动作 {@code setPaintingReportState}，逐字符等同于既有枚举
 * {@code AIGCAction.SetPaintingReportState} 的 {@code name} 字段。</p>
 *
 * <p><b>状态码序列</b>：
 * {@code NoToken → InvalidParameter → Failure → Ok}。</p>
 *
 * <p><b>为何 requiresToken 取 false</b>：
 * {@code SetPaintingReportStateTask} 只用 {@code getTokenCode(dialect) == null}
 * 判定「无令牌」并以 {@code NoToken} 应答，<b>不调用 extractAuthToken</b>，
 * 因此不校验令牌有效性。若交由宿主以 {@code requiresToken=true} 前置校验，
 * 「无令牌」会被改答 {@code InvalidParameter}（状态码由 22 变为 1），
 * 且无效令牌会被新增的 {@code InconsistentToken} 拦截——两者都是线协议可见的
 * 语义变更。本处理器改为自行复刻令牌「存在性」判定，保持应答逐字节一致。</p>
 */
public final class SetPaintingReportStateAction implements AIGCActionTask {

    @Override
    public AIGCStateCode handle(ActionContext ctx) {
        ActionDialect dialect = ctx.getDialect();

        // 等价于 Task#getTokenCode(ActionDialect)：该方法在 common 的 Task 上，
        // 需要 Cellet/Nucleus，插件无法继承，故此处复刻其三行逻辑
        String tokenCode = dialect.containsParam("token") ? dialect.getParamAsString("token") : null;
        if (null == tokenCode) {
            ctx.respondEmpty(AIGCStateCode.NoToken);
            return AIGCStateCode.NoToken;
        }

        long sn = 0;
        int state = 0;
        try {
            JSONObject params = ctx.getParams();
            sn = params.getLong("sn");
            state = params.getInt("state");
        } catch (Exception e) {
            ctx.respondEmpty(AIGCStateCode.InvalidParameter);
            return AIGCStateCode.InvalidParameter;
        }

        boolean written = ((PsychologyModule) ctx.getModule()).writePaintingReportState(sn, state);
        if (!written) {
            ctx.respondEmpty(AIGCStateCode.Failure);
            return AIGCStateCode.Failure;
        }

        JSONObject responseData = new JSONObject();
        responseData.put("sn", sn);
        responseData.put("state", state);
        ctx.respond(AIGCStateCode.Ok, responseData);

        return AIGCStateCode.Ok;
    }
}
