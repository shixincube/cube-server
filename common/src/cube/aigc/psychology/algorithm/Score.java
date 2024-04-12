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

package cube.aigc.psychology.algorithm;

import cube.aigc.psychology.Indicator;
import cube.common.JSONable;
import org.json.JSONObject;

/**
 * 得分。
 */
public class Score implements JSONable {

    public final Indicator indicator;

    public int value;

    public double weight;

    public Score(Indicator indicator, int value, double weight) {
        this.indicator = indicator;
        this.value = value;
        this.weight = weight;
    }

    public Score(JSONObject json) {
        this.indicator = Indicator.parse(json.getString("indicator"));
        this.value = json.getInt("value");
        this.weight = json.getDouble("weight");
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Score) {
            Score other = (Score) obj;
            if (other.indicator == this.indicator) {
                return true;
            }
        }

        return false;
    }

    @Override
    public int hashCode() {
        return this.indicator.hashCode();
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("indicator", this.indicator.name);
        json.put("value", this.value);
        json.put("weight", this.weight);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = new JSONObject();
        json.put("indicator", this.indicator.name);
        json.put("value", this.value);
        json.put("weight", this.weight);
        return json;
    }
}
