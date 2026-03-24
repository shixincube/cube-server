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

public class PromptTemplate implements JSONable {

    public final String name;

    public final Map<String, String> parameters;

    public PromptTemplate(JSONObject json) {
        this.name = json.getString("name");
        this.parameters = new HashMap<>();
        JSONObject paramMap = json.getJSONObject("parameters");
        for (String key : paramMap.keySet()) {
            this.parameters.put(key, paramMap.getString(key));
        }
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("name", this.name);
        JSONObject paramMap = new JSONObject();
        Iterator<Map.Entry<String, String>> iter = this.parameters.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, String> e = iter.next();
            paramMap.put(e.getKey(), e.getValue());
        }
        json.put("parameters", paramMap);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
