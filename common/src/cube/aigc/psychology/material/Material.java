/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology.material;

import cube.common.JSONable;
import cube.vision.BoundingBox;
import org.json.JSONObject;

public class Material implements JSONable {

    public String label;

    public double prob;

    public BoundingBox boundingBox;

    public Material(JSONObject json) {
        this.label = json.getString("label");
        this.prob = json.getDouble("prob");
        this.boundingBox = new BoundingBox(json.getJSONObject("bbox"));
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("label", this.label);
        json.put("prob", this.prob);
        json.put("bbox", this.boundingBox.toJSON());
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
