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

import cube.common.JSONable;
import org.json.JSONObject;

/**
 * AIGC 互动聊天记录。
 */
public class AIGCConversationResponse implements JSONable {

    public String query;

    public String answer;

    public String thought;

    public boolean needHistory;

    public String candidateQuery;

    public String candidateAnswer;

    public long timestamp;

    public AIGCConversationResponse(JSONObject json) {
        if (json.has("query")) {
            this.query = json.getString("query");
        }

        this.answer = json.getString("answer");
        this.thought = json.getString("thought");
        this.needHistory = json.getBoolean("needHistory");
        this.candidateQuery = json.getString("candidateQuery");
        this.candidateAnswer = json.getString("candidateAnswer");

        if (json.has("timestamp")) {
            this.timestamp = json.getLong("timestamp");
        }
        else {
            this.timestamp = System.currentTimeMillis();
        }
    }

    public AIGCConversationResponse(String query, JSONObject json) {
        this.query = query;

        this.answer = json.getString("answer");
        this.thought = json.getString("thought");
        this.needHistory = json.getBoolean("needHistory");
        this.candidateQuery = json.getString("candidateQuery");
        this.candidateAnswer = json.getString("candidateAnswer");

        if (json.has("timestamp")) {
            this.timestamp = json.getLong("timestamp");
        }
        else {
            this.timestamp = System.currentTimeMillis();
        }
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        if (null != this.query) {
            json.put("query", this.query);
        }

        json.put("answer", this.answer);
        json.put("thought", this.thought);
        json.put("needHistory", this.needHistory);
        json.put("candidateQuery", this.candidateQuery);
        json.put("candidateAnswer", this.candidateAnswer);

        json.put("timestamp", this.timestamp);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}