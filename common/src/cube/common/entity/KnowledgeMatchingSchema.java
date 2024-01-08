/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 Cube Team.
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

import cube.aigc.Consts;
import cube.common.JSONable;
import org.json.JSONObject;

/**
 * 知识匹配模式。
 */
public class KnowledgeMatchingSchema implements JSONable {

    private String category;

    private String sectionQuery;

    private String comprehensiveQuery;

    private int maxArticles = 5;

    private int maxParaphrases = 5;

    public KnowledgeMatchingSchema(String category, String comprehensiveQuery) {
        this.category = category;
        this.sectionQuery = Consts.KNOWLEDGE_SECTION_PROMPT;
        this.comprehensiveQuery = comprehensiveQuery;
    }

    public KnowledgeMatchingSchema(JSONObject json) {
        if (json.has("category")) {
            this.category = json.getString("category");
        }
        if (json.has("sectionQuery")) {
            this.sectionQuery = json.getString("sectionQuery");
        }
        if (json.has("comprehensiveQuery")) {
            this.comprehensiveQuery = json.getString("comprehensiveQuery");
        }
        this.maxArticles = json.getInt("maxArticles");
        this.maxParaphrases = json.getInt("maxParaphrases");
    }

    public String getCategory() {
        return this.category;
    }

    public String getSectionQuery() {
        return this.sectionQuery;
    }

    public String getComprehensiveQuery() {
        return this.comprehensiveQuery;
    }

    public int getMaxArticles() {
        return this.maxArticles;
    }

    public int getMaxParaphrases() {
        return this.maxParaphrases;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        if (null != this.category) {
            json.put("category", this.category);
        }
        if (null != this.sectionQuery) {
            json.put("sectionQuery", this.sectionQuery);
        }
        if (null != this.comprehensiveQuery) {
            json.put("comprehensiveQuery", this.comprehensiveQuery);
        }
        json.put("maxArticles", this.maxArticles);
        json.put("maxParaphrases", this.maxParaphrases);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
