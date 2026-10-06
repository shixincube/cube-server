/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.psychology.action;

import cell.core.talk.dialect.ActionDialect;
import cube.aigc.spi.AIGCActionTask;
import cube.aigc.spi.ActionContext;
import cube.auth.AuthToken;
import cube.common.state.AIGCStateCode;
import cube.service.psychology.scene.VoiceStreamService;
import org.json.JSONObject;

import java.util.HashMap;

/**
 * 语音分析动作。
 *
 * <p>对应线协议动作 {@code speechAnalysis}，逐字符等同于既有枚举
 * {@code AIGCAction.SpeechAnalysis} 的 {@code name} 字段。</p>
 *
 * <p><b>状态码序列</b>：{@code InvalidParameter → Ok / Failure}。</p>
 *
 * <p><b>为何 requiresToken 取 false</b>：宿主原任务类只校验「方言里有没有
 * token 参数」，未校验令牌有效性。交由骨架前置校验会把无效令牌变成
 * {@code InconsistentToken}，属线协议可见变更。</p>
 */
public final class SpeechAnalysisAction implements AIGCActionTask {

    @Override
    public AIGCStateCode handle(ActionContext ctx) {
        ActionDialect dialect = ctx.getDialect();

        String tokenCode = dialect.containsParam("token") ? dialect.getParamAsString("token") : null;
        if (null == tokenCode || !ctx.getRequest().data.has("fileCode")
                || !ctx.getRequest().data.has("templateName")) {
            ctx.respond(AIGCStateCode.InvalidParameter, ctx.getRequest().data);
            return AIGCStateCode.InvalidParameter;
        }

        String fileCode = ctx.getRequest().data.getString("fileCode");
        String templateName = ctx.getRequest().data.getString("templateName");

        // parameters 为可选：存在时按「键值对全字符串化」读出，
        // 交由服务层判断该路径是否可用（当前未实现，回失败）
        HashMap<String, String> parameters = null;
        if (ctx.getRequest().data.has("parameters")) {
            parameters = new HashMap<>();
            JSONObject parameterJson = ctx.getRequest().data.getJSONObject("parameters");
            for (String key : parameterJson.keySet()) {
                parameters.put(key, parameterJson.get(key).toString());
            }
        }

        AuthToken authToken = ctx.getHost().resolveToken(tokenCode);
        String result = VoiceStreamService.getInstance().performSpeechAnalysis(
                authToken, fileCode, templateName, parameters);

        if (null != result) {
            JSONObject resultJson = new JSONObject();
            resultJson.put("fileCode", fileCode);
            resultJson.put("templateName", templateName);
            resultJson.put("result", result);
            ctx.respond(AIGCStateCode.Ok, resultJson);
            return AIGCStateCode.Ok;
        }

        ctx.respond(AIGCStateCode.Failure, ctx.getRequest().data);
        return AIGCStateCode.Failure;
    }
}
