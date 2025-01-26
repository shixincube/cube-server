/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

import cell.util.Utils;
import cube.common.JSONable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * AIGC 内容生成记录。
 */
public class GeneratingRecord implements JSONable {

    public final long sn;

    public String query;

    public String answer;

    public List<FileLabel> queryFileLabels;

    public String[] queryAdditions;

    public List<FileLabel> answerFileLabels;

    public String unit;

    public long timestamp;

    public ComplexContext context;

    public int feedback = 0;

    /**
     * 仅用于客户端向服务器以附件方式传递数据。
     *
     * @param queryAdditions
     */
    public GeneratingRecord(String[] queryAdditions) {
        this.sn = Utils.generateSerialNumber();
        this.queryAdditions = queryAdditions;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 仅用于客户端向服务器以附件方式传递数据。
     *
     * @param queryFileLabels
     */
    public GeneratingRecord(List<FileLabel> queryFileLabels) {
        this.sn = Utils.generateSerialNumber();
        this.queryFileLabels = queryFileLabels;
        this.timestamp = System.currentTimeMillis();
    }

    public GeneratingRecord(String unit, String query, String answer) {
        this.sn = Utils.generateSerialNumber();
        this.unit = unit;
        this.query = query;
        this.answer = answer;
        this.timestamp = System.currentTimeMillis();
    }

    public GeneratingRecord(long sn, String unit, String query, String answer, long timestamp, ComplexContext context) {
        this.sn = sn;
        this.unit = unit;
        this.query = query;
        this.answer = answer;
        this.timestamp = timestamp;
        this.context = context;
    }

    public GeneratingRecord(long sn, String unit, String query, FileLabel answerFileLabel, long timestamp) {
        this.sn = sn;
        this.unit = unit;
        this.query = query;
        this.answerFileLabels = new ArrayList<>();
        this.answerFileLabels.add(answerFileLabel);
        this.timestamp = timestamp;
    }

    public GeneratingRecord(String query) {
        this.sn = Utils.generateSerialNumber();
        this.query = query;
        this.timestamp = System.currentTimeMillis();
    }

    public GeneratingRecord(String query, FileLabel queryFileLabel) {
        this.sn = Utils.generateSerialNumber();
        this.query = query;
        this.queryFileLabels = new ArrayList<>();
        this.queryFileLabels.add(queryFileLabel);
        this.timestamp = System.currentTimeMillis();
    }

    public GeneratingRecord(JSONObject json) {
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

        if (json.has("queryAdditions")) {
            JSONArray array = json.getJSONArray("queryAdditions");
            this.queryAdditions = new String[array.length()];
            for (int i = 0; i < array.length(); ++i) {
                String text = array.getString(i);
                this.queryAdditions[i] = text;
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

    public boolean hasQueryAddition() {
        return (null != this.queryAdditions && this.queryAdditions.length > 0);
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

    public void addAnswerFileLabel(FileLabel fileLabel) {
        if (null == this.answerFileLabels) {
            this.answerFileLabels = new ArrayList<>();
        }
        this.answerFileLabels.add(fileLabel);
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
    public String toString() {
        StringBuilder buf = new StringBuilder();
        if (null != this.query) {
            buf.append(this.query);
        }
        buf.append(" -> ");
        if (null != this.answer) {
            buf.append(this.answer);
        }
        return buf.toString();
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

            // 兼容旧版本
            json.put("fileLabels", array);
        }

        if (null != this.queryAdditions) {
            JSONArray array = new JSONArray();
            for (String text : this.queryAdditions) {
                array.put(text);
            }
            json.put("queryAdditions", array);
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

    public JSONObject toTraceJSON() {
        JSONObject json = this.toJSON();
        if (json.has("query")) {
            json.remove("query");
        }
        if (json.has("unit")) {
            json.remove("unit");
        }
        json.put("unit", "AiXinLi");
        json.put("company", "白泽京智");

        json.put("date", Utils.gsDateFormat.format(new Date(this.timestamp)));
        return json;
    }
}
