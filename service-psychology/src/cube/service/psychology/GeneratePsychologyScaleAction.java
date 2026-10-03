/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.psychology;

import cell.core.talk.dialect.ActionDialect;
import cube.aigc.psychology.Attribute;
import cube.aigc.psychology.composition.Scale;
import cube.aigc.spi.AIGCActionTask;
import cube.aigc.spi.ActionContext;
import cube.auth.AuthToken;
import cube.common.Language;
import cube.common.state.AIGCStateCode;
import org.json.JSONObject;

/**
 * 生成量表动作。
 *
 * <p>对应线协议动作 {@code generatePsychologyScale}，逐字符等同于既有枚举
 * {@code AIGCAction.GeneratePsychologyScale} 的 {@code name} 字段。</p>
 *
 * <p><b>状态码序列与迁移前逐项一致</b>：
 * {@code NoToken → IllegalOperation → Ok / Failure / InvalidParameter}。</p>
 *
 * <p><b>参数默认值逐字复刻迁移前</b>：{@code name} 必取（缺失抛异常落入
 * {@code catch}）；{@code language} 缺省为 {@link Language#Chinese}；
 * {@code role} 缺省为空串；{@code strict} 缺省为 {@code false}。
 * 注意 {@code gender} 与 {@code age} 在迁移前是<b>无默认值</b>的必取项——
 * {@code packet.data.getString("gender")} 在字段缺失时抛 JSONException。</p>
 *
 * <p><b>为何 requiresToken 取 false</b>：同
 * {@link ListPsychologyScalesAction}。</p>
 */
public final class GeneratePsychologyScaleAction implements AIGCActionTask {

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
            String scaleName = requestData.getString("name");
            Language language = requestData.has("language") ?
                    Language.parse(requestData.getString("language")) : Language.Chinese;

            Attribute attribute = new Attribute(requestData.getString("gender"),
                    requestData.getInt("age"),
                    requestData.has("role") ? requestData.getString("role") : "",
                    language,
                    requestData.has("strict") && requestData.getBoolean("strict"));

            Scale scale = ((PsychologyModule) ctx.getModule())
                    .generateScale(authToken.getContactId(), scaleName, attribute);

            if (null == scale) {
                // 迁移前此处回显原始请求体，不是空对象
                ctx.respond(AIGCStateCode.Failure, requestData);
                return AIGCStateCode.Failure;
            }

            JSONObject scaleJson = scale.toJSON();
            scaleJson.remove("scoringScript");
            scaleJson.remove("result");

            ctx.respond(AIGCStateCode.Ok, scaleJson);
            return AIGCStateCode.Ok;
        } catch (Exception e) {
            ctx.respondEmpty(AIGCStateCode.InvalidParameter);
            return AIGCStateCode.InvalidParameter;
        }
    }
}
