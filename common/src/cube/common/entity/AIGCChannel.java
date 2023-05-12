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

package cube.common.entity;

import cell.util.Utils;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AIGC 分配给用户的通道。
 */
public class AIGCChannel extends Entity {

    private int historyLengthLimit = 1024;

    private String participant;

    private long creationTime;

    private String code;

    private long activeTimestamp;

    // 倒序存储历史记录
    private LinkedList<AIGCChatRecord> history;

    // 倒序存储应答历史
    private LinkedList<AIGCConversationResponse> conversationResponses;

    private AtomicBoolean processing;

    public AIGCChannel(String participant) {
        this.participant = participant;
        this.creationTime = System.currentTimeMillis();
        this.code = Utils.randomString(16);
        this.history = new LinkedList<>();
        this.conversationResponses = new LinkedList<>();
        this.processing = new AtomicBoolean(false);
        this.activeTimestamp = this.creationTime;
    }

    public AIGCChannel(JSONObject json) {
        this.code = json.getString("code");
        this.creationTime = json.getLong("creationTime");
        this.participant = json.getString("participant");
        this.history = new LinkedList<>();
        this.conversationResponses = new LinkedList<>();
        this.processing = new AtomicBoolean(false);
        this.activeTimestamp = this.creationTime;
    }

    public String getCode() {
        return this.code;
    }

    public long getCreationTime() {
        return this.creationTime;
    }

    public String getParticipant() {
        return this.participant;
    }

    public long getActiveTimestamp() {
        return this.activeTimestamp;
    }

    public void setProcessing(boolean value) {
        this.processing.set(value);
    }

    public boolean isProcessing() {
        return this.processing.get();
    }

    public AIGCChatRecord appendRecord(String query, String answer) {
        this.activeTimestamp = System.currentTimeMillis();

        AIGCChatRecord record = new AIGCChatRecord(query, answer, this.activeTimestamp);
        this.history.addFirst(record);
        return record;
    }

    public AIGCChatRecord appendRecord(AIGCConversationResponse conversationResponse) {
        this.activeTimestamp = System.currentTimeMillis();

        AIGCChatRecord record = new AIGCChatRecord(conversationResponse.query,
                conversationResponse.answer, this.activeTimestamp);
        this.history.addFirst(record);

        this.conversationResponses.addFirst(conversationResponse);
        return record;
    }

    public List<AIGCChatRecord> getLastHistory(int num) {
        List<AIGCChatRecord> list = new ArrayList<>(num);
        for (AIGCChatRecord record : this.history) {
            if (record.totalWords() > this.historyLengthLimit) {
                // 过滤掉词多的问答
                continue;
            }

            list.add(record);
            if (list.size() >= num) {
                break;
            }
        }

        // 调换顺序，成为时间正序
        Collections.reverse(list);
        return list;
    }

    public List<AIGCConversationResponse> extractConversationResponses() {
        List<AIGCConversationResponse> list = new ArrayList<>();
        if (this.conversationResponses.isEmpty()) {
            return list;
        }

        for (AIGCConversationResponse cr : this.conversationResponses) {
            if (cr.needHistory) {
                list.add(cr);
            }
            else {
                break;
            }
        }

        // 调换顺序，成为时间正序
        Collections.reverse(list);
        return list;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("code", this.code);
        json.put("creationTime", this.creationTime);
        json.put("participant", this.participant);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
