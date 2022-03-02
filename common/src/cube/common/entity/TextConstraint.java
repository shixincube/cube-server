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

package cube.common.entity;

import cube.common.JSONable;
import cube.vision.Color;
import org.json.JSONObject;

/**
 * 文本约束。
 */
public class TextConstraint implements JSONable {

    public String font = null;

    public int pointSize = 24;

    public Color color = new Color(0, 0, 0);

    public TextConstraint() {
    }

    public TextConstraint(JSONObject json) {
        this.pointSize = json.getInt("pointSize");
        this.color = new Color(json.getJSONObject("color"));
        if (json.has("font")) {
            this.font = json.getString("font");
        }
    }

    public TextConstraint setFont(String font) {
        this.font = font;
        return this;
    }

    public TextConstraint setPointSize(int size) {
        this.pointSize = size;
        return this;
    }

    public TextConstraint setColor(Color color) {
        this.color = color;
        return this;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("pointSize", this.pointSize);
        json.put("color", this.color.toJSON());
        if (null != this.font) {
            json.put("font", this.font);
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
