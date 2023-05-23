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

import org.json.JSONObject;

/**
 * AIGC Chat 历史记录。
 */
public class AIGCChatHistory extends Entity {

    public String unit;

    public long queryContactId;

    public long queryTime;

    public String queryContent;

    public long answerContactId;

    public long answerTime;

    public String answerContent;

    public long contextId;

    public AIGCChatHistory(String unit) {
        super(0L, "shixincube.com");
        this.unit = unit;
    }

    public AIGCChatHistory(long id) {
        super(id, "shixincube.com");
    }

    public AIGCChatHistory(JSONObject json) {
        super(json);
        this.unit = json.getString("unit");
        this.queryContactId = json.getLong("queryContactId");
        this.queryTime = json.getLong("queryTime");
        this.queryContent = json.getString("queryContent");
        this.answerContactId = json.getLong("answerContactId");
        this.answerTime = json.getLong("answerTime");
        this.answerContent = json.getString("answerContent");
        this.contextId = json.getLong("contextId");
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("unit", this.unit);
        json.put("queryContactId", this.queryContactId);
        json.put("queryTime", this.queryTime);
        json.put("queryContent", this.queryContent);
        json.put("answerContactId", this.answerContactId);
        json.put("answerTime", this.answerTime);
        json.put("answerContent", this.answerContent);
        json.put("contextId", this.contextId);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
