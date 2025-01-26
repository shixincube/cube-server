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
 * 窗帘。
 */
public class Curtain extends Thing {

    private boolean open = false;

    public Curtain(JSONObject json) {
        super(json);

        if (Label.HouseCurtainOpened == this.paintingLabel) {
            this.open = true;
        }
    }

    public boolean isOpen() {
        return this.open;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("open", this.open);
        return json;
    }
}
