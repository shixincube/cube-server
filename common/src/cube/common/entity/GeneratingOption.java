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

    public GeneratingOption() {
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
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("temperature", this.temperature);
        json.put("topP", this.topP);
        json.put("repetitionPenalty", this.repetitionPenalty);
        json.put("topK", this.topK);
        json.put("maxNewTokens", this.maxNewTokens);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
