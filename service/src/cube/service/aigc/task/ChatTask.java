/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 Cube Team.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
import cube.common.entity.AIGCChannel;
import cube.common.entity.AIGCGenerationRecord;
import cube.common.entity.KnowledgeQAResult;
import cube.common.state.AIGCStateCode;
import cube.service.ServiceTask;
import cube.service.aigc.AIGCCellet;
import cube.service.aigc.AIGCService;
import cube.service.aigc.knowledge.KnowledgeBase;
import cube.service.aigc.knowledge.KnowledgeFramework;
import cube.service.aigc.listener.GenerateTextListener;
import cube.service.aigc.listener.KnowledgeQAListener;
import cube.service.aigc.listener.TextToImageListener;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 向模型请求聊天。
 */
public class ChatTask extends ServiceTask {

    public ChatTask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
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
        String unit = packet.data.has("unit") ? packet.data.getString("unit") : ModelConfig.BAIZE_UNIT;
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

        List<AIGCGenerationRecord> recordList = null;
        if (null != records) {
            recordList = new ArrayList<>();
            try {
                for (int i = 0; i < records.length(); ++i) {
                    AIGCGenerationRecord record = new AIGCGenerationRecord(records.getJSONObject(i));
                    recordList.add(record);
                }
            } catch (Exception e) {
                Logger.w(this.getClass(), "#run", e);
            }
        }

        AIGCService service = ((AIGCCellet) this.cellet).getService();

        boolean success = false;

        // 检查频道
        AIGCChannel channel = service.getChannel(code);
        if (null == channel) {
            // 创建指定的频道
            Logger.i(this.getClass(), "#run - Create new channel for token: " + token);
            service.createChannel(token, "User-" + code, code);
        }

        // 根据工作模式进行调用
        if (pattern.equalsIgnoreCase(Consts.PATTERN_CHAT)) {
            // 判断单元任务类型
            if (ModelConfig.isTextToImageUnit(unit)) {
                // 执行 Text to Image
                success = service.generateImage(channel, content, unit, new TextToImageListener() {
                    @Override
                    public void onProcessing(AIGCChannel channel) {
                        cellet.speak(talkContext,
                                makeResponse(dialect, packet, AIGCStateCode.Processing.code, channel.toInfo()));
                        markResponseTime();
                    }

                    @Override
                    public void onCompleted(AIGCGenerationRecord record) {
                        cellet.speak(talkContext,
                                makeResponse(dialect, packet, AIGCStateCode.Ok.code, record.toJSON()));
                        markResponseTime();
                    }

                    @Override
                    public void onFailed(AIGCChannel channel, AIGCStateCode stateCode) {
                        if (stateCode == AIGCStateCode.Interrupted) {
                            // 被中断
                            AIGCGenerationRecord record = new AIGCGenerationRecord(unit,
                                    content, Consts.ANSWER_INTERRUPTED);
                            cellet.speak(talkContext,
                                    makeResponse(dialect, packet, AIGCStateCode.Ok.code, record.toJSON()));
                            markResponseTime();
                        }
                        else {
                            cellet.speak(talkContext,
                                    makeResponse(dialect, packet, AIGCStateCode.UnitError.code, new JSONObject()));
                            markResponseTime();
                        }
                    }
                });
            }
            else {
                // 执行文本生成
                success = service.generateText(code, content, unit, histories, recordList, categories, recordable, networking,
                        new GenerateTextListener() {
                    @Override
                    public void onGenerated(AIGCChannel channel, AIGCGenerationRecord record) {
                        cellet.speak(talkContext,
                                makeResponse(dialect, packet, AIGCStateCode.Ok.code, record.toJSON()));
                        markResponseTime();

                        // 写入事件
                        AppEvent appEvent = new AppEvent(AppEvent.Chat, System.currentTimeMillis(),
                                channel.getAuthToken().getContactId(),
                                AppEvent.createChatSuccessfulData(record));
                        if (!service.getStorage().writeAppEvent(appEvent)) {
                            Logger.w(ChatTask.class, "Writes app event failed (chat) - cid: " +
                                    channel.getAuthToken().getContactId());
                        }
                    }

                    @Override
                    public void onFailed(AIGCChannel channel, AIGCStateCode stateCode) {
                        if (stateCode == AIGCStateCode.Interrupted) {
                            // 被中断
                            AIGCGenerationRecord record = new AIGCGenerationRecord(unit,
                                    content, Consts.ANSWER_INTERRUPTED);
                            cellet.speak(talkContext,
                                    makeResponse(dialect, packet, AIGCStateCode.Ok.code, record.toJSON()));
                            markResponseTime();
                        }
                        else {
                            cellet.speak(talkContext,
                                    makeResponse(dialect, packet, stateCode.code, new JSONObject()));
                            markResponseTime();
                        }

                        // 写入事件
                        AppEvent appEvent = new AppEvent(AppEvent.Chat, System.currentTimeMillis(),
                                channel.getAuthToken().getContactId(),
                                AppEvent.createChatFailedData(channel.getLastUnitMetaSn(), stateCode,
                                        content, unit));
                        if (!service.getStorage().writeAppEvent(appEvent)) {
                            Logger.w(ChatTask.class, "Writes app event failed (chat) - cid: " +
                                    channel.getAuthToken().getContactId());
                        }
                    }
                });
            }
        }
        else if (pattern.equalsIgnoreCase(Consts.PATTERN_KNOWLEDGE)) {
            // 执行知识库问答
            int searchTopK = packet.data.has("searchTopK")
                    ? packet.data.getInt("searchTopK") : 10;
            int searchFetchK = packet.data.has("searchFetchK")
                    ? packet.data.getInt("searchFetchK") : 50;

            String baseName = KnowledgeFramework.DefaultName;
            if (packet.data.has("base")) {
                baseName = packet.data.getString("base");
            }

            KnowledgeBase knowledgeBase = service.getKnowledgeBase(token, baseName);
            if (null != knowledgeBase) {
                success = knowledgeBase.performKnowledgeQA(code, unit, content, searchTopK, searchFetchK,
                        categories, recordList, new KnowledgeQAListener() {
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
