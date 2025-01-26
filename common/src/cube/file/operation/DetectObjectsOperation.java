/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.file.operation;

import cube.common.entity.DetectedObject;
import cube.file.ImageOperation;
import org.json.JSONObject;

import java.util.List;

/**
 * 检测图像里的对象操作。
 */
public class DetectObjectsOperation extends ImageOperation {

    public final static String Operation = "detectObject";

    private List<DetectedObject> detectedObjectList;

    public DetectObjectsOperation() {
        super();
    }

    public DetectObjectsOperation(JSONObject json) {

    }

    public void setDetectedObjects(List<DetectedObject> detectedObjects) {
        this.detectedObjectList = detectedObjects;
    }

    @Override
    public String getOperation() {
        return Operation;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();

        return json;
    }
}
