/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc;

import cell.util.Utils;
import cube.common.JSONable;
import cube.common.entity.GeneratingRecord;
import cube.common.entity.KnowledgeBaseInfo;
import cube.common.entity.KnowledgeQAResult;
import cube.common.state.AIGCStateCode;
import cube.util.EmojiFilter;
import org.json.JSONObject;

import java.util.Date;

/**
 * 应用事件。
 */
public class AppEvent implements JSONable {

    /**
     * 会话激活事件。
     */
    public final static String Session = "Session";

    /**
     * 对话事件。
     */
    public final static String Chat = "Chat";

    /**
     * 知识库问答事件。
     */
    public final static String Knowledge = "Knowledge";

    /**
     * 提交了新文件事件。
     */
    public final static String NewFile = "NewFile";

    /**
     * 删除已提交文件事件。
     */
    public final static String DeleteFile = "DeleteFile";

    /**
     * 导入知识库文档事件。
     */
    public final static String ImportKnowledgeDoc = "ImportKnowledgeDoc";

    /**
     * 移除知识库文档事件。
     */
    public final static String RemoveKnowledgeDoc = "RemoveKnowledgeDoc";

    /**
     * 新建知识库。
     */
    public final static String NewKnowledgeBase = "NewKnowledgeBase";

    /**
     * 删除知识库。
     */
    public final static String DeleteKnowledgeBase = "DeleteKnowledgeBase";

    public final String event;

    public final long timestamp;

    public final String time;

    public long contactId;

    public JSONObject data;

    public AppEvent(String event, long timestamp, JSONObject data) {
        this.event = event;
        this.timestamp = timestamp;
        this.time = Utils.gsDateFormat.format(new Date(this.timestamp));
        this.data = this.filterData(data);
    }

    public AppEvent(String event, long timestamp, long contactId, JSONObject data) {
        this.event = event;
        this.timestamp = timestamp;
        this.time = Utils.gsDateFormat.format(new Date(this.timestamp));
        this.contactId = contactId;
        this.data = this.filterData(data);
    }

    public AppEvent(String event, long timestamp, String time, long contactId, JSONObject data) {
        this.event = event;
        this.timestamp = timestamp;
        this.time = time;
        this.contactId = contactId;
        this.data = this.filterData(data);
    }

    public AppEvent(JSONObject json) {
        this.event = json.getString("event");
        this.timestamp = json.getLong("timestamp");
        this.time = json.has("time") ? json.getString("time") :
                Utils.gsDateFormat.format(new Date(this.timestamp));
        this.data = json.has("data") ? json.getJSONObject("data") : null;
        this.contactId = json.has("contactId") ? json.getLong("contactId") : 0;
    }

    private JSONObject filterData(JSONObject data) {
        if (null == data) {
            return null;
        }

        // 过滤表情字符
        String str = data.toString();
        String result = EmojiFilter.filterEmoji(str);
        try {
            return new JSONObject(result);
        } catch (Exception e) {
            return data;
        }
    }

    public JSONObject getSafeData() {
        return (null != this.data) ? this.data : new JSONObject();
    }

    public String getSafeString(String key) {
        return (null != this.data) ? (this.data.has(key) ? this.data.getString(key) : "") : "";
    }
    
    public int getSafeInt(String key) {
        return (null != this.data) ? (this.data.has(key) ? this.data.getInt(key) : 0) : 0;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("event", this.event);
        json.put("timestamp", this.timestamp);
        json.put("time", this.time);
        json.put("contactId", this.contactId);
        if (null != this.data) {
            json.put("data", this.data);
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }


    public static JSONObject createChatSuccessfulData(GeneratingRecord record) {
        JSONObject json = record.toJSON();
        json.put("code", AIGCStateCode.Ok.code);
        return json;
    }

    public static JSONObject createChatFailedData(long sn, AIGCStateCode stateCode,
                                                  String query, String unit) {
        JSONObject json = new JSONObject();
        json.put("sn", sn);
        json.put("code", stateCode.code);
        json.put("query", query);
        if (null != unit) {
            json.put("unit", unit);
        }
        return json;
    }

    public static JSONObject createKnowledgeSuccessfulData(KnowledgeQAResult result) {
        JSONObject json = result.toCompactJSON();
        json.put("sn", result.record.sn);
        json.put("code", AIGCStateCode.Ok.code);
        return json;
    }

    public static JSONObject createKnowledgeFailedData(long sn, AIGCStateCode stateCode,
                                                       String query) {
        JSONObject json = new JSONObject();
        json.put("sn", sn);
        json.put("code", stateCode.code);
        json.put("query", query);
        return json;
    }

    public static JSONObject createDeleteKnowledgeBaseData(KnowledgeBaseInfo info, JSONObject data) {
        data.put("name", info.name);
        data.put("category", info.category);
        data.put("baseTimestamp", info.timestamp);
        return data;
    }
}
