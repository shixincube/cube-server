/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.psychology;

import cell.core.talk.dialect.ActionDialect;
import cell.util.log.Logger;
import cube.aigc.psychology.Painting;
import cube.aigc.spi.AIGCActionTask;
import cube.aigc.spi.AIGCHost;
import cube.aigc.spi.ActionContext;
import cube.auth.AuthToken;
import cube.common.entity.FileLabel;
import cube.common.state.AIGCStateCode;
import org.json.JSONObject;

/**
 * 读取绘画数据动作。
 *
 * <p>对应线协议动作 {@code getPsychologyPainting}，逐字符等同于既有枚举
 * {@code AIGCAction.GetPsychologyPainting} 的 {@code name} 字段。</p>
 *
 * <p><b>状态码序列</b>：
 * {@code NoToken → IllegalOperation → Ok / Failure → InvalidParameter}，
 * 其中三个 {@code Failure} 分支都<b>回显原始请求体</b>。</p>
 *
 * <p><b>三个分支的默认参数</b>（逐字复刻 L58-63）：
 * {@code sn=0}、{@code chart=false}、{@code bbox=true}、{@code vparam=false}、
 * {@code prob=0.5}、{@code fileCode=null}。其中 {@code bbox} 默认
 * <b>true</b> 而非 false。</p>
 *
 * <p>⚠️ {@code chart} 分支把 {@code authToken} 传入
 * {@code getPaintingInferenceData}，但该方法<b>从未使用它</b>。
 * 此处保持一致（传了但不用），不「顺手修正」。</p>
 *
 * <p><b>为何 requiresToken 取 false</b>：令牌无效回的是
 * {@code IllegalOperation}，而宿主动作骨架在 {@code requiresToken=true} 时
 * 会改回 {@code InconsistentToken}——这是线协议可见的语义变更。</p>
 */
public final class GetPsychologyPaintingAction implements AIGCActionTask {

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

        try {
            // 刻意不判 data 是否为 null：packet.data 为 null 时会在下方
            // 解析块抛 NPE 并落入 catch 回 InvalidParameter
            JSONObject data = ctx.getRequest().data;

            long sn = data.has("sn") ? data.getLong("sn") : 0;
            boolean chart = data.has("chart") ? data.getBoolean("chart") : false;
            boolean bbox = data.has("bbox") ? data.getBoolean("bbox") : true;
            boolean vparam = data.has("vparam") ? data.getBoolean("vparam") : false;
            double prob = data.has("prob") ? data.getDouble("prob") : 0.5d;
            String fileCode = data.has("fileCode") ? data.getString("fileCode") : null;

            AIGCHost host = ctx.getHost();

            if (chart) {
                JSONObject inferenceData = host.getPaintingInferenceData(sn);
                return this.respond(ctx, AIGCStateCode.Ok, inferenceData, AIGCStateCode.Failure);
            }

            if (null != fileCode) {
                Painting painting = host.getPredictedPainting(authToken, fileCode);
                return this.respond(ctx, AIGCStateCode.Ok,
                        (null == painting) ? null : painting.toFullJson(), AIGCStateCode.Failure);
            }

            FileLabel fileLabel = host.getPredictedPainting(authToken, sn, bbox, vparam, prob);
            return this.respond(ctx, AIGCStateCode.Ok,
                    (null == fileLabel) ? null : fileLabel.toCompactJSON(), AIGCStateCode.Failure);
        } catch (Exception e) {
            Logger.w(this.getClass(), "#handle", e);
            ctx.respondEmpty(AIGCStateCode.InvalidParameter);
            return AIGCStateCode.InvalidParameter;
        }
    }

    /**
     * 二选一应答。
     *
     * <p>三个分支的失败态都回显原始请求体（L74/88/101），
     * 此处统一处理以免漏掉。</p>
     *
     * @param ctx 动作上下文。
     * @param okCode 成功时的状态码。
     * @param data 成功时的应答数据，可为 {@code null}。
     * @param failureCode 数据为空时使用的状态码。
     * @return 返回实际使用的状态码。
     */
    private AIGCStateCode respond(ActionContext ctx, AIGCStateCode okCode, JSONObject data,
            AIGCStateCode failureCode) {
        if (null != data) {
            ctx.respond(okCode, data);
            return okCode;
        }

        // 回显原始请求体而非空对象——这是本动作区别于同批其他动作之处
        ctx.respond(failureCode, ctx.getRequest().data);
        return failureCode;
    }
}
