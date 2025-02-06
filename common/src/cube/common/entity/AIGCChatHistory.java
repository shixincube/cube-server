/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

import org.json.JSONObject;

import java.util.List;

/**
 * AIGC Chat 历史记录。
 */
public class AIGCChatHistory extends Entity {

    public final long sn;

    public final String channelCode;

    public String unit;

    public long queryContactId;

    public long queryTime;

    public String queryContent;

    public List<FileLabel> queryFileLabels;

    public long answerContactId;

    public long answerTime;

    public String answerContent;

    public List<FileLabel> answerFileLabels;

    public ComplexContext context;

    public int feedback = 0;

    public long contextId = 0;

    public AIGCChatHistory(long sn, String channelCode, String unit, String domain) {
        super(0L, (null != domain ? domain : ""));
        this.sn = sn;
        this.channelCode = channelCode;
        this.unit = unit;
    }

    public AIGCChatHistory(long id, long sn, String channelCode, String domain) {
        super(id, (null != domain ? domain : ""));
        this.sn = sn;
        this.channelCode = channelCode;
    }

    public AIGCChatHistory(JSONObject json) {
        super(json);
        this.sn = json.getLong("sn");
        this.channelCode = json.has("channel") ? json.getString("channel") : "-";
        this.unit = json.getString("unit");
        this.queryContactId = json.getLong("queryContactId");
        this.queryTime = json.getLong("queryTime");
        this.queryContent = json.getString("queryContent");
        this.answerContactId = json.getLong("answerContactId");
        this.answerTime = json.getLong("answerTime");
        this.answerContent = json.getString("answerContent");
        this.feedback = json.getInt("feedback");
        this.contextId = json.getLong("contextId");
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("sn", this.sn);
        json.put("channel", this.channelCode);
        json.put("unit", this.unit);
        json.put("queryContactId", this.queryContactId);
        json.put("queryTime", this.queryTime);
        json.put("queryContent", this.queryContent);
        json.put("answerContactId", this.answerContactId);
        json.put("answerTime", this.answerTime);
        json.put("answerContent", this.answerContent);
        json.put("feedback", this.feedback);
        json.put("contextId", this.contextId);



        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
