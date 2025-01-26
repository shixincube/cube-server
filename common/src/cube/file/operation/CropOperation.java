/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.file.operation;

import cube.file.ImageOperation;
import cube.vision.Rectangle;
import org.json.JSONObject;

/**
 * 剪裁图像。
 */
public class CropOperation extends ImageOperation {

    public final static String Operation = "Crop";

    private Rectangle cropRect;

    public CropOperation(int x, int y, int width, int height) {
        super();
        this.cropRect = new Rectangle(x, y, width, height);
    }

    public CropOperation(Rectangle rect) {
        super();
        this.cropRect = rect;
    }

    public CropOperation(JSONObject json) {
        super(json);
        this.cropRect = new Rectangle(json.getJSONObject("rect"));
    }

    public void setCropRect(Rectangle rect) {
        this.cropRect = rect;
    }

    public Rectangle getCropRect() {
        return this.cropRect;
    }

    @Override
    public String getOperation() {
        return Operation;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();

        json.put("rect", this.cropRect.toJSON());

        return json;
    }
}
