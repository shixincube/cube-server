/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.common.entity;

import cube.common.JSONable;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MultimodalTask implements JSONable {

    private String name;

    private Map<String, Object> option;

    public MultimodalTask(JSONObject json) {
        this.name = json.getString("name");

        this.option = new HashMap<>();
        Iterator<String> iter = json.keys();
        while (iter.hasNext()) {
            String key = iter.next();
            if (key.equals("name")) {
                continue;
            }

            Object value = json.get(key);
            if (value instanceof String || value instanceof Integer || value instanceof Long) {
                this.option.put(key, value);
            }
        }
    }

    public String getName() {
        return this.name;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("name", this.name);
        if (null != this.option) {
            for (String key : this.option.keySet()) {
                Object value = this.option.get(key);
                json.put(key, value);
            }
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
