/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.file.operation;

import cube.file.ImageOperation;
import org.json.JSONObject;

/**
 * 亮度对比度操作。
 */
public class BrightnessOperation extends ImageOperation {

    public final static String Operation = "Brightness";

    private int brightness;

    private int contrast;

    public BrightnessOperation(int brightness, int contrast) {
        this.brightness = brightness;
        this.contrast = contrast;
    }

    public BrightnessOperation(JSONObject json) {
        super(json);
        this.brightness = json.getInt("brightness");
        this.contrast = json.getInt("contrast");
    }

    public int getBrightness() {
        return this.brightness;
    }

    public int getContrast() {
        return this.contrast;
    }

    @Override
    public String getOperation() {
        return Operation;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("brightness", this.brightness);
        json.put("contrast", this.contrast);
        return json;
    }
}
