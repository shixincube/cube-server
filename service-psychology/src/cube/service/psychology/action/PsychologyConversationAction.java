/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.psychology.action;

import cell.core.net.Endpoint;
import cell.core.talk.dialect.ActionDialect;
import cell.util.log.Logger;
import cube.aigc.listener.GenerateTextListener;
import cube.aigc.psychology.composition.ConversationRelation;
import cube.aigc.spi.AIGCActionTask;
import cube.aigc.spi.ActionContext;
import cube.auth.AuthToken;
import cube.common.Language;
import cube.common.entity.AIGCChannel;
import cube.common.entity.ComplexContext;
import cube.common.entity.FileLabel;
import cube.common.entity.FileResource;
import cube.common.entity.GeneratingRecord;
import cube.common.state.AIGCStateCode;
import cube.service.psychology.scene.ConversationWorker;
import cube.util.TextUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 心理学对话动作。
 *
 * <p>对应线协议动作 {@code psychologyConversation}，逐字符等同于既有枚举
 * {@code AIGCAction.PsychologyConversation} 的 {@code name} 字段。</p>
 *
 * <p><b>本动作是流式的</b>（{@link cube.aigc.spi.ActionBinding#streaming} 为
 * {@code true}）：它对同一个请求可能 speak 两次 —— 先回受理结果，
 * 模型产出后再回一段流式内容。这与 {@code ActionRunner} 契约的默认
 * 「单次应答 + 幂等去重 + 返回时补空应答」都相悖，故必须显式声明流式。</p>
 *
 * <p><b>状态码序列</b>（逐字复刻宿主原任务类）：
 * {@code NoToken → InvalidParameter / Ok / Failure}。
 * 参数解析失败回 {@code InvalidParameter}（含端点、关系、上下文构造失败）。</p>
 *
 * <p>两条入参形态：{@code relations}（爱心理平台的旧形态，按关系列表走）
 * 与 {@code relation} + {@code context}（按单关系走）。</p>
 */
public final class PsychologyConversationAction implements AIGCActionTask {

    @Override
    public AIGCStateCode handle(ActionContext ctx) {
        ActionDialect dialect = ctx.getDialect();

        String tokenCode = dialect.containsParam("token") ? dialect.getParamAsString("token") : null;
        if (null == tokenCode) {
            ctx.respondEmpty(AIGCStateCode.NoToken);
            return AIGCStateCode.NoToken;
        }

        String channelCode = null;
        Endpoint httpEndpoint = null;
        Endpoint httpsEndpoint = null;
        List<ConversationRelation> conversationRelationList = null;
        ConversationRelation relation = null;
        ComplexContext context = null;
        String query = null;
        try {
            channelCode = ctx.getRequest().data.getString("channelCode");

            if (ctx.getRequest().data.has("endpoint")) {
                httpEndpoint = new Endpoint(
                        ctx.getRequest().data.getJSONObject("endpoint").getJSONObject("http"));
                httpsEndpoint = new Endpoint(
                        ctx.getRequest().data.getJSONObject("endpoint").getJSONObject("https"));
            }

            if (ctx.getRequest().data.has("relations")) {
                // 兼容爱心理平台
                JSONArray array = ctx.getRequest().data.getJSONArray("relations");
                conversationRelationList = new ArrayList<>();
                for (int i = 0; i < array.length(); ++i) {
                    conversationRelationList.add(new ConversationRelation(array.getJSONObject(i)));
                }
            }

            if (ctx.getRequest().data.has("relation")) {
                relation = new ConversationRelation(ctx.getRequest().data.getJSONObject("relation"));
            }

            if (ctx.getRequest().data.has("context")) {
                JSONObject ctxJson = ctx.getRequest().data.getJSONObject("context");
                if (ctxJson.has("fileCode")) {
                    AuthToken authToken = ctx.getHost().resolveToken(tokenCode);
                    FileLabel fileLabel = ctx.getHost().getFile(
                            authToken.getDomain(), ctxJson.getString("fileCode"));
                    if (null != fileLabel) {
                        context = new ComplexContext();
                        context.addResource(new FileResource(fileLabel));
                    }
                }
                else {
                    context = new ComplexContext(ctxJson);
                }
            }

            query = ctx.getRequest().data.getString("query");
        } catch (Exception e) {
            Logger.w(this.getClass(), "#handle - " + ctx.getRequest().data.toString(4), e);
            ctx.respondEmpty(AIGCStateCode.InvalidParameter);
            return AIGCStateCode.InvalidParameter;
        }

        final ActionContext fctx = ctx;
        final JSONObject requestData = ctx.getRequest().data;
        final String token = tokenCode;

        ConversationWorker worker = new ConversationWorker(ctx.getHost());
        AIGCStateCode stateCode = AIGCStateCode.Failure;

        if (null != conversationRelationList && !conversationRelationList.isEmpty()) {
            stateCode = worker.work(token, channelCode, conversationRelationList, query,
                    new GenerateTextListener() {
                        @Override
                        public void onGenerated(AIGCChannel channel, GeneratingRecord record) {
                            if (null != record) {
                                fctx.respond(AIGCStateCode.Ok, record.toTraceJSON());
                            }
                            else {
                                fctx.respond(AIGCStateCode.Failure, requestData);
                            }
                        }

                        @Override
                        public void onFailed(AIGCChannel channel, AIGCStateCode stateCode) {
                            fctx.respond(AIGCStateCode.Failure, requestData);
                        }
                    });
        }
        else {
            if (null == context) {
                context = new ComplexContext();
            }

            if (null == relation) {
                relation = new ConversationRelation();
            }

            // 取频道：没有则按 query 的语言创建
            AIGCChannel channel = ctx.getHost().getChannel(channelCode);
            if (null == channel) {
                boolean english = TextUtils.isTextMainlyInEnglish(query);
                // 按令牌码建频道（宿主原实现走的是 service.createChannel(tokenCode, ...)）
                channel = ctx.getHost().createChannelByCode(token, channelCode, channelCode,
                        english ? Language.English : Language.Chinese);
            }
            channel.setEndpoint(httpEndpoint, httpsEndpoint);

            final AIGCChannel fchannel = channel;
            final ComplexContext fcontext = context;
            final ConversationRelation frelation = relation;
            final String fquery = query;

            stateCode = worker.work(fchannel, fquery, fcontext, frelation, new GenerateTextListener() {
                @Override
                public void onGenerated(AIGCChannel channel, GeneratingRecord record) {
                    if (null != record) {
                        fctx.respond(AIGCStateCode.Ok, record.toTraceJSON());
                    }
                    else {
                        fctx.respond(AIGCStateCode.Failure, requestData);
                    }
                }

                @Override
                public void onFailed(AIGCChannel channel, AIGCStateCode stateCode) {
                    fctx.respond(stateCode, requestData);
                }
            });
        }

        // ⚠️ 流式动作不在此补应答：处理器返回时产出可能仍在途，
        //    提前回一个空应答会让客户端把后续片段当成 unsolicited。
        //    但「连受理都失败」时必须应答，否则调用方无限等待。
        // isResponded() 在流式模式下不充当闸门（多次应答都放行），
        // 但仍如实反映「至少应答过一次」，用它避免重复补受理应答
        if (AIGCStateCode.Ok != stateCode && !ctx.isResponded()) {
            ctx.respondEmpty(stateCode);
        }

        return stateCode;
    }
}
