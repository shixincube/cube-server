/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.fileprocessor;

import cube.common.JSONable;
import cube.common.entity.BoundingBox;
import cube.common.entity.DetectedObject;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * CV 处理结果。
 */
public class CVResult implements JSONable {

    private String fileName;

    private List<DetectedObject> detectedObjects;

    public CVResult() {
    }

    public CVResult(String fileName) {
        this.fileName = fileName;
    }

    public void set(CVResult result) {
        this.fileName = result.fileName;
        this.detectedObjects = result.detectedObjects;
    }

    public List<DetectedObject> getDetectedObjects() {
        return this.detectedObjects;
    }

    public void setDetectedObjects(JSONArray array) {
        this.detectedObjects = new ArrayList<>();

        for (int i = 0; i < array.length(); ++i) {
            JSONObject json = array.getJSONObject(i);
            DetectedObject obj = new DetectedObject(json.getString("class"),
                    json.getDouble("probability"),
                    new BoundingBox(json.getJSONObject("bound")));
            this.detectedObjects.add(obj);
        }
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("fileName", this.fileName);

        JSONArray objects = new JSONArray();
        for (DetectedObject object : this.detectedObjects) {
            objects.put(object.toJSON());
        }
        json.put("detectedObjects", objects);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
