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
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 情绪分析结果。
 * @deprecated
 */
public class SentimentResult implements JSONable {

    /**
     * 负面得分。
     */
    public double negative = 0;

    /**
     * 正面得分。
     */
    public double positive = 0;

    /**
     * 评价标签。
     */
    public String label;

    /**
     * 段落得分。
     */
    public List<SentimentResult> paragraphs;

    public SentimentResult(JSONObject json) {
        this.label = json.getString("label");
        this.negative = json.getDouble("negative");
        this.positive = json.getDouble("positive");

        if (json.has("paragraphs")) {
            this.paragraphs = new ArrayList<>();
            JSONArray array = json.getJSONArray("paragraphs");
            for (int i = 0; i < array.length(); ++i) {
                SentimentResult sr = new SentimentResult(array.getJSONObject(i));
                this.paragraphs.add(sr);
            }
        }
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("label", this.label);
        json.put("negative", this.negative);
        json.put("positive", this.positive);

        if (null != this.paragraphs) {
            JSONArray array = new JSONArray();
            for (SentimentResult sr : this.paragraphs) {
                array.put(sr.toJSON());
            }
            json.put("paragraphs", array);
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
