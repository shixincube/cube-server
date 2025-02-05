/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.task;

import cell.core.cellet.Cellet;
import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cube.aigc.psychology.composition.ReportRelation;
import cube.auth.AuthToken;
import cube.benchmark.ResponseTime;
import cube.common.Packet;
import cube.common.entity.AIGCChannel;
import cube.common.entity.FileLabel;
import cube.common.entity.GeneratingRecord;
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
        List<ReportRelation> reportRelationList = null;
        GeneratingRecord conversationContext = null;
        String query = null;
        try {
            channelCode = packet.data.getString("channelCode");

            if (packet.data.has("relations")) {
                JSONArray array = packet.data.getJSONArray("relations");
                reportRelationList = new ArrayList<>();
                for (int i = 0; i < array.length(); ++i) {
                    reportRelationList.add(new ReportRelation(array.getJSONObject(i)));
                }
            }

            if (packet.data.has("context")) {
                JSONObject ctxJson = packet.data.getJSONObject("context");
                if (ctxJson.has("fileCode")) {
                    AuthToken authToken = service.getToken(token);
                    FileLabel fileLabel = service.getFile(authToken.getDomain(), ctxJson.getString("fileCode"));
                    if (null != fileLabel) {
                        List<FileLabel> files = new ArrayList<>();
                        files.add(fileLabel);
                        conversationContext = new GeneratingRecord(files);
                    }
                }
                else {
                    conversationContext = new GeneratingRecord(packet.data.getJSONObject("context"));
                }
            }

            query = packet.data.getString("query");
        } catch (Exception e) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.InvalidParameter.code, new JSONObject()));
            markResponseTime();
            return;
        }

        ConversationWorker worker = new ConversationWorker(service);
        AIGCStateCode stateCode = AIGCStateCode.Failure;

        if (null != reportRelationList) {
            stateCode = worker.work(token, channelCode, reportRelationList, query, new GenerateTextListener() {
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
            if (null == conversationContext) {
                conversationContext = new GeneratingRecord(query);
            }
            else {
                conversationContext.query = query;
            }

            // 获取频道
            AIGCChannel channel = service.getChannel(channelCode);
            if (null == channel) {
                channel = service.createChannel(token, channelCode, channelCode);
            }

            stateCode = worker.work(token, channel, conversationContext, new GenerateTextListener() {
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
