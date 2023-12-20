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

package cube.aigc;

import cell.util.Utils;
import cube.common.JSONable;
import cube.common.entity.AIGCGenerationRecord;
import cube.common.entity.KnowledgeQAResult;
import cube.common.state.AIGCStateCode;
import org.json.JSONObject;

import java.util.Date;

/**
 * 应用事件。
 */
public class AppEvent implements JSONable {

    /**
     * 会话激活事件。
     */
    public final static String Session = "session";

    /**
     * 对话事件。
     */
    public final static String Chat = "chat";

    /**
     * 知识库问答事件。
     */
    public final static String Knowledge = "knowledge";

    /**
     * 提交了新文件事件。
     */
    public final static String NewFile = "newFile";

    /**
     * 删除已提交文件事件。
     */
    public final static String DeleteFile = "deleteFile";

    public final String event;

    public final long timestamp;

    public final String time;

    public long contactId;

    public JSONObject data;

    public AppEvent(String event, long timestamp, JSONObject data) {
        this.event = event;
        this.timestamp = timestamp;
        this.time = Utils.gsDateFormat.format(new Date(this.timestamp));
        this.data = data;
    }

    public AppEvent(String event, long timestamp, long contactId, JSONObject data) {
        this.event = event;
        this.timestamp = timestamp;
        this.time = Utils.gsDateFormat.format(new Date(this.timestamp));
        this.contactId = contactId;
        this.data = data;
    }

    public AppEvent(String event, long timestamp, String time, long contactId, JSONObject data) {
        this.event = event;
        this.timestamp = timestamp;
        this.time = time;
        this.contactId = contactId;
        this.data = data;
    }

    public AppEvent(JSONObject json) {
        this.event = json.getString("event");
        this.timestamp = json.getLong("timestamp");
        this.time = json.has("time") ? json.getString("time") :
                Utils.gsDateFormat.format(new Date(this.timestamp));
        this.data = json.has("data") ? json.getJSONObject("data") : null;
        this.contactId = json.has("contactId") ? json.getLong("contactId") : 0;
    }

    public JSONObject getSafeData() {
        return (null != this.data) ? this.data : new JSONObject();
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


    public static JSONObject createChatSuccessfulData(AIGCGenerationRecord record) {
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
}
