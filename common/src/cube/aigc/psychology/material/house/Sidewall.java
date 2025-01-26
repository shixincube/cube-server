/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology.material.house;

import cube.aigc.psychology.material.Label;
import cube.aigc.psychology.material.Thing;
import org.json.JSONObject;

import java.util.List;

/**
 * 侧墙。
 */
public class Sidewall extends Thing {

    public Sidewall(JSONObject json) {
        super(json);
    }

    @Override
    public List<Thing> getSubThings(Label label) {
        return null;
    }
}
