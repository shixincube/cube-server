/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology.composition;

import cube.common.JSONable;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class ScaleScore implements JSONable {

    private Map<String, JSONObject> items;

    public ScaleScore() {
        this.items = new LinkedHashMap<>();
    }

    public ScaleScore(JSONObject json) {
        this.items = new LinkedHashMap<>();
        Iterator<String> iter = json.keys();
        while (iter.hasNext()) {
            String key = iter.next();
            JSONObject value = json.getJSONObject(key);
            this.items.put(key, value);
        }
    }

    public void addItem(String key, String name, Object value, ScaleFactorLevel level) {
        JSONObject pair = new JSONObject();
        pair.put("name", name);
        pair.put("value", value);
        pair.put("level", level.toJSON());
        this.items.put(key, pair);
    }

    public Object getItem(String name) {
        return this.items.get(name);
    }

    public String getItemName(String key) {
        JSONObject data = this.items.get(key);
        if (null == data) {
            return null;
        }

        return data.getString("name");
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        for (Map.Entry<String, JSONObject> e : this.items.entrySet()) {
            String key = e.getKey();
            JSONObject value = e.getValue();
            json.put(key, value);
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
