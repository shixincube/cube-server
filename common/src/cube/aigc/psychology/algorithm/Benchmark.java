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

import cell.util.log.Logger;
import cube.aigc.psychology.Indicator;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Benchmark {

    private Map<Indicator, BenchmarkScore> scoreMap;

    public Benchmark(JSONObject json) {
        this.scoreMap = new HashMap<>();

        Iterator<String> keys = json.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            Indicator indicator = Indicator.parse(key);
            if (null == indicator) {
                Logger.e(this.getClass(), "Can not find indicator: " + key);
                continue;
            }

            JSONObject value = json.getJSONObject(key);
            BenchmarkScore score = new BenchmarkScore(value);
            this.scoreMap.put(indicator, score);
        }
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        for (Map.Entry<Indicator, BenchmarkScore> entry : this.scoreMap.entrySet()) {
            json.put(entry.getKey().code, entry.getValue().toJSON(entry.getKey().name));
        }
        return json;
    }

    public class BenchmarkScore {

        public double positiveMin;

        public double positiveMax;

        public double negativeMin;

        public double negativeMax;

        public BenchmarkScore(JSONObject json) {
            JSONObject positive = json.getJSONObject("positive");
            JSONObject negative = json.getJSONObject("negative");
            this.positiveMin = positive.getDouble("min");
            this.positiveMax = positive.getDouble("max");
            this.negativeMin = negative.getDouble("min");
            this.negativeMax = negative.getDouble("max");
        }

        public JSONObject toJSON(String name) {
            JSONObject json = new JSONObject();
            json.put("name", name);

            JSONObject positive = new JSONObject();
            positive.put("min", this.positiveMin);
            positive.put("max", this.positiveMax);
            json.put("positive", positive);

            JSONObject negative = new JSONObject();
            negative.put("min", this.negativeMin);
            negative.put("max", this.negativeMax);
            json.put("negative", negative);

            return json;
        }
    }
}
