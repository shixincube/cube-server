/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
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
