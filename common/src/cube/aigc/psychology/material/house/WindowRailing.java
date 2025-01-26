/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology.material.house;

import cube.aigc.psychology.material.Thing;
import org.json.JSONObject;

/**
 * 窗户栏栅。
 */
public class WindowRailing extends Thing {

    public WindowRailing(JSONObject json) {
        super(json);
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        return json;
    }
}
