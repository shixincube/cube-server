/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

import cube.common.JSONable;
import org.json.JSONObject;

public class UserModification implements JSONable {

    public final long id;

    public String displayName;

    public UserModification(long id) {
        this.id = id;
    }

    public UserModification(JSONObject json) {
        this.id = json.getLong("id");
        if (json.has("displayName")) {
            this.displayName = json.getString("displayName");
        }
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("id", this.id);
        if (null != this.displayName) {
            json.put("displayName", this.displayName);
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
