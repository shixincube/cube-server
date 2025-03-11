/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

import cell.core.net.Endpoint;
import cell.util.Utils;
import cube.auth.AuthToken;
import cube.common.Domain;
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

    private int historyLengthLimit = 2 * 1024;

    private AuthToken authToken;

    private String participant;

    private long creationTime;

    private String code;

    private long activeTimestamp;

    // 倒序存储历史记录
    private LinkedList<GeneratingRecord> history;

    private AtomicBoolean processing;

    private long processingTimestamp;

    private int totalQueryWords = 0;

    private int totalAnswerWords = 0;

    private Endpoint httpEndpoint;
    private Endpoint httpsEndpoint;

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
        this.domain = new Domain(authToken.getDomain());
        this.authToken = authToken;
        this.participant = participant;
        this.creationTime = System.currentTimeMillis();
        this.code = channelCode;
        this.history = new LinkedList<>();
        this.processing = new AtomicBoolean(false);
        this.activeTimestamp = this.creationTime;
        this.rounds = new AtomicInteger(0);
    }

    public AIGCChannel(JSONObject json) {
        this.code = json.getString("code");
        this.creationTime = json.getLong("creationTime");
        this.participant = json.getString("participant");

        if (json.has("authToken")) {
            this.authToken = new AuthToken(json.getJSONObject("authToken"));
            this.domain = new Domain(this.authToken.getDomain());
        }

        this.history = new LinkedList<>();

        this.processing = new AtomicBoolean(json.has("processing") && json.getBoolean("processing"));
        this.activeTimestamp = json.has("activeTimestamp") ? json.getLong("activeTimestamp") :  this.creationTime;
        this.rounds = new AtomicInteger(json.has("rounds") ? json.getInt("rounds") : 0);
        this.totalQueryWords = json.has("totalQueryWords") ? json.getInt("totalQueryWords") : 0;
        this.totalAnswerWords = json.has("totalAnswerWords") ? json.getInt("totalAnswerWords") : 0;

        this.lastUnitMetaSn = json.has("lastMetaSn") ? json.getLong("lastMetaSn") : 0;

        if (json.has("lastRecord")) {
            GeneratingRecord record = new GeneratingRecord(json.getJSONObject("lastRecord"));
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

    public void setEndpoint(Endpoint http, Endpoint https) {
        this.httpEndpoint = http;
        this.httpsEndpoint = https;
    }

    public Endpoint getHttpEndpoint() {
        return this.httpEndpoint;
    }

    public Endpoint getHttpsEndpoint() {
        return this.httpsEndpoint;
    }

    public void setProcessing(boolean value) {
        this.processingTimestamp = System.currentTimeMillis();
        this.processing.set(value);
    }

    public boolean isProcessing() {
        return this.processing.get();
    }

    public long getProcessingTimestamp() {
        return this.processingTimestamp;
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

    public GeneratingRecord getLastRecord() {
        synchronized (this.history) {
            if (this.history.isEmpty()) {
                return null;
            }

            return this.history.getFirst();
        }
    }

    public GeneratingRecord getRecord(long sn) {
        synchronized (this.history) {
            for (GeneratingRecord record : this.history) {
                if (record.sn == sn) {
                    return record;
                }
            }
        }
        return null;
    }

    public GeneratingRecord appendRecord(long sn, String unit, String query, String answer, String thought, ComplexContext context) {
        this.activeTimestamp = System.currentTimeMillis();

        this.totalQueryWords += query.length();
        this.totalAnswerWords += answer.length();

        this.rounds.incrementAndGet();

        GeneratingRecord record = new GeneratingRecord(sn, unit, query, answer, thought, this.activeTimestamp, context);
        synchronized (this.history) {
            this.history.addFirst(record);
        }
        return record;
    }

    public GeneratingRecord appendRecord(long sn, String unit, String query, FileLabel fileLabel) {
        this.activeTimestamp = System.currentTimeMillis();

        this.totalQueryWords += query.length();
        this.totalAnswerWords += fileLabel.getFileCode().length();

        this.rounds.incrementAndGet();

        GeneratingRecord record = new GeneratingRecord(sn, unit, query, fileLabel, this.activeTimestamp);
        synchronized (this.history) {
            this.history.addFirst(record);
        }
        return record;
    }

//    public GenerativeRecord appendRecord(long sn, AIGCConversationResponse conversationResponse) {
//        this.activeTimestamp = System.currentTimeMillis();
//
//        this.totalQueryWords += conversationResponse.query.length();
//        this.totalAnswerWords += conversationResponse.answer.length();
//
//        this.rounds.incrementAndGet();
//
//        GenerativeRecord record = new GenerativeRecord(sn, conversationResponse.unit,
//                conversationResponse.query, conversationResponse.answer, this.activeTimestamp,
//                conversationResponse.context);
//        synchronized (this.history) {
//            this.history.addFirst(record);
//        }
//
//        this.conversationResponses.addFirst(conversationResponse);
//        return record;
//    }

    public List<GeneratingRecord> getHistories() {
        return this.history;
    }

    public List<GeneratingRecord> getLastHistory(int num) {
        List<GeneratingRecord> list = new ArrayList<>(num);

        synchronized (this.history) {
            for (GeneratingRecord record : this.history) {
                if (record.totalWords() > this.historyLengthLimit) {
                    // 过滤掉词多的问答
                    continue;
                }

                // 过滤低分项
                if (record.feedback == 1) {
                    // 跳过差评
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

    public void feedbackRecord(long sn, int feedback) {
        synchronized (this.history) {
            for (GeneratingRecord record : this.history) {
                if (record.sn == sn) {
                    record.feedback = feedback;
                    break;
                }
            }
        }
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

        GeneratingRecord record = this.getLastRecord();
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

        if (null != this.authToken) {
            json.put("authToken", this.authToken.toJSON());
        }

        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = this.toJSON();
        if (json.has("authToken")) {
            json.remove("authToken");
        }
        return json;
    }
}
