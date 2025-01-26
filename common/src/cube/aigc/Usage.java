/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc;

import cube.common.JSONable;
import org.json.JSONObject;

/**
 * 用量。
 */
public class Usage implements JSONable {

    public final String model;

    public final long completionTokens;

    public final long promptTokens;

    public final long totalTokens;

    public String name;

    public Usage(String model, long completionTokens, long promptTokens, long totalTokens) {
        this.model = model;
        this.completionTokens = completionTokens;
        this.promptTokens = promptTokens;
        this.totalTokens = totalTokens;
    }

    public Usage(JSONObject json) {
        this.model = json.has("model") ? json.getString("model") : "Baize";
        this.completionTokens = json.has("completionTokens") ?
                json.getLong("completionTokens") : json.getLong("completion_tokens");
        this.promptTokens = json.has("promptTokens") ?
                json.getLong("promptTokens") : json.getLong("prompt_tokens");
        this.totalTokens = json.has("totalTokens") ?
                json.getLong("totalTokens") : json.getLong("total_tokens");

        if (json.has("name")) {
            this.name = json.getString("name");
        }
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("model", this.model);
        json.put("completionTokens", this.completionTokens);
        json.put("completion_tokens", this.completionTokens);
        json.put("promptTokens", this.promptTokens);
        json.put("prompt_tokens", this.promptTokens);
        json.put("totalTokens", this.totalTokens);
        json.put("total_tokens", this.totalTokens);
        if (null != this.name) {
            json.put("name", this.name);
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
