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
 * 房门。
 */
public class Door extends Thing {

    private boolean open = false;

    private boolean lock = false;

    public Door(JSONObject json) {
        super(json);

        if (Label.HouseDoorOpened == this.paintingLabel) {
            this.open = true;
        }
        else if (Label.HouseDoorLocked == this.paintingLabel) {
            this.lock = true;
        }
    }

    public boolean isOpen() {
        return this.open;
    }

    public boolean isLock() {
        return this.lock;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("open", this.open);
        json.put("lock", this.lock);
        return json;
    }
}
