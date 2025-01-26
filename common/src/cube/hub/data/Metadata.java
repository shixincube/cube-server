/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.hub.data;

import cube.common.JSONable;
import org.json.JSONObject;

/**
 * 元。
 */
public class Metadata implements JSONable {

    protected long id;

    protected long timestamp;

    public Metadata(long id) {
        this.id = id;
        this.timestamp = System.currentTimeMillis();
    }

    public Metadata(long id, long timestamp) {
        this.id = id;
        this.timestamp = timestamp;
    }

    public Metadata(JSONObject json) {
        this.id = json.getLong("id");
        this.timestamp = json.getLong("timestamp");
    }

    public long getId() {
        return this.id;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("id", this.id);
        json.put("timestamp", this.timestamp);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
