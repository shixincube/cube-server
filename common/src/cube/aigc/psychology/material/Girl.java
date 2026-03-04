/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology.material;

import cube.util.Gender;
import org.json.JSONObject;

/**
 * 女孩。
 */
public class Girl extends Person {

    public Girl(JSONObject json) {
        super(json);
        this.gender = Gender.Female;
    }
}
