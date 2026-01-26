/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology.material.other;

import cube.aigc.psychology.material.Thing;
import cube.common.entity.Material;
import org.json.JSONObject;

/**
 * 伞。
 */
public class Umbrella extends Thing {

    public Umbrella(JSONObject json) {
        super(json);
    }

    public Umbrella(Material material) {
        super(material);
    }
}
