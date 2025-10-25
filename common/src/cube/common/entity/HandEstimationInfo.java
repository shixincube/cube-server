/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

import cube.common.JSONable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 手势估算。
 */
public class HandEstimationInfo implements JSONable {

    public final String fileCode;

    public final long elapsed;

    public FileLabel fileLabel;

    private List<HandEstimation> handEstimations = new ArrayList<>();

    private FileLabel visualization;

    public HandEstimationInfo(JSONObject json) {
        this.fileCode = json.getString("fileCode");
        this.elapsed = json.getLong("elapsed");
        JSONArray keypoints = json.getJSONArray("keypoints");
        for (int i = 0; i < keypoints.length(); ++i) {
            JSONObject keypoint = keypoints.getJSONObject(i);
            HandEstimation handEstimation = new HandEstimation(keypoint);
            this.handEstimations.add(handEstimation);
        }
        if (json.has("visualization")) {
            this.visualization = new FileLabel(json.getJSONObject("visualization"));
        }
    }

    public void setFileLabel(FileLabel fileLabel) {
        this.fileLabel = fileLabel;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("fileCode", this.fileCode);
        json.put("elapsed", this.elapsed);

        if (null != this.fileLabel) {
            json.put("fileLabel", this.fileLabel.toJSON());
        }

        JSONArray array = new JSONArray();
        for (HandEstimation he : this.handEstimations) {
            array.put(he.toJSON());
        }
        json.put("keypoints", array);

        if (null != this.visualization) {
            json.put("visualization", this.visualization.toJSON());
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
