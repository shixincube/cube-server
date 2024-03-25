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
public class GenerativeOption implements JSONable {

    public double temperature = 0.3;

    public double topP = 0.95;

    public double repetitionPenalty = 1.3;

    public int topK = 50;

    public GenerativeOption() {
    }

    public GenerativeOption(double temperature, double topP, double repetitionPenalty, int topK) {
        this.temperature = temperature;
        this.topP = topP;
        this.repetitionPenalty = repetitionPenalty;
        this.topK = topK;
    }

    public GenerativeOption(JSONObject json) {
        this.temperature = json.getDouble("temperature");
        this.topP = json.getDouble("topP");
        this.repetitionPenalty = json.getDouble("repetitionPenalty");
        this.topK = json.getInt("topK");
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("temperature", this.temperature);
        json.put("topP", this.topP);
        json.put("repetitionPenalty", this.repetitionPenalty);
        json.put("topK", this.topK);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
