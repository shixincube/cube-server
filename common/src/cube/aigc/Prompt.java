/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc;

import cube.common.JSONable;
import org.json.JSONObject;

/**
 * 提示词。
 */
public class Prompt implements JSONable {

    public String role;

    public String query;

    public String answer;

    public Prompt(String query) {
        this.query = query;
    }

    public Prompt(String query, String answer) {
        this.query = query;
        this.answer = answer;
    }

    public Prompt(JSONObject json) {
        if (json.has("role")) {
            this.role = json.getString("role");
        }
        if (json.has("query")) {
            this.query = json.getString("query");
        }
        if (json.has("answer")) {
            this.answer = json.getString("answer");
        }
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        if (null != this.role) {
            json.put("role", this.role);
        }
        if (null != this.query) {
            json.put("query", this.query);
        }
        if (null != this.answer) {
            json.put("answer", this.answer);
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
