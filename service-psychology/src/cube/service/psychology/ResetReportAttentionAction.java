/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.psychology;

import cell.core.talk.dialect.ActionDialect;
import cell.util.log.Logger;
import cube.aigc.spi.AIGCActionTask;
import cube.aigc.spi.ActionContext;
import cube.common.state.AIGCStateCode;
import org.json.JSONObject;

/**
 * 重置报告关注等级动作。
 *
 * <p>对应线协议动作 {@code resetReportAttention}，逐字符等同于既有枚举
 * {@code AIGCAction.ResetReportAttention} 的 {@code name} 字段。</p>
 *
 * <p><b>状态码序列与迁移前逐项一致</b>：
 * {@code NoToken → InvalidParameter → Ok / Failure → IllegalOperation}。</p>
 *
 * <p><b>⚠️ 本动作有两处与同批动作不同，绝不可「顺手统一」</b>：</p>
 * <ol>
 *   <li><b>无令牌有效性校验</b>：迁移前只判方言里有没有 {@code token} 参数，
 *       <b>不</b>调 {@code getToken} 校验。因此即使 {@code requiresToken=false}，
 *       本处理器也<b>不能</b>加有效性判定——加了会让原本能通过的无效令牌被拦下，
 *       属线协议可见变更；</li>
 *   <li><b>catch 分支回 {@code IllegalOperation}</b>：同批另两个动作
 *       参数异常回 {@code InvalidParameter}，而本动作
 *       （迁移前 {@code ResetReportAttentionTask} L67-72）回
 *       {@code IllegalOperation}。这是全批 9 个动作里唯一一个，
 *       保持原样以确保异常场景下客户端行为不变。</li>
 * </ol>
 *
 * <p>此外本动作的 {@code Failure} 分支回<b>空对象</b>而非回显请求体
 * （迁移前 L58-59），与同批另两个动作亦不同。</p>
 *
 * <p><b>为何 requiresToken 取 false</b>：同
 * {@link StopGeneratingReportAction}，避免宿主骨架把「无令牌」的应答码
 * 从 {@code NoToken} 改成 {@code InvalidParameter}。</p>
 */
public final class ResetReportAttentionAction implements AIGCActionTask {

    @Override
    public AIGCStateCode handle(ActionContext ctx) {
        ActionDialect dialect = ctx.getDialect();

        String tokenCode = dialect.containsParam("token") ? dialect.getParamAsString("token") : null;
        if (null == tokenCode) {
            ctx.respondEmpty(AIGCStateCode.NoToken);
            return AIGCStateCode.NoToken;
        }

        // 刻意不校验令牌有效性：迁移前本动作只判令牌存在性（L37-43），
        // 之后直接进入参数判断。若此处补 resolveToken 判定，
        // 无效令牌会从「继续执行」变成「被拦截」，属线协议可见变更

        JSONObject data = ctx.getRequest().data;

        // 迁移前用 has("sn") 而非 getLong：缺 sn 时走 InvalidParameter（L45-50），
        // 而 sn 类型非法（如字符串）时抛 JSONException 落入 catch 回 IllegalOperation。
        // 两种异常的应答码不同，故此处不能用统一的 try 包住 sn 读取
        if (!data.has("sn")) {
            ctx.respondEmpty(AIGCStateCode.InvalidParameter);
            return AIGCStateCode.InvalidParameter;
        }

        try {
            long sn = data.getLong("sn");

            // 缺 attention 时传 null：宿主侧据此回滚到滚动建议而非置为指定等级
            Integer attention = data.has("attention") ? data.getInt("attention") : null;

            JSONObject report = ((PsychologyModule) ctx.getModule()).resetReportAttention(ctx.getHost(), sn, attention);

            if (null == report) {
                // 迁移前此处回空对象，不是回显请求体
                ctx.respondEmpty(AIGCStateCode.Failure);
                return AIGCStateCode.Failure;
            }

            ctx.respond(AIGCStateCode.Ok, report);
            return AIGCStateCode.Ok;
        } catch (Exception e) {
            // ⚠️ 迁移前 catch 回 IllegalOperation（L67-72），同批另两个动作是 InvalidParameter
            Logger.e(this.getClass(), "#handle - Can NOT reset attention", e);
            ctx.respondEmpty(AIGCStateCode.IllegalOperation);
            return AIGCStateCode.IllegalOperation;
        }
    }
}
