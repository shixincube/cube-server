/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.file.operation;

import cube.file.ImageOperation;
import org.json.JSONObject;

/**
 * 反转颜色。
 */
public class ReverseColorOperation extends ImageOperation {

    public final static String Operation = "ReverseColor";

    public ReverseColorOperation() {
    }

    public ReverseColorOperation(JSONObject json) {
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
