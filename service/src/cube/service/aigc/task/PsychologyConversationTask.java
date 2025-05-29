/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.task;

import cell.core.cellet.Cellet;
import cell.core.net.Endpoint;
import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.util.log.Logger;
import cube.aigc.psychology.composition.ConversationRelation;
import cube.auth.AuthToken;
import cube.benchmark.ResponseTime;
import cube.common.Packet;
import cube.common.entity.*;
import cube.common.state.AIGCStateCode;
import cube.service.ServiceTask;
import cube.service.aigc.AIGCCellet;
import cube.service.aigc.AIGCService;
import cube.service.aigc.listener.GenerateTextListener;
import cube.service.aigc.scene.ConversationWorker;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 心理学相关的对话任务。
 */
public class PsychologyConversationTask extends ServiceTask {

    public PsychologyConversationTask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
        super(cellet, talkContext, primitive, responseTime);
    }

    @Override
    public void run() {
        ActionDialect dialect = new ActionDialect(this.primitive);
        Packet packet = new Packet(dialect);

        String token = getTokenCode(dialect);
        if (null == token) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.NoToken.code, new JSONObject()));
            markResponseTime();
            return;
        }

        AIGCService service = ((AIGCCellet) this.cellet).getService();

        String channelCode = null;
        Endpoint httpEndpoint = null;
        Endpoint httpsEndpoint = null;
        List<ConversationRelation> conversationRelationList = null;
        ConversationRelation relation = null;
        ComplexContext context = null;
        String query = null;
        try {
            channelCode = packet.data.getString("channelCode");

            if (packet.data.has("endpoint")) {
                httpEndpoint = new Endpoint(packet.data.getJSONObject("endpoint").getJSONObject("http"));
                httpsEndpoint = new Endpoint(packet.data.getJSONObject("endpoint").getJSONObject("https"));
            }

            if (packet.data.has("relations")) {
                JSONArray array = packet.data.getJSONArray("relations");
                conversationRelationList = new ArrayList<>();
                for (int i = 0; i < array.length(); ++i) {
                    conversationRelationList.add(new ConversationRelation(array.getJSONObject(i)));
                }
            }

            if (packet.data.has("relation")) {
                relation = new ConversationRelation(packet.data.getJSONObject("relation"));
            }

            if (packet.data.has("context")) {
                JSONObject ctxJson = packet.data.getJSONObject("context");
                if (ctxJson.has("fileCode")) {
                    AuthToken authToken = service.getToken(token);
                    FileLabel fileLabel = service.getFile(authToken.getDomain(), ctxJson.getString("fileCode"));
                    if (null != fileLabel) {
                        context = new ComplexContext();
                        context.addResource(new FileResource(fileLabel));
                    }
                }
                else {
                    context = new ComplexContext(packet.data.getJSONObject("context"));
                }
            }

            query = packet.data.getString("query");
        } catch (Exception e) {
            Logger.w(this.getClass(), "#run - " + packet.data.toString(4), e);
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.InvalidParameter.code, new JSONObject()));
            markResponseTime();
            return;
        }

        ConversationWorker worker = new ConversationWorker(service);
        AIGCStateCode stateCode = AIGCStateCode.Failure;

        if (null != conversationRelationList) {
            stateCode = worker.work(token, channelCode, conversationRelationList, query, new GenerateTextListener() {
                @Override
                public void onGenerated(AIGCChannel channel, GeneratingRecord record) {
                    if (null != record) {
                        cellet.speak(talkContext,
                                makeResponse(dialect, packet, AIGCStateCode.Ok.code,
                                        record.toTraceJSON()));
                    }
                    else {
                        cellet.speak(talkContext,
                                makeResponse(dialect, packet, AIGCStateCode.Failure.code, packet.data));
                    }
                    markResponseTime();
                }

                @Override
                public void onFailed(AIGCChannel channel, AIGCStateCode stateCode) {
                    cellet.speak(talkContext,
                            makeResponse(dialect, packet, AIGCStateCode.Failure.code, packet.data));
                    markResponseTime();
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

            // 获取频道
            AIGCChannel channel = service.getChannel(channelCode);
            if (null == channel) {
                channel = service.createChannel(token, channelCode, channelCode);
            }
            channel.setEndpoint(httpEndpoint, httpsEndpoint);

            stateCode = worker.work(channel, query, context, relation, new GenerateTextListener() {
                @Override
                public void onGenerated(AIGCChannel channel, GeneratingRecord record) {
                    if (null != record) {
                        cellet.speak(talkContext,
                                makeResponse(dialect, packet, AIGCStateCode.Ok.code,
                                        record.toTraceJSON()));
                    }
                    else {
                        cellet.speak(talkContext,
                                makeResponse(dialect, packet, AIGCStateCode.Failure.code, packet.data));
                    }
                    markResponseTime();
                }

                @Override
                public void onFailed(AIGCChannel channel, AIGCStateCode stateCode) {
                    cellet.speak(talkContext,
                            makeResponse(dialect, packet, stateCode.code, packet.data));
                    markResponseTime();
                }
            });
        }

        if (AIGCStateCode.Ok != stateCode) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, stateCode.code, new JSONObject()));
            markResponseTime();
        }
    }
}
