/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

import cube.common.JSONable;
import org.json.JSONObject;

public class EmotionRatio implements JSONable {

    public final double score;

    public final int positiveRatio;

    public final int negativeRatio;

    public final int neutralRatio;

    public EmotionRatio(double score, int positiveRatio, int negativeRatio, int neutralRatio) {
        this.score = score;
        this.positiveRatio = positiveRatio;
        this.negativeRatio = negativeRatio;
        this.neutralRatio = neutralRatio;
    }

    public EmotionRatio(JSONObject json) {
        this.score = json.getDouble("score");
        this.positiveRatio = json.getInt("positiveRatio");
        this.negativeRatio = json.getInt("negativeRatio");
        this.neutralRatio = json.getInt("neutralRatio");
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("score", this.score);
        json.put("positiveRatio", this.positiveRatio);
        json.put("negativeRatio", this.negativeRatio);
        json.put("neutralRatio", this.neutralRatio);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
