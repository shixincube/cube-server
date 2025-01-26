/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology.material;

import org.json.JSONObject;

/**
 * 女人。
 */
public class Woman extends Person {

    public Woman(JSONObject json) {
        super(json);
        this.gender = Gender.Female;
    }
}
