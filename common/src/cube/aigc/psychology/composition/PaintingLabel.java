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

import cube.aigc.psychology.algorithm.Representation;
import cube.common.JSONable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class PaintingLabel implements JSONable {

    private long sn;

    private long timestamp;

    private String description;

    private List<Representation> representations;

    private List<EvaluationScore> evaluationScores;

    public PaintingLabel(JSONObject json) {
        this.sn = json.getLong("sn");
        this.timestamp = json.getLong("timestamp");
        this.description = json.getString("description");
        if (json.has("representations")) {
            this.representations = new ArrayList<>();
            JSONArray array = json.getJSONArray("representations");
            for (int i = 0; i < array.length(); ++i) {
                this.representations.add(new Representation(array.getJSONObject(i)));
            }
        }
        if (json.has("evaluationScores")) {
            this.evaluationScores = new ArrayList<>();
            JSONArray array = json.getJSONArray("evaluationScores");
            for (int i = 0; i < array.length(); ++i) {
                this.evaluationScores.add(new EvaluationScore(array.getJSONObject(i)));
            }
        }
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("sn", this.sn);
        json.put("timestamp", this.timestamp);
        json.put("description", this.description);
        if (null != this.representations) {
            JSONArray array = new JSONArray();
            for (Representation representation : this.representations) {
                array.put(representation.toJSON());
            }
            json.put("representations", array);
        }
        if (null != this.evaluationScores) {
            JSONArray array = new JSONArray();
            for (EvaluationScore score : this.evaluationScores) {
                array.put(score.toJSON());
            }
            json.put("evaluationScores", array);
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
