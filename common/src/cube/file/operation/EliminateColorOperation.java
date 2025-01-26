/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.file.operation;

import cube.file.ImageOperation;
import cube.vision.Color;
import org.json.JSONObject;

/**
 * 剔除图像颜色。
 */
public class EliminateColorOperation extends ImageOperation {

    public final static String Operation = "EliminateColor";

    private Color reservedColor;

    private Color fillColor;

    public EliminateColorOperation(Color reservedColor, Color fillColor) {
        this.reservedColor = reservedColor;
        this.fillColor = fillColor;
    }

    public EliminateColorOperation(JSONObject json) {
        super(json);

        this.reservedColor = new Color(json.getJSONObject("reserved"));
        this.fillColor = new Color(json.getJSONObject("fill"));
    }

    @Override
    public String getOperation() {
        return Operation;
    }

    public Color getReservedColor() {
        return this.reservedColor;
    }

    public Color getFillColor() {
        return this.fillColor;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();

        json.put("reserved", this.reservedColor.toJSON());
        json.put("fill", this.fillColor.toJSON());

        return json;
    }
}
