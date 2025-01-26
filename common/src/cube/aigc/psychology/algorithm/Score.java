/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
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
