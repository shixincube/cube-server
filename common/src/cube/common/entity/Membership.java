/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

import org.json.JSONObject;

public class Membership extends Entity {

    public String name;

    public String type;

    public long duration;

    public String description;

    public JSONObject context;

    public Membership(long contactId, String domain, String name, String type, long timestamp, long duration,
                      String description, JSONObject context) {
        super(contactId, domain, timestamp);
        this.name = name;
        this.type = type;
        this.duration = duration;
        this.description = description;
        this.context = context;
    }

    public Membership(JSONObject json) {
        super(json);
        this.name = json.getString("name");
        this.type = json.getString("type");
        this.duration = json.getLong("duration");
        this.description = json.getString("description");
        if (json.has("context")) {
            this.context = json.getJSONObject("context");
        }
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("name", this.name);
        json.put("type", this.type);
        json.put("duration", this.duration);
        json.put("description", this.description);
        if (null != this.context) {
            json.put("context", this.context);
        }
        return json;
    }
}
