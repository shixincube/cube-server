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

import cube.util.FloatUtils;
import org.json.JSONObject;

import java.util.*;

public class SixDimensionScore {

    private Map<SixDimension, Integer> scores;

    public SixDimensionScore() {
        this.scores = new LinkedHashMap<>();
    }

    public SixDimensionScore(JSONObject json) {
        this.scores = new LinkedHashMap<>();
        Iterator<String> iter = json.keys();
        while (iter.hasNext()) {
            String key = iter.next();
            int value = json.getInt(key);
            SixDimension sixDimension = SixDimension.parse(key);
            this.scores.put(sixDimension, value);
        }
    }

    public void record(SixDimension dim, int value) {
        this.scores.put(dim, value);
    }

    public int getDimensionScore(SixDimension sixDimension) {
        return this.scores.get(sixDimension);
    }

    public void normalization() {
        double[] values = new double[this.scores.size()];
        int index = 0;
        for (Map.Entry<SixDimension, Integer> entry : this.scores.entrySet()) {
            values[index++] = entry.getValue();
        }
        double[] output = FloatUtils.scale(values, 100);
        index = 0;
        for (Map.Entry<SixDimension, Integer> entry : this.scores.entrySet()) {
            entry.setValue((int) Math.floor(output[index++]));
        }
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        for (Map.Entry<SixDimension, Integer> e : this.scores.entrySet()) {
            json.put(e.getKey().name, e.getValue().intValue());
        }
        return json;
    }
}
