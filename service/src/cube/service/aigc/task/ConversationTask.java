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
import cube.benchmark.ResponseTime;
import cube.common.Packet;
import cube.common.entity.AIGCChannel;
import cube.common.entity.AIGCChatRecord;
import cube.common.entity.AIGCConversationParameter;
import cube.common.entity.AIGCConversationResponse;
import cube.common.state.AIGCStateCode;
import cube.service.ServiceTask;
import cube.service.aigc.AIGCCellet;
import cube.service.aigc.AIGCService;
import cube.service.aigc.listener.ConversationListener;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 向模型请求会话。
 */
public class ConversationTask extends ServiceTask {

    public ConversationTask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
        super(cellet, talkContext, primitive, responseTime);
    }

    @Override
    public void run() {
        ActionDialect dialect = new ActionDialect(this.primitive);
        Packet packet = new Packet(dialect);

        if (!packet.data.has("content") || !packet.data.has("code")) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.InvalidParameter.code, new JSONObject()));
            markResponseTime();
            return;
        }

        String code = packet.data.getString("code");
        String content = packet.data.getString("content");
        JSONArray records = packet.data.has("records") ? packet.data.getJSONArray("records") : null;
        float temperature = packet.data.has("temperature") ? packet.data.getFloat("temperature") : 0.7f;
        float topP = packet.data.has("topP") ? packet.data.getFloat("topP") : 0.8f;
        float repetitionPenalty = packet.data.has("repetitionPenalty") ?
                packet.data.getFloat("repetitionPenalty") : 1.02f;

        List<AIGCChatRecord> recordList = null;
        if (null != records) {
            recordList = new ArrayList<>();
            try {
                for (int i = 0; i < records.length(); ++i) {
                    AIGCChatRecord record = new AIGCChatRecord(records.getJSONObject(i));
                    recordList.add(record);
                }
            } catch (Exception e) {
                Logger.w(this.getClass(), "#run", e);
            }
        }

        AIGCConversationParameter parameter = new AIGCConversationParameter(temperature,
                topP, repetitionPenalty, recordList);

        AIGCService service = ((AIGCCellet) this.cellet).getService();

        // 执行 Conversation
        boolean success = service.conversation(code, content, parameter, new ConversationListener() {
            @Override
            public void onConversation(AIGCChannel channel, AIGCConversationResponse response) {
                cellet.speak(talkContext,
                        makeResponse(dialect, packet, AIGCStateCode.Ok.code, response.toJSON()));
                markResponseTime();
            }

            @Override
            public void onFailed(AIGCChannel channel, int errorCode) {
                cellet.speak(talkContext,
                        makeResponse(dialect, packet, errorCode, new JSONObject()));
                markResponseTime();
            }
        });

        if (!success) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.Failure.code, new JSONObject()));
            markResponseTime();
        }
    }
}
