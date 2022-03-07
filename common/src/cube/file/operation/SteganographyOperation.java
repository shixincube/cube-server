/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 Cube Team.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
