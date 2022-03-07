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

    private String outputFilename;

    public EliminateColorOperation(Color reservedColor, Color fillColor) {
        this.reservedColor = reservedColor;
        this.fillColor = fillColor;
    }

    public EliminateColorOperation(JSONObject json) {
        this.reservedColor = new Color(json.getJSONObject("reserved"));
        this.fillColor = new Color(json.getJSONObject("fill"));
        if (json.has("outputFilename")) {
            this.outputFilename = json.getString("outputFilename");
        }
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

    public void setOutputFilename(String outputFilename) {
        this.outputFilename = outputFilename;
    }

    public String getOutputFilename() {
        return this.outputFilename;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("reserved", this.reservedColor.toJSON());
        json.put("fill", this.fillColor.toJSON());
        if (null != this.outputFilename) {
            json.put("outputFilename", this.outputFilename);
        }
        return json;
    }
}
