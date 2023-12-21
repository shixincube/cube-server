/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 Cube Team.
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

import cell.util.Utils;
import cube.common.JSONable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * AIGC 内容生成记录。
 */
public class AIGCGenerationRecord implements JSONable {

    public final long sn;

    public String query;

    public String answer;

    public String unit;

    public List<FileLabel> fileLabels;

    public long timestamp;

    public ComplexContext context;

    public AIGCGenerationRecord(String unit, String query, String answer) {
        this.sn = Utils.generateSerialNumber();
        this.unit = unit;
        this.query = query;
        this.answer = answer;
        this.timestamp = System.currentTimeMillis();
    }

    public AIGCGenerationRecord(long sn, String unit, String query, String answer, long timestamp, ComplexContext context) {
        this.sn = sn;
        this.unit = unit;
        this.query = query;
        this.answer = answer;
        this.timestamp = timestamp;
        this.context = context;
    }

    public AIGCGenerationRecord(long sn, String unit, String query, FileLabel fileLabel, long timestamp) {
        this.sn = sn;
        this.unit = unit;
        this.query = query;
        this.fileLabels = new ArrayList<>();
        this.fileLabels.add(fileLabel);
        this.timestamp = timestamp;
    }

    public AIGCGenerationRecord(JSONObject json) {
        if (json.has("sn")) {
            this.sn = json.getLong("sn");
        }
        else {
            this.sn = Utils.generateSerialNumber();
        }

        if (json.has("unit")) {
            this.unit = json.getString("unit");
        }

        if (json.has("answer")) {
            this.answer = json.getString("answer");
        }

        if (json.has("query")) {
            this.query = json.getString("query");
        }

        if (json.has("fileLabels")) {
            this.fileLabels = new ArrayList<>();
            JSONArray array = json.getJSONArray("fileLabels");
            for (int i = 0; i < array.length(); ++i) {
                FileLabel fileLabel = new FileLabel(array.getJSONObject(i));
                this.fileLabels.add(fileLabel);
            }
        }

        if (json.has("timestamp")) {
            this.timestamp = json.getLong("timestamp");
        }

        if (json.has("context")) {
            this.context = new ComplexContext(json.getJSONObject("context"));
        }
    }

    public int totalWords() {
        return this.query.length() + ((null != this.answer) ? this.answer.length() : 0);
    }

    public JSONArray outputFileLabelArray() {
        if (null == this.fileLabels) {
            return null;
        }

        JSONArray array = new JSONArray();
        for (FileLabel fileLabel : this.fileLabels) {
            array.put(fileLabel.toJSON());
        }
        return array;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("sn", this.sn);

        if (null != this.unit) {
            json.put("unit", this.unit);
        }

        json.put("query", this.query);

        if (null != this.answer) {
            json.put("answer", this.answer);
        }

        if (null != this.fileLabels) {
            JSONArray array = new JSONArray();
            for (FileLabel fileLabel : this.fileLabels) {
                array.put(fileLabel.toJSON());
            }
            json.put("fileLabels", array);
        }

        json.put("timestamp", this.timestamp);

        if (null != this.context) {
            json.put("context", this.context.toJSON());
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = this.toJSON();
        if (json.has("query")) {
            json.remove("query");
        }
        return json;
    }
}
