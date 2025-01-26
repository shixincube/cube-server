/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.file.operation;

import cube.file.ImageOperation;
import org.json.JSONObject;

/**
 * 灰度图操作。
 */
public class GrayscaleOperation extends ImageOperation {

    public final static String Operation = "Grayscale";

    public GrayscaleOperation() {
        super();
    }

    public GrayscaleOperation(JSONObject json) {
        super(json);
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
