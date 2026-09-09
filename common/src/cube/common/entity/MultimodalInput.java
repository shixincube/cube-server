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

import java.util.List;

/**
 * 多模态输入。
 */
public class MultimodalInput implements JSONable {

    public String unit;

    public String content;

    public List<String> files;

    public List<FileLabel> fileLabels;

    public MultimodalTask task;

    public JSONObject option;

    public int histories = 0;

    public boolean recordable = false;

//    public double temperature = 0.3;
//    public double topP = 0.95;
//    public double repetitionPenalty = 1.3;
//    public int topK = 10;
//    public int maxNewTokens = 2048;

//    public MultimodalInput(String unit, String content, List<FileLabel> fileLabels, MultimodalTask task) {
//        this.unit = unit;
//        this.content = content;
//        this.fileLabels = fileLabels;
//        this.task = task;
//    }

    public MultimodalInput(JSONObject json) {
        this.unit = json.has("unit") ? json.getString("unit") : "OmniGround";
        this.content = json.getString("content");
        this.files = JSONUtils.toStringList(json.getJSONArray("files"));
        this.task = json.has("task") ? new MultimodalTask(json.getJSONObject("task")) : null;
        this.option = json.has("option") ? json.getJSONObject("option") : null;
        this.histories = json.has("histories") ? json.getInt("histories") : 0;
        this.recordable = json.has("recordable") && json.getBoolean("recordable");
    }

    public JSONObject toUnitJson() {
        JSONObject json = new JSONObject();
        json.put("unit", this.unit);
        json.put("content", this.content);

        if (null != this.fileLabels) {
            JSONArray files = new JSONArray();
            for (FileLabel fileLabel : this.fileLabels) {
                files.put(fileLabel.toJSON());
            }
            json.put("files", files);
        }
        else if (null != this.files) {
            json.put("files", JSONUtils.toStringArray(this.files));
        }
        else {
            json.put("files", new JSONArray());
        }

        if (null != this.task) {
            json.put("task", this.task.toJSON());
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
        json.put("files", JSONUtils.toStringArray(this.files));
        if (null != this.task) {
            json.put("task", this.task.toJSON());
        }
        if (null != this.option) {
            json.put("option", this.option);
        }
        json.put("histories", this.histories);
        json.put("recordable", this.recordable);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
