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

package cube.vision;

import org.json.JSONObject;

/**
 * 颜色描述。
 */
public class Color {

    private int red;

    private int green;

    private int blue;

    private int color = -1;

    public Color() {
    }

    public Color(int red, int green, int blue) {
        this.red = Math.min(red, 255);
        this.green = Math.min(green, 255);
        this.blue = Math.min(blue, 255);
    }

    public Color(String desc) {
        if (desc.startsWith("#")) {
            this.red = Integer.parseInt(desc.substring(1, 3), 16);
            this.green = Integer.parseInt(desc.substring(3, 5), 16);
            this.blue = Integer.parseInt(desc.substring(5, 7), 16);
        }
        else if (desc.startsWith("rgb")) {
            // TODO
        }
        else if (desc.startsWith("rgba")) {
            // TODO
        }
    }

    public Color(JSONObject json) {
        this.red = json.getInt("r");
        this.green = json.getInt("g");
        this.blue = json.getInt("b");
    }

    public int red() {
        return this.red;
    }

    public int green() {
        return this.green;
    }

    public int blue() {
        return this.blue;
    }

    public int color() {
        if (this.color == -1) {
            int r = (this.red << 16) & 0x00FF0000;
            int g = (this.green << 8) & 0x0000FF00;
            int b = this.blue & 0x000000FF;
            this.color = (r | g | b);
        }

        return this.color;
    }

    public String formatRGB() {
        StringBuilder buf = new StringBuilder("rgb(");
        buf.append(this.red);
        buf.append(",");
        buf.append(this.green);
        buf.append(",");
        buf.append(this.blue);
        buf.append(")");
        return buf.toString();
    }

    public String formatHex() {
        StringBuilder buf = new StringBuilder("#");
        String redHex = Integer.toHexString(this.red);
        buf.append(redHex.length() == 1 ? "0" + redHex : redHex);
        String greenHex = Integer.toHexString(this.green);
        buf.append(greenHex.length() == 1 ? "0" + greenHex : greenHex);
        String blueHex = Integer.toHexString(this.blue);
        buf.append(blueHex.length() == 1 ? "0" + blueHex : blueHex);
        return buf.toString();
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("r", this.red);
        json.put("g", this.green);
        json.put("b", this.blue);
        return json;
    }
}
