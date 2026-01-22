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
 * 火焰。
 */
public class Fire extends Thing {

    public Fire(JSONObject json) {
        super(json);
    }

    public Fire(Material material) {
        super(material);
    }
}
