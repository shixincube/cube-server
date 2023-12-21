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
import cube.auth.AuthToken;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AIGC 分配给用户的通道。
 */
public class AIGCChannel extends Entity {

    private int historyLengthLimit = 4 * 1024;

    private AuthToken authToken;

    private String participant;

    private long creationTime;

    private String code;

    private long activeTimestamp;

    // 倒序存储历史记录
    private LinkedList<AIGCGenerationRecord> history;

    // 倒序存储应答历史
    private LinkedList<AIGCConversationResponse> conversationResponses;

    private AtomicBoolean processing;

    private int totalQueryWords = 0;

    private int totalAnswerWords = 0;

    /**
     * 对话轮次记录。
     */
    private AtomicInteger rounds;

    /**
     * 最近一次任务的 SN 。
     */
    private long lastUnitMetaSn;

    public AIGCChannel(AuthToken authToken, String participant) {
        this(authToken, participant, Utils.randomString(16));
    }

    public AIGCChannel(AuthToken authToken, String participant, String channelCode) {
        this.authToken = authToken;
        this.participant = participant;
        this.creationTime = System.currentTimeMillis();
        this.code = channelCode;
        this.history = new LinkedList<>();
        this.conversationResponses = new LinkedList<>();
        this.processing = new AtomicBoolean(false);
        this.activeTimestamp = this.creationTime;
        this.rounds = new AtomicInteger(0);
    }

    public AIGCChannel(JSONObject json) {
        this.authToken = new AuthToken(json.getJSONObject("authToken"));
        this.code = json.getString("code");
        this.creationTime = json.getLong("creationTime");
        this.participant = json.getString("participant");
        this.history = new LinkedList<>();
        this.conversationResponses = new LinkedList<>();

        this.processing = new AtomicBoolean(json.has("processing") && json.getBoolean("processing"));
        this.activeTimestamp = json.has("activeTimestamp") ? json.getLong("activeTimestamp") :  this.creationTime;
        this.rounds = new AtomicInteger(json.has("rounds") ? json.getInt("rounds") : 0);
        this.totalQueryWords = json.has("totalQueryWords") ? json.getInt("totalQueryWords") : 0;
        this.totalAnswerWords = json.has("totalAnswerWords") ? json.getInt("totalAnswerWords") : 0;

        this.lastUnitMetaSn = json.has("lastMetaSn") ? json.getLong("lastMetaSn") : 0;

        if (json.has("lastRecord")) {
            AIGCGenerationRecord record = new AIGCGenerationRecord(json.getJSONObject("lastRecord"));
            this.history.addFirst(record);
        }
    }

    public AuthToken getAuthToken() {
        return this.authToken;
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

    public void setActiveTimestamp(long time) {
        this.activeTimestamp = time;
    }

    public void setProcessing(boolean value) {
        this.processing.set(value);
    }

    public boolean isProcessing() {
        return this.processing.get();
    }

    public int getRounds() {
        return this.rounds.get();
    }

    public void setLastUnitMetaSn(long sn) {
        this.lastUnitMetaSn = sn;
    }

    public long getLastUnitMetaSn() {
        return this.lastUnitMetaSn;
    }

    public AIGCGenerationRecord getLastRecord() {
        synchronized (this.history) {
            if (this.history.isEmpty()) {
                return null;
            }

            return this.history.getFirst();
        }
    }

    public AIGCGenerationRecord appendRecord(long sn, String unit, String query, String answer, ComplexContext context) {
        this.activeTimestamp = System.currentTimeMillis();

        this.totalQueryWords += query.length();
        this.totalAnswerWords += answer.length();

        this.rounds.incrementAndGet();

        AIGCGenerationRecord record = new AIGCGenerationRecord(sn, unit, query, answer, this.activeTimestamp, context);
        synchronized (this.history) {
            this.history.addFirst(record);
        }
        return record;
    }

    public AIGCGenerationRecord appendRecord(long sn, String unit, String query, FileLabel fileLabel) {
        this.activeTimestamp = System.currentTimeMillis();

        this.totalQueryWords += query.length();
        this.totalAnswerWords += fileLabel.getFileCode().length();

        this.rounds.incrementAndGet();

        AIGCGenerationRecord record = new AIGCGenerationRecord(sn, unit, query, fileLabel, this.activeTimestamp);
        synchronized (this.history) {
            this.history.addFirst(record);
        }
        return record;
    }

    public AIGCGenerationRecord appendRecord(long sn, AIGCConversationResponse conversationResponse) {
        this.activeTimestamp = System.currentTimeMillis();

        this.totalQueryWords += conversationResponse.query.length();
        this.totalAnswerWords += conversationResponse.answer.length();

        this.rounds.incrementAndGet();

        AIGCGenerationRecord record = new AIGCGenerationRecord(sn, conversationResponse.unit,
                conversationResponse.query, conversationResponse.answer, this.activeTimestamp,
                conversationResponse.context);
        synchronized (this.history) {
            this.history.addFirst(record);
        }

        this.conversationResponses.addFirst(conversationResponse);
        return record;
    }

    public List<AIGCGenerationRecord> getLastHistory(int num) {
        List<AIGCGenerationRecord> list = new ArrayList<>(num);

        synchronized (this.history) {
            for (AIGCGenerationRecord record : this.history) {
                if (record.totalWords() > this.historyLengthLimit) {
                    // 过滤掉词多的问答
                    continue;
                }

                list.add(record);
                if (list.size() >= num) {
                    break;
                }
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

    /**
     * 统计向 AI 提问的总字数。
     *
     * @return
     */
    public int totalQueryWords() {
        return this.totalQueryWords;
    }

    /**
     * 统计 AI 回答的总字数。
     *
     * @return
     */
    public int totalAnswerWords() {
        return this.totalAnswerWords;
    }

    public JSONObject toInfo() {
        JSONObject json = this.toJSON();
        json.put("activeTimestamp", this.activeTimestamp);
        json.put("rounds", this.rounds.get());
        json.put("processing", this.processing.get());
        json.put("totalQueryWords", this.totalQueryWords());
        json.put("totalAnswerWords", this.totalAnswerWords());
        json.put("lastMetaSn", this.lastUnitMetaSn);

        AIGCGenerationRecord record = this.getLastRecord();
        if (null != record) {
            json.put("lastRecord", record.toCompactJSON());
        }
        return json;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("code", this.code);
        json.put("creationTime", this.creationTime);
        json.put("participant", this.participant);
        json.put("authToken", this.authToken.toJSON());
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
