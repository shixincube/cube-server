/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.guidance;

import cube.common.JSONable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;

public class EvaluationResult implements JSONable {

    public String name;

    public boolean result;

    public Map<String, Boolean> evaluationItems;

    private Map<String, String> qaPairs;

    public EvaluationResult(String name) {
        this.name = name;
        this.evaluationItems = new LinkedHashMap<>();
        this.qaPairs = new LinkedHashMap<>();
    }

    public EvaluationResult(JSONObject json) {
        this.name = json.getString("name");
        this.result = json.getBoolean("result");
        this.evaluationItems = new LinkedHashMap<>();
        JSONArray array = json.getJSONArray("items");
        for (int i = 0; i < array.length(); ++i) {
            JSONObject data = array.getJSONObject(i);
            this.evaluationItems.put(data.getString("item"), data.getBoolean("value"));
        }
        this.qaPairs = new LinkedHashMap<>();
    }

    public void addItem(String name, boolean value) {
        this.evaluationItems.put(name, value);
    }

    public boolean getItem(String name) {
        return this.evaluationItems.get(name);
    }

    public String toMarkdown() {
        StringBuilder buf = new StringBuilder();
        buf.append("**").append(this.name).append("**\n\n");
        buf.append("- ***").append(this.result ? "是" : "否").append("***").append("\n\n");
        for (Map.Entry<String, Boolean> entry : this.evaluationItems.entrySet()) {
            buf.append("> ").append(entry.getKey());
            buf.append("    ").append(entry.getValue() ? "是" : "否");
            buf.append("\n");
        }
        return buf.toString().trim();
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("name", this.name);
        json.put("result", this.result);
        JSONArray array = new JSONArray();
        for (Map.Entry<String, Boolean> entry : this.evaluationItems.entrySet()) {
            JSONObject item = new JSONObject();
            item.put("item", entry.getKey());
            item.put("value", entry.getValue().booleanValue());
            array.put(item);
        }
        json.put("items", array);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
