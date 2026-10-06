/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.psychology.action;

import cell.core.talk.dialect.ActionDialect;
import cube.aigc.listener.VoiceStreamAnalysisListener;
import cube.aigc.spi.AIGCActionTask;
import cube.aigc.spi.ActionContext;
import cube.auth.AuthToken;
import cube.common.entity.FileLabel;
import cube.common.entity.VoiceStreamSink;
import cube.common.state.AIGCStateCode;
import cube.service.psychology.scene.VoiceStreamService;

/**
 * 分析语音流动作。
 *
 * <p>对应线协议动作 {@code analyseVoiceStream}，逐字符等同于既有枚举
 * {@code AIGCAction.AnalyseVoiceStream} 的 {@code name} 字段。</p>
 *
 * <p><b>状态码序列</b>：{@code InvalidParameter → Ok / Failure}。
 * 注意本动作<b>可能两次应答</b>：受理失败时立刻回 {@code Failure}；
 * 受理成功则先不答，待说话人分离完成后由回调回 {@code Ok} 或失败码。
 * 这与既有宿主任务类的行为一致（它在分离未受理时立即回 {@code Failure}，
 * 受理后则在监听器里 speak）。</p>
 *
 * <p><b>为何 requiresToken 取 false</b>：宿主原任务类只校验「方言里有没有
 * token 参数」，没有校验令牌有效性——无效令牌会一路走到分离环节。
 * 若交由骨架前置校验，无效令牌会变成 {@code InconsistentToken}，
 * 属线协议可见变更，故保留 {@code false}。</p>
 */
public final class AnalyseVoiceStreamAction implements AIGCActionTask {

    @Override
    public AIGCStateCode handle(ActionContext ctx) {
        ActionDialect dialect = ctx.getDialect();

        String tokenCode = this.getTokenCode(dialect);
        if (null == tokenCode) {
            ctx.respond(AIGCStateCode.InvalidParameter, ctx.getRequest().data);
            return AIGCStateCode.InvalidParameter;
        }

        String fileCode;
        String streamName;
        int index;
        try {
            // 刻意用 ctx.getRequest().data 而非 ctx.getParams()：
            // 后者在无 data 时返回空对象，会把「参数缺失」变成「index=0 的合法请求」
            if (!ctx.getRequest().data.has("fileCode")
                    || !ctx.getRequest().data.has("streamName")
                    || !ctx.getRequest().data.has("index")) {
                ctx.respond(AIGCStateCode.InvalidParameter, ctx.getRequest().data);
                return AIGCStateCode.InvalidParameter;
            }

            fileCode = ctx.getRequest().data.getString("fileCode");
            streamName = ctx.getRequest().data.getString("streamName");
            index = ctx.getRequest().data.getInt("index");
        } catch (Exception e) {
            ctx.respond(AIGCStateCode.InvalidParameter, ctx.getRequest().data);
            return AIGCStateCode.InvalidParameter;
        }

        final ActionContext fctx = ctx;
        final org.json.JSONObject requestData = ctx.getRequest().data;

        AuthToken authToken = ctx.getHost().resolveToken(tokenCode);
        boolean accepted = VoiceStreamService.getInstance().analyse(authToken, fileCode, streamName, index,
                new VoiceStreamAnalysisListener() {
                    @Override
                    public void onCompleted(FileLabel source, VoiceStreamSink streamSink) {
                        fctx.respond(AIGCStateCode.Ok, streamSink.toJSON());
                    }

                    @Override
                    public void onFailed(FileLabel source, AIGCStateCode stateCode) {
                        fctx.respond(stateCode, requestData);
                    }
                });

        if (!accepted) {
            // 受理失败（流已超时或分离单元不可用）：立刻应答，
            // 此后回调不会再触发
            ctx.respond(AIGCStateCode.Failure, ctx.getRequest().data);
        }

        return accepted ? AIGCStateCode.Ok : AIGCStateCode.Failure;
    }

    /**
     * 取令牌码。
     *
     * @param dialect 请求方言。
     * @return 返回令牌码；方言未带 token 参数时返回 {@code null}。
     */
    private String getTokenCode(ActionDialect dialect) {
        return dialect.containsParam("token") ? dialect.getParamAsString("token") : null;
    }
}
