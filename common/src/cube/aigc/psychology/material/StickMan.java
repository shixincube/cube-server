/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology.material;

import org.json.JSONObject;

/**
 * 火柴人。
 */
public class StickMan extends Person {

    public StickMan(JSONObject json) {
        super(json);
        this.gender = Gender.Unknown;
    }
}
