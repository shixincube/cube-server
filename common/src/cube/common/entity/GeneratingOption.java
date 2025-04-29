/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

import cube.common.JSONable;
import org.json.JSONObject;

/**
 * AIGC 生成选项。
 */
public class GeneratingOption implements JSONable {

    public double temperature = 0.3;

    public double topP = 0.85;

    public double repetitionPenalty = 1.05;

    public int topK = 5;

    public int maxNewTokens = 2048;

    public boolean recognizeContext = false;

    public GeneratingOption() {
    }

    public GeneratingOption(boolean recognizeFlow) {
        this.recognizeContext = recognizeFlow;
    }

    public GeneratingOption(double temperature, double topP, double repetitionPenalty, int maxNewTokens, int topK) {
        this.temperature = temperature;
        this.topP = topP;
        this.repetitionPenalty = repetitionPenalty;
        this.maxNewTokens = maxNewTokens;
        this.topK = topK;
    }

    public GeneratingOption(JSONObject json) {
        if (json.has("temperature")) {
            this.temperature = json.getDouble("temperature");
        }

        if (json.has("topP")) {
            this.topP = json.getDouble("topP");
        }

        if (json.has("repetitionPenalty")) {
            this.repetitionPenalty = json.getDouble("repetitionPenalty");
        }

        if (json.has("topK")) {
            this.topK = json.getInt("topK");
        }

        if (json.has("maxNewTokens")) {
            this.maxNewTokens = json.getInt("maxNewTokens");
        }

        if (json.has("recognizeContext")) {
            this.recognizeContext = json.getBoolean("recognizeContext");
        }
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("temperature", this.temperature);
        json.put("topP", this.topP);
        json.put("repetitionPenalty", this.repetitionPenalty);
        json.put("topK", this.topK);
        json.put("maxNewTokens", this.maxNewTokens);
        json.put("recognizeContext", this.recognizeContext);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
