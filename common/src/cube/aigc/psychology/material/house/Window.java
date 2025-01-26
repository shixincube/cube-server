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
 * 窗户。
 */
public class Window extends Thing {

    private boolean open = false;

    public Window(JSONObject json) {
        super(json);
        if (Label.HouseWindowOpened == this.paintingLabel) {
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
