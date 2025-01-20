/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 Ambrose Xu.
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
import cube.common.JSONable;
import org.json.JSONObject;

/**
 * AIGC 互动聊天应答。
 */
public class AIGCConversationResponse implements JSONable {

    public final long sn;

    public final String unit;

    public String query;

    public String answer;

//    public String thought;

//    public boolean needHistory;

//    public String candidateQuery;

//    public String candidateAnswer;

    public long timestamp;

    public ComplexContext context;

    public boolean processing = false;

    public AIGCConversationResponse(long sn, String unit) {
        this.sn = sn;
        this.unit = unit;
        this.answer = "";
        this.timestamp = System.currentTimeMillis();
        this.processing = true;
    }

    public AIGCConversationResponse(JSONObject json) {
        if (json.has("sn")) {
            this.sn = json.getLong("sn");
        }
        else {
            this.sn = Utils.generateSerialNumber();
        }

        if (json.has("unit")) {
            this.unit = json.getString("unit");
        }
        else {
            this.unit = "";
        }

        if (json.has("query")) {
            this.query = json.getString("query");
        }

        this.answer = json.getString("answer");

//        this.thought = json.getString("thought");
//        this.needHistory = json.getBoolean("needHistory");
//        this.candidateQuery = json.getString("candidateQuery");
//        this.candidateAnswer = json.getString("candidateAnswer");

        if (json.has("timestamp")) {
            this.timestamp = json.getLong("timestamp");
        }
        else {
            this.timestamp = System.currentTimeMillis();
        }

        if (json.has("context")) {
            this.context = new ComplexContext(json.getJSONObject("context"));
        }

        this.processing = json.getBoolean("processing");
    }

    public AIGCConversationResponse(GeneratingRecord record) {
        this.sn = record.sn;
        this.unit = record.unit;
        this.query = record.query;
        this.answer = record.answer;
        this.context = record.context;
        this.timestamp = record.timestamp;
    }

//    public AIGCConversationResponse(long sn, String unit, String query, ComplexContext context, JSONObject payload) {
//        this.sn = sn;
//        this.unit = unit;
//        this.query = query;
//        this.context = context;
//
//        this.answer = payload.getString("answer");
//        this.thought = payload.getString("thought");
//        this.needHistory = payload.getBoolean("needHistory");
//        this.candidateQuery = payload.getString("candidateQuery");
//        this.candidateAnswer = payload.getString("candidateAnswer");
//
//        if (payload.has("timestamp")) {
//            this.timestamp = payload.getLong("timestamp");
//        }
//        else {
//            this.timestamp = System.currentTimeMillis();
//        }
//    }

    public GeneratingRecord toRecord() {
        GeneratingRecord record = new GeneratingRecord(this.sn,
                this.unit, this.query, this.answer, this.timestamp, this.context);
        return record;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        if (null != this.query) {
            json.put("query", this.query);
        }

        json.put("sn", this.sn);
        json.put("unit", this.unit);
        json.put("answer", this.answer);

//        json.put("thought", this.thought);
//        json.put("needHistory", this.needHistory);
//        json.put("candidateQuery", this.candidateQuery);
//        json.put("candidateAnswer", this.candidateAnswer);

        json.put("timestamp", this.timestamp);

        if (null != this.context) {
            json.put("context", this.context.toJSON());
        }

        json.put("processing", this.processing);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = this.toJSON();
        if (json.has("query")) {
            json.remove("query");
        }
        return json;
    }
}
