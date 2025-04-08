/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.guidance;

import org.json.JSONObject;

public class Answer {

    public final String code;

    public final String content;

    public Answer(JSONObject json) {
        this.code = json.has("code") ? json.getString("code") : null;
        this.content = json.has("content") ? json.getString("content") : null;
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        if (null != this.code) {
            json.put("code", this.code);
        }
        if (null != this.content) {
            json.put("content", this.content);
        }
        return json;
    }
}
