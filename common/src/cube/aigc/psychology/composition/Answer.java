/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology.composition;

import org.json.JSONObject;

public class Answer {

    public final int sn;

    public final String code;

    public final String content;

    public boolean chosen = false;

    public Answer(int sn, JSONObject structure) {
        this.sn = sn;
        this.code = structure.getString("code");
        this.content = structure.getString("content");
        if (structure.has("chosen")) {
            this.chosen = structure.getBoolean("chosen");
        }
    }

    public Answer(JSONObject json) {
        this.sn = json.getInt("sn");
        this.code = json.getString("code");
        this.content = json.getString("content");
        if (json.has("chosen")) {
            this.chosen = json.getBoolean("chosen");
        }
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("sn", this.sn);
        json.put("code", this.code);
        json.put("content", this.content);
        json.put("chosen", this.chosen);
        return json;
    }
}
