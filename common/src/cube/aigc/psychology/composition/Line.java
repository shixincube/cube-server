/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology.composition;

import cube.common.JSONable;
import org.json.JSONObject;

public class Line implements JSONable {

    public double thickness;

    public String type;

    public Line(JSONObject json) {
        this.thickness = json.getDouble("thickness");
        this.type = json.getString("type");
    }

    public boolean isThick() {
        return this.type.equalsIgnoreCase("thick");
    }

    public boolean isThin() {
        return this.type.equalsIgnoreCase("thin");
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("thickness", this.thickness);
        json.put("type", this.type);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
