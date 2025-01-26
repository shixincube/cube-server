/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.file.operation;

import cube.file.ImageOperation;
import org.json.JSONObject;

/**
 * 图片覆盖水印。
 */
public class CoverWatermarkOperation extends ImageOperation {

    public final static String Operation = "CoverWatermark";

    public CoverWatermarkOperation() {
        super();
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
