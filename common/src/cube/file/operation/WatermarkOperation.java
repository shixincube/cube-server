/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.file.operation;

import cube.common.entity.TextConstraint;
import cube.file.ImageOperation;
import org.json.JSONObject;

/**
 * 水印操作。
 */
public class WatermarkOperation extends ImageOperation {

    public final static String Operation = "Watermark";

    private String text;

    private TextConstraint constraint;

    public WatermarkOperation(String text, TextConstraint constraint) {
        this.text = text;
        this.constraint = constraint;
    }

    public WatermarkOperation(JSONObject json) {
        super(json);
        this.text = json.getString("text");
        this.constraint = new TextConstraint(json.getJSONObject("constraint"));
    }

    public String getText() {
        return this.text;
    }

    public TextConstraint getTextConstraint() {
        return this.constraint;
    }

    @Override
    public String getOperation() {
        return Operation;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("text", this.text);
        json.put("constraint", this.constraint.toJSON());
        return json;
    }
}
