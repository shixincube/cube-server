/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.guidance;

import cell.util.log.Logger;
import cube.common.JSONable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;

public class EvaluationResult implements JSONable {

    public String name;

    private JSONObject result;

    private Map<String, Boolean> evaluationItems;

    private String description;

    private Map<String, String> qaPairs;

    private boolean terminated = false;

    public EvaluationResult(String name) {
        this.name = name;
        this.evaluationItems = new LinkedHashMap<>();
        this.qaPairs = new LinkedHashMap<>();
    }

    public EvaluationResult(JSONObject json) {
        this.name = json.getString("name");
        this.result = json.has("result") ? json.getJSONObject("result") : null;
        this.evaluationItems = new LinkedHashMap<>();
        JSONArray array = json.getJSONArray("items");
        for (int i = 0; i < array.length(); ++i) {
            JSONObject data = array.getJSONObject(i);
            this.evaluationItems.put(data.getString("item"), data.getBoolean("value"));
        }
        this.description = json.has("description") ? json.getString("description") : null;
        this.qaPairs = new LinkedHashMap<>();
    }

    public boolean isValid() {
        return (null != this.result) || (null != this.description) || !this.evaluationItems.isEmpty();
    }

    public void setTerminated(boolean value) {
        this.terminated = value;
    }

    public boolean hasTerminated() {
        return this.terminated;
    }

    public void setResult(Object value) {
        this.result = new JSONObject();
        this.result.put("value", value.toString());
    }

    public void setDescription(String description) {
        this.description = description;
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

        if (null != this.result && this.result.has("value")) {
            buf.append("- ***");
            String value = this.result.getString("value");
            try {
                if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
                    boolean boolValue = Boolean.parseBoolean(value.toLowerCase());
                    buf.append(boolValue ? "是" : "否");
                }
                else {
                    buf.append(value);
                }
            } catch (Exception e) {
                Logger.e(this.getClass(), "#toMarkdown", e);
            }
            buf.append("***").append("\n\n");
        }

        for (Map.Entry<String, Boolean> entry : this.evaluationItems.entrySet()) {
            buf.append("> ").append(entry.getKey());
            buf.append("    ").append(entry.getValue() ? "是" : "否");
            buf.append("\n");
        }

        if (!this.evaluationItems.isEmpty()) {
            buf.append("\n");
        }

        if (null != this.description) {
            buf.append(this.description);
        }

        return buf.toString().trim();
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("name", this.name);
        if (null != this.result) {
            json.put("result", this.result);
        }
        JSONArray array = new JSONArray();
        for (Map.Entry<String, Boolean> entry : this.evaluationItems.entrySet()) {
            JSONObject item = new JSONObject();
            item.put("item", entry.getKey());
            item.put("value", entry.getValue().booleanValue());
            array.put(item);
        }
        json.put("items", array);
        if (null != this.description) {
            json.put("description", this.description);
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
