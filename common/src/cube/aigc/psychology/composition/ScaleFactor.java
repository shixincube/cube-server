/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology.composition;

import org.json.JSONObject;

/**
 * 量表因子。
 */
public class ScaleFactor {

    public String name;

    public String displayName;

    public double score;

    public String description = "";

    public String suggestion = "";

    public ScaleFactor(String name, String displayName, double score) {
        this.name = name;
        this.displayName = displayName;
        this.score = score;
    }

    public ScaleFactor(JSONObject json) {
        this.name = json.getString("name");
        this.displayName = json.getString("displayName");
        this.score = json.getDouble("score");
        this.description = json.getString("description");
        this.suggestion = json.getString("suggestion");
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("name", this.name);
        json.put("displayName", this.displayName);
        json.put("score", this.score);
        json.put("description", this.description);
        json.put("suggestion", this.suggestion);
        return json;
    }
}
