/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.aigc.psychology;

import cube.aigc.psychology.algorithm.Score;
import cube.common.JSONable;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class KeyFeature implements JSONable {

    private String name;

    private String description;

    private List<Score> indicators;

    public KeyFeature(String name, String description) {
        this.name = name;
        this.description = description;
        this.indicators = new ArrayList<>();
    }

    public void addIndicatorScore(Score score) {
        this.indicators.add(score);
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();

        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
