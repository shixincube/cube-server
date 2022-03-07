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
 * 替换图像颜色。
 */
public class ReplaceColorOperation extends ImageOperation {

    public final static String Operation = "ReplaceColor";

    private Color targetColor;

    private Color replaceColor;

    private int fuzzFactor = 20;

    public ReplaceColorOperation(Color targetColor, Color replaceColor) {
        this.targetColor = targetColor;
        this.replaceColor = replaceColor;
    }

    public ReplaceColorOperation(Color targetColor, Color replaceColor, int fuzzFactor) {
        this.targetColor = targetColor;
        this.replaceColor = replaceColor;
        this.fuzzFactor = fuzzFactor;
    }

    public ReplaceColorOperation(JSONObject json) {
        super(json);

        this.targetColor = new Color(json.getJSONObject("target"));
        this.replaceColor = new Color(json.getJSONObject("replace"));
        this.fuzzFactor = json.getInt("fuzz");
    }

    public Color getTargetColor() {
        return this.targetColor;
    }

    public Color getReplaceColor() {
        return this.replaceColor;
    }

    public void setFuzzFactor(int fuzzFactor) {
        this.fuzzFactor = fuzzFactor;
    }

    public int getFuzzFactor() {
        return this.fuzzFactor;
    }

    @Override
    public String getOperation() {
        return Operation;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();

        json.put("target", this.targetColor.toJSON());
        json.put("replace", this.replaceColor.toJSON());
        json.put("fuzz", this.fuzzFactor);

        return json;
    }
}
