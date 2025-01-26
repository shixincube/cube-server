/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology.material;

import org.json.JSONObject;

/**
 * 男孩。
 */
public class Boy extends Person {

    public Boy(JSONObject json) {
        super(json);
        this.gender = Gender.Male;
    }
}
