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

import cell.util.Utils;
import cube.util.FloatUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class HexagonDimensionScore {

    private Map<HexagonDimension, Integer> scores;

    private Map<HexagonDimension, String> descriptions;

    private Map<HexagonDimension, Integer> rates;

    public HexagonDimensionScore() {
        this.scores = new LinkedHashMap<>();
        this.descriptions = new LinkedHashMap<>();
        this.rates = new LinkedHashMap<>();
    }

    public HexagonDimensionScore(JSONObject json) {
        this.scores = new LinkedHashMap<>();
        this.descriptions = new LinkedHashMap<>();
        this.rates = new LinkedHashMap<>();
        if (json.has("factors") && json.has("scores") && json.has("descriptions")) {
            // 新结构
            JSONArray factors = json.getJSONArray("factors");
            JSONArray scores = json.getJSONArray("scores");
            JSONArray descriptions = json.getJSONArray("descriptions");
            JSONArray rates = json.getJSONArray("rates");
            for (int i = 0; i < factors.length(); ++i) {
                String factor = factors.getString(i);
                int score = scores.getInt(i);
                String description = descriptions.getString(i);
                int rate = rates.getInt(i);
                HexagonDimension hexagonDimension = HexagonDimension.parse(factor);
                this.scores.put(hexagonDimension, score);
                this.descriptions.put(hexagonDimension, description);
                this.rates.put(hexagonDimension, rate);
            }
        }
        else {
            // 旧结构
            Iterator<String> iter = json.keys();
            while (iter.hasNext()) {
                String key = iter.next();
                HexagonDimension hexagonDimension = HexagonDimension.parse(key);
                if (null == hexagonDimension) {
                    continue;
                }
                int value = json.getInt(key);
                this.scores.put(hexagonDimension, value);
            }
        }
    }

    public void record(HexagonDimension dim, int value) {
        this.scores.put(dim, value);
    }

    public void record(HexagonDimension dim, int rate, String description) {
        this.rates.put(dim, rate);
        this.descriptions.put(dim, description);
    }

    public int getDimensionScore(HexagonDimension hexagonDimension) {
        return this.scores.get(hexagonDimension);
    }

    public String getDimensionDescription(HexagonDimension hexagonDimension) {
        return this.descriptions.get(hexagonDimension);
    }

    public void normalization() {
        int max = 0;
        for (Integer v : this.scores.values()) {
            if (v > max) {
                max = v;
            }
        }
        max = max + max > 99 ?
                Math.max(max, (70 + Utils.randomInt(1, 9))) : (max + max);

        double[] values = new double[this.scores.size()];
        int index = 0;
        for (Map.Entry<HexagonDimension, Integer> entry : this.scores.entrySet()) {
            values[index++] = entry.getValue();
        }
        double[] output = FloatUtils.scale(values, max);
        index = 0;
        for (Map.Entry<HexagonDimension, Integer> entry : this.scores.entrySet()) {
            entry.setValue((int) Math.floor(output[index++]));
        }
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();

        // 旧结构
        for (Map.Entry<HexagonDimension, Integer> e : this.scores.entrySet()) {
            json.put(e.getKey().name, e.getValue().intValue());
        }

        // 新结构
        JSONArray factors = new JSONArray();
        JSONArray scores = new JSONArray();
        JSONArray descriptions = new JSONArray();
        JSONArray rates = new JSONArray();
        for (Map.Entry<HexagonDimension, Integer> e : this.scores.entrySet()) {
            factors.put(e.getKey().name);
            scores.put(e.getValue().intValue());

            String desc = this.descriptions.get(e.getKey());
            if (null != desc) {
                descriptions.put(desc);
            }

            rates.put(this.rates.get(e.getKey()));
        }
        json.put("factors", factors);
        json.put("scores", scores);
        if (descriptions.length() > 0) {
            json.put("descriptions", descriptions);
            json.put("rates", rates);
        }

        return json;
    }
}
