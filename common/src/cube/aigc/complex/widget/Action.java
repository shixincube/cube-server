/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.complex.widget;

import org.json.JSONObject;

public abstract class Action {

    protected final String name;

    public Action(String name) {
        this.name = name;
    }

    public Action(JSONObject json) {
        this.name = json.getString("name");
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("name", this.name);
        return json;
    }
}
