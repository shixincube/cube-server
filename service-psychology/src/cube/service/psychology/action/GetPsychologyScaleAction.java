/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.psychology;

import cell.core.talk.dialect.ActionDialect;
import cell.util.log.Logger;
import cube.aigc.psychology.Resource;
import cube.aigc.psychology.composition.Scale;
import cube.aigc.spi.AIGCActionTask;
import cube.aigc.spi.ActionContext;
import cube.auth.AuthToken;
import cube.common.state.AIGCStateCode;
import org.json.JSONObject;

/**
 * 读取单个量表动作。
 *
 * <p>对应线协议动作 {@code getPsychologyScale}，逐字符等同于既有枚举
 * {@code AIGCAction.GetPsychologyScale} 的 {@code name} 字段。</p>
 *
 * <p><b>状态码序列</b>：
 * {@code NoToken → IllegalOperation → Ok / Failure / InvalidParameter}。</p>
 *
 * <p><b>两个易错点</b>：</p>
 * <ul>
 *   <li>{@code Failure} 分支回显的是<b>原始请求体</b>而非空对象，而
 *       {@code catch} 分支回空对象——两者不同，不可统一；</li>
 *   <li>读取请求参数必须用 {@code ctx.getRequest().data}：
 *       {@code packet.data} 为 {@code null} 时会抛 NPE 并落入
 *       {@code catch}（{@code InvalidParameter}）；若改用
 *       {@code ctx.getParams()}，它会返回空对象，
 *       {@code has("sn")} 变 false 从而<b>静默走进正常分支</b>，
 *       把失败变成成功。</li>
 * </ul>
 *
 * <p><b>为何 requiresToken 取 false</b>：同
 * {@link ListPsychologyScalesAction}，令牌无效回
 * {@code IllegalOperation} 而非 {@code InconsistentToken}。</p>
 */
public final class GetPsychologyScaleAction implements AIGCActionTask {

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
            ctx.respondEmpty(AIGCStateCode.IllegalOperation);
            return AIGCStateCode.IllegalOperation;
        }

        JSONObject requestData = ctx.getRequest().data;

        try {
            Scale scale = null;
            long sn = requestData.has("sn") ? requestData.getLong("sn") : 0;
            String name = requestData.has("name") ? requestData.getString("name") : null;

            if (0 != sn) {
                scale = ((PsychologyModule) ctx.getModule()).getScale(sn);
            }
            else if (null != name) {
                scale = Resource.getInstance().loadScaleByName(name, authToken.getContactId());
            }

            if (null == scale) {
                // 此处回显原始请求体，不是空对象
                ctx.respond(AIGCStateCode.Failure, requestData);
                return AIGCStateCode.Failure;
            }

            JSONObject scaleJson = scale.toJSON();
            scaleJson.remove("scoringScript");
            scaleJson.remove("result");

            ctx.respond(AIGCStateCode.Ok, scaleJson);
            return AIGCStateCode.Ok;
        } catch (Exception e) {
            Logger.w(this.getClass(), "#handle - Can NOT read scale", e);
            ctx.respondEmpty(AIGCStateCode.InvalidParameter);
            return AIGCStateCode.InvalidParameter;
        }
    }
}
