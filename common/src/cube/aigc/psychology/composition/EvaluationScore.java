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

package cube.aigc.psychology.composition;

import cube.aigc.psychology.Indicator;
import cube.common.JSONable;
import org.json.JSONObject;

/**
 * 评估得分。
 */
public class EvaluationScore implements JSONable {

    public final Indicator indicator;

    public int hit = 0;

    public int value = 0;

    public int positive = 0;

    public double positiveWeight = 0;

    public int negative = 0;

    public double negativeWeight = 0;

    public double positiveScore = 0;

    public double negativeScore = 0;

    public EvaluationScore(Indicator indicator, int value, double weight) {
        this.indicator = indicator;
        this.hit = 1;
        this.value = value;
        if (value > 0) {
            this.positive = value;
            this.positiveWeight = weight;
            this.positiveScore = value * weight;
        }
        else if (value < 0) {
            this.negative = Math.abs(value);
            this.negativeWeight = weight;
            this.negativeScore = Math.abs(value) * weight;
        }
    }

    public EvaluationScore(JSONObject json) {
        this.indicator = Indicator.parse(json.getString("indicator"));
        this.hit = json.getInt("hit");
        this.value = json.getInt("value");
        this.positiveScore = json.getDouble("positiveScore");
        this.negativeScore = json.getDouble("negativeScore");
    }

    public void scoring(Score score) {
        this.hit += 1;
        // 原始分
        this.value += score.value;

        if (score.value > 0) {
            // 最大权重值
            if (score.weight > this.positiveWeight) {
                this.positive = score.value;
                this.positiveWeight = score.weight;
            }

            // 计分
            this.positiveScore += score.value * score.weight;
        }
        else if (score.value < 0) {
            // 最大权重值
            if (score.weight > this.negativeWeight) {
                this.negative = Math.abs(score.value);
                this.negativeWeight = score.weight;
            }

            // 计分
            this.negativeScore += Math.abs(score.value) * score.weight;
        }
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("indicator", this.indicator.name);
        json.put("hit", this.hit);
        json.put("value", this.value);
        json.put("positiveScore", this.positiveScore);
        json.put("negativeScore", this.negativeScore);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
