/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.file.operation;

import cube.common.entity.TextConstraint;
import cube.file.ImageOperation;
import cube.util.TextUtils;
import cube.vision.Size;
import org.json.JSONObject;

/**
 * 隐写
 */
public class SteganographyOperation extends ImageOperation {

    public final static String Operation = "Steganography";

    private boolean recover = false;

    private String hiddenText;

    private Size watermarkSize;

    private TextConstraint textConstraint;

    public SteganographyOperation(String text) {
        this.hiddenText = text;
        this.watermarkSize = TextUtils.measureTextAreas(text, new TextConstraint(),
                2, 10, 1024, 1024);
    }

    public SteganographyOperation(Size watermarkSize) {
        this.recover = true;
        this.watermarkSize = watermarkSize;
    }

    public SteganographyOperation(JSONObject json) {
        super(json);

        this.recover = json.getBoolean("recover");
        this.watermarkSize = new Size(json.getJSONObject("watermarkSize"));

        if (json.has("hiddenText")) {
            this.hiddenText = json.getString("hiddenText");
        }

        if (json.has("textConstraint")) {
            this.textConstraint = new TextConstraint(json.getJSONObject("textConstraint"));
        }
    }

    public boolean isRecover() {
        return this.recover;
    }

    public void setHiddenText(String hiddenText) {
        this.hiddenText = hiddenText;
    }

    public String getHiddenText() {
        return this.hiddenText;
    }

    public void setWatermarkSize(Size size) {
        this.watermarkSize = size;
    }

    public Size getWatermarkSize() {
        return this.watermarkSize;
    }

    public void setTextConstraint(TextConstraint textConstraint) {
        this.textConstraint = textConstraint;
    }

    public TextConstraint getTextConstraint() {
        return this.textConstraint;
    }

    @Override
    public String getOperation() {
        return Operation;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();

        json.put("recover", this.recover);
        json.put("watermarkSize", this.watermarkSize.toJSON());

        if (null != this.hiddenText) {
            json.put("hiddenText", this.hiddenText);
        }

        if (null != this.textConstraint) {
            json.put("textConstraint", this.textConstraint.toJSON());
        }

        return json;
    }
}
