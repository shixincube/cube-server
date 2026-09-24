/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

import cube.common.JSONable;
import cube.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 多模态输入。
 */
public class MultimodalInput implements JSONable {

    public String unit;

    public String content;

    public List<JSONObject> history;

    public List<String> fileCodes;

    public List<FileLabel> fileLabels;

    public List<String> streams;

    public JSONObject option;

    public boolean recordable = false;

    public MultimodalInput(JSONObject json) {
        this.unit = json.has("unit") ? json.getString("unit") : "OmniGround";
        this.content = json.getString("content");
        this.history = json.has("history") ? JSONUtils.toObjectList(json.getJSONArray("history")) : null;
        this.fileCodes = json.has("files") ? JSONUtils.toStringList(json.getJSONArray("files")) : new ArrayList<>();
        this.option = json.has("option") ? json.getJSONObject("option") : null;
        this.recordable = json.has("recordable") && json.getBoolean("recordable");
    }

    public JSONObject toUnitJson() {
        JSONObject json = new JSONObject();
        json.put("unit", this.unit);
        json.put("content", this.content);

        if (null != this.history) {
            JSONArray array = JSONUtils.toObjectArray(this.history);
            json.put("history", array);
        }

        if (null != this.fileLabels) {
            JSONArray files = new JSONArray();
            for (FileLabel fileLabel : this.fileLabels) {
                files.put(fileLabel.toJSON());
            }
            json.put("files", files);
        }
        else if (null != this.fileCodes) {
            json.put("files", JSONUtils.toStringArray(this.fileCodes));
        }
        else {
            json.put("files", new JSONArray());
        }

        if (null != this.option) {
            json.put("option", this.option);
        }

        return json;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("unit", this.unit);
        json.put("content", this.content);

        if (null != this.history) {
            JSONArray array = JSONUtils.toObjectArray(this.history);
            json.put("history", array);
        }

        if (null != this.fileCodes) {
            json.put("files", JSONUtils.toStringArray(this.fileCodes));
        }
        else {
            json.put("files", new JSONArray());
        }

        if (null != this.option) {
            json.put("option", this.option);
        }
        json.put("recordable", this.recordable);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
