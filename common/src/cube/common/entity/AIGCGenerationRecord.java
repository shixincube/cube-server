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

    public List<FileLabel> queryFileLabels;

    public List<FileLabel> answerFileLabels;

    public String unit;

    public long timestamp;

    public ComplexContext context;

    public int feedback = 0;

    public AIGCGenerationRecord(List<FileLabel> queryFileLabels) {
        this.sn = Utils.generateSerialNumber();
        this.queryFileLabels = queryFileLabels;
        this.timestamp = System.currentTimeMillis();
    }

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

    public AIGCGenerationRecord(long sn, String unit, String query, FileLabel answerFileLabel, long timestamp) {
        this.sn = sn;
        this.unit = unit;
        this.query = query;
        this.answerFileLabels = new ArrayList<>();
        this.answerFileLabels.add(answerFileLabel);
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

        if (json.has("answerFileLabels")) {
            this.answerFileLabels = new ArrayList<>();
            JSONArray array = json.getJSONArray("answerFileLabels");
            for (int i = 0; i < array.length(); ++i) {
                FileLabel fileLabel = new FileLabel(array.getJSONObject(i));
                this.answerFileLabels.add(fileLabel);
            }
        }

        if (json.has("queryFileLabels")) {
            this.queryFileLabels = new ArrayList<>();
            JSONArray array = json.getJSONArray("queryFileLabels");
            for (int i = 0; i < array.length(); ++i) {
                FileLabel fileLabel = new FileLabel(array.getJSONObject(i));
                this.queryFileLabels.add(fileLabel);
            }
        }

        if (json.has("timestamp")) {
            this.timestamp = json.getLong("timestamp");
        }

        if (json.has("context")) {
            this.context = new ComplexContext(json.getJSONObject("context"));
        }
    }

    public boolean hasQueryFile() {
        return (null != this.queryFileLabels && !this.queryFileLabels.isEmpty());
    }

    public int totalWords() {
        if (null == this.query) {
            return 0;
        }

        return this.query.length() + ((null != this.answer) ? this.answer.length() : 0);
    }

    public JSONArray outputAnswerFileLabelArray() {
        if (null == this.answerFileLabels) {
            return null;
        }

        JSONArray array = new JSONArray();
        for (FileLabel fileLabel : this.answerFileLabels) {
            array.put(fileLabel.toJSON());
        }
        return array;
    }

    public JSONArray outputQueryFileLabelArray() {
        if (null == this.queryFileLabels) {
            return null;
        }

        JSONArray array = new JSONArray();
        for (FileLabel fileLabel : this.queryFileLabels) {
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

        if (null != this.query) {
            json.put("query", this.query);
        }

        if (null != this.answer) {
            json.put("answer", this.answer);
        }

        if (null != this.answerFileLabels) {
            JSONArray array = new JSONArray();
            for (FileLabel fileLabel : this.answerFileLabels) {
                array.put(fileLabel.toJSON());
            }
            json.put("answerFileLabels", array);
        }

        if (null != this.queryFileLabels) {
            JSONArray array = new JSONArray();
            for (FileLabel fileLabel : this.queryFileLabels) {
                array.put(fileLabel.toJSON());
            }
            json.put("queryFileLabels", array);
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
