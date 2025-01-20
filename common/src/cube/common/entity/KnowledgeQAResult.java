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

import cube.common.JSONable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 知识库问答结果。
 */
public class KnowledgeQAResult implements JSONable {

    public final String query;

    public String prompt;

    public List<KnowledgeSource> sources;

    public GeneratingRecord record;

    public AIGCConversationResponse conversationResponse;

    public KnowledgeQAResult(String query, String prompt) {
        this.query = query;
        this.prompt = prompt;
        this.sources = new ArrayList<>();
    }

    public KnowledgeQAResult(JSONObject json) {
        this.sources = new ArrayList<>();
        this.query = json.getString("query");
        if (json.has("prompt")) {
            this.prompt = json.getString("prompt");
        }
        if (json.has("record")) {
            this.record = new GeneratingRecord(json.getJSONObject("record"));
        }
        if (json.has("sources")) {
            JSONArray array = json.getJSONArray("sources");
            for (int i = 0; i < array.length(); ++i) {
                KnowledgeSource source = new KnowledgeSource(array.getJSONObject(i));
                this.sources.add(source);
            }
        }
    }

    public JSONArray sourcesToArray() {
        JSONArray array = new JSONArray();
        for (KnowledgeSource source : this.sources) {
            array.put(source.toJSON());
        }
        return array;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("query", this.query);
        if (null != this.prompt) {
            json.put("prompt", this.prompt);
        }
        if (null != this.record) {
            json.put("record", this.record.toJSON());
        }
        if (!this.sources.isEmpty()) {
            json.put("sources", this.sourcesToArray());
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = this.toJSON();
        if (json.has("prompt")) {
            json.remove("prompt");
        }
        if (json.has("record")) {
            // 将 record 里的 query 修改为原 query
            JSONObject recordJson = json.getJSONObject("record");
            recordJson.put("query", this.query);
        }
        return json;
    }
}
