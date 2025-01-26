/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology.material.house;

import cube.aigc.psychology.material.Label;
import cube.aigc.psychology.material.Thing;
import org.json.JSONObject;

/**
 * 房顶。
 */
public class Roof extends Thing {

    private boolean textured = false;

    public Roof(JSONObject json) {
        super(json);
        if (Label.HouseRoofTextured == this.paintingLabel) {
            this.textured = true;
        }
    }

    public boolean isTextured() {
        return this.textured;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("textured", this.textured);
        return json;
    }
}
