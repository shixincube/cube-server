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
import cell.util.log.Logger;
import cube.aigc.AppEvent;
import cube.aigc.Consts;
import cube.aigc.ModelConfig;
import cube.benchmark.ResponseTime;
import cube.common.Packet;
import cube.common.entity.*;
import cube.common.state.AIGCStateCode;
import cube.service.ServiceTask;
import cube.service.aigc.AIGCCellet;
import cube.service.aigc.AIGCService;
import cube.service.aigc.knowledge.KnowledgeBase;
import cube.service.aigc.knowledge.KnowledgeFramework;
import cube.service.aigc.listener.ConversationListener;
import cube.service.aigc.listener.KnowledgeQAListener;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 互动聊天会话。
 * @deprecated
 */
public class ConversationTask extends ServiceTask {

    public ConversationTask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
        super(cellet, talkContext, primitive, responseTime);
    }

    @Override
    public void run() {
        ActionDialect dialect = new ActionDialect(this.primitive);
        Packet packet = new Packet(dialect);

        String token = getTokenCode(dialect);

        if (null == token || !packet.data.has("content") || !packet.data.has("code")) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.InvalidParameter.code, new JSONObject()));
            markResponseTime();
            return;
        }

        String code = packet.data.getString("code");
        String content = packet.data.getString("content").trim();
        String pattern = packet.data.has("pattern") ? packet.data.getString("pattern") : Consts.PATTERN_CHAT;
        GeneratingOption option = packet.data.has("option") ?
                new GeneratingOption(packet.data.getJSONObject("option")) : new GeneratingOption();
        int histories = packet.data.has("histories") ? packet.data.getInt("histories") : 10;
        JSONArray records = packet.data.has("records") ? packet.data.getJSONArray("records") : null;
        boolean recordable = packet.data.has("recordable") && packet.data.getBoolean("recordable");
        boolean networking = packet.data.has("networking") && packet.data.getBoolean("networking");

        JSONArray categoryArray = packet.data.has("categories")
                ? packet.data.getJSONArray("categories") : new JSONArray();
        List<String> categories = new ArrayList<>();
        for (int i = 0; i < categoryArray.length(); ++i) {
            categories.add(categoryArray.getString(i));
        }

        List<GeneratingRecord> recordList = null;
        if (null != records) {
            recordList = new ArrayList<>();
            try {
                for (int i = 0; i < records.length(); ++i) {
                    GeneratingRecord record = new GeneratingRecord(records.getJSONObject(i));
                    recordList.add(record);
                }
            } catch (Exception e) {
                Logger.w(this.getClass(), "#run", e);
            }
        }

        AIGCConversationParameter parameter = new AIGCConversationParameter(option.temperature,
                option.topP, option.repetitionPenalty, option.maxNewTokens, recordList, categories, histories,
                recordable, networking);
        parameter.topK = option.topK;

        AIGCService service = ((AIGCCellet) this.cellet).getService();

        boolean success = false;

        // 根据工作模式进行调用
        if (pattern.equalsIgnoreCase(Consts.PATTERN_CHAT)) {
            // 执行 Conversation
            long sn = service.executeConversation(token, code, content, parameter, new ConversationListener() {
                @Override
                public void onConversation(AIGCChannel channel, AIGCConversationResponse response) {
                    if (Logger.isDebugLevel()) {
                        Logger.d(ConversationTask.class, "#onConversation - " + token);
                    }
                }

                @Override
                public void onFailed(AIGCChannel channel, AIGCStateCode errorCode) {
                    if (Logger.isDebugLevel()) {
                        Logger.d(ConversationTask.class, "#onFailed : " + errorCode.code + " - " + token);
                    }
                }
            });

            if (sn > 0) {
                success = true;

                JSONObject response = new JSONObject();
                response.put("sn", sn);
                response.put("processing", true);
                response.put("timestamp", System.currentTimeMillis());

                // 修正频道的 last meta sn 数据
                JSONObject channelJson = service.getChannel(code).toCompactJSON();
                channelJson.put("lastMetaSn", sn);
                response.put("channel", channelJson);
                cellet.speak(talkContext,
                        makeResponse(dialect, packet, AIGCStateCode.Ok.code, response));
                markResponseTime();
            }
        }
        else if (pattern.equalsIgnoreCase(Consts.PATTERN_KNOWLEDGE)) {
            // 执行知识库问答
            int searchTopK = packet.data.has("searchTopK")
                    ? packet.data.getInt("searchTopK") : 10;
            int searchFetchK = packet.data.has("searchFetchK")
                    ? packet.data.getInt("searchFetchK") : 50;

            KnowledgeBase knowledgeBase = null;

            if (categories.isEmpty()) {
                String baseName = KnowledgeFramework.DefaultName;
                if (packet.data.has("base")) {
                    baseName = packet.data.getString("base");
                }
                knowledgeBase = service.getKnowledgeBase(token, baseName);
            }
            else {
                Logger.d(this.getClass(), "Select knowledge base by category: " + categories.get(0));
                List<KnowledgeBase> baseList = service.getKnowledgeBaseByCategory(token, categories.get(0));
                if (!baseList.isEmpty()) {
                    knowledgeBase = baseList.get(0);
                }
                else {
                    // 没有找到知识库，使用默认库
                    Logger.w(this.getClass(), "Can NOT find knowledge by category " + categories.get(0));
                    knowledgeBase = service.getKnowledgeBase(token, KnowledgeFramework.DefaultName);
                }
            }

            if (null != knowledgeBase) {
                success = knowledgeBase.performKnowledgeQA(code, ModelConfig.BAIZE_NEXT_UNIT, content, searchTopK,
                        recordList, new KnowledgeQAListener() {
                            @Override
                            public void onCompleted(AIGCChannel channel, KnowledgeQAResult result) {
                                cellet.speak(talkContext,
                                        makeResponse(dialect, packet, AIGCStateCode.Ok.code, result.toCompactJSON()));
                                markResponseTime();

                                // 写入事件
                                AppEvent appEvent = new AppEvent(AppEvent.Knowledge, System.currentTimeMillis(),
                                        channel.getAuthToken().getContactId(),
                                        AppEvent.createKnowledgeSuccessfulData(result));
                                if (!service.getStorage().writeAppEvent(appEvent)) {
                                    Logger.w(ChatTask.class, "Writes app event failed (knowledge) - cid: " +
                                            channel.getAuthToken().getContactId());
                                }
                            }

                            @Override
                            public void onFailed(AIGCChannel channel, AIGCStateCode errorCode) {
                                cellet.speak(talkContext,
                                        makeResponse(dialect, packet, errorCode.code, new JSONObject()));
                                markResponseTime();

                                // 写入事件
                                AppEvent appEvent = new AppEvent(AppEvent.Knowledge, System.currentTimeMillis(),
                                        channel.getAuthToken().getContactId(),
                                        AppEvent.createKnowledgeFailedData(channel.getLastUnitMetaSn(), errorCode,
                                                content));
                                if (!service.getStorage().writeAppEvent(appEvent)) {
                                    Logger.w(ChatTask.class, "Writes app event failed (knowledge) - cid: " +
                                            channel.getAuthToken().getContactId());
                                }
                            }
                        });
            }
        }

        if (!success) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.Failure.code, new JSONObject()));
            markResponseTime();
        }
    }
}
