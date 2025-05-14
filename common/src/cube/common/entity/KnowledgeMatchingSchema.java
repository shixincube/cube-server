/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

import cube.common.JSONable;
import org.json.JSONObject;

/**
 * 知识匹配模式。
 * @deprecated
 */
public class KnowledgeMatchingSchema implements JSONable {

    private String category;

    private String sectionQuery;

    private String comprehensiveQuery;

    private int maxArticles = 5;

    private int maxParaphrases = 5;

    public KnowledgeMatchingSchema(String category, String comprehensiveQuery) {
        this.category = category;
//        this.sectionQuery = Consts.KNOWLEDGE_SECTION_PROMPT;
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
