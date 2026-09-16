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

    public long inputTokens;

    public long outputTokens;

    public final long elapsed;

    public Usage(String model, long inputTokens, long outputTokens, long elapsed) {
        this.model = model;
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
        this.elapsed = elapsed;
    }

    public Usage(JSONObject json) {
        this.model = json.has("model") ? json.getString("model") : "Omni";

        this.inputTokens = 0;
        if (json.has("inputTokens")) {
            this.inputTokens = json.getLong("inputTokens");
        }
        else if (json.has("tokens")) {
            this.inputTokens = json.getLong("tokens");
        }

        this.outputTokens =  0;
        if (json.has("outputTokens")) {
            this.outputTokens = json.getLong("outputTokens");
        }
        else if (json.has("generatedTokens")) {
            this.outputTokens = json.getLong("generatedTokens");
        }

        this.elapsed = json.has("elapsed") ? Long.parseLong(json.get("elapsed").toString()) : 0;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("model", this.model);
        json.put("inputTokens", this.inputTokens);
        json.put("outputTokens", this.outputTokens);
        json.put("elapsed", this.elapsed);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
