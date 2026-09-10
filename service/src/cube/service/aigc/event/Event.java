/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.service.aigc.event;

import cube.common.JSONable;
import org.json.JSONObject;

public class Event implements JSONable {

    public final String name;

    public final String source;

    public final JSONObject payload;

    public Event(JSONObject json) {
        this.name = json.getString("name");
        this.source = json.getString("source");
        this.payload = json.getJSONObject("payload");
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("name", this.name);
        json.put("source", this.source);
        json.put("payload", this.payload);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
