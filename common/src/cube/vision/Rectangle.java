/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 Ambrose Xu.
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
 * 矩形描述。
 */
public class Rectangle extends BoundingBox {

    public Rectangle(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    public Rectangle(JSONObject json) {
        super(json);
    }

    public Rectangle(Rectangle rectangle) {
        super(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(this.width);
        buf.append("x");
        buf.append(this.height);
        buf.append("+");
        buf.append(this.x);
        buf.append("+");
        buf.append(this.y);
        return buf.toString();
    }

    /**
     * 解析字符串形式。
     *
     * @param string
     * @return
     */
    public static Rectangle parse(String string) {
        String[] seg = string.split("\\+");
        if (seg.length == 3) {
            String[] wh = seg[0].split("x");
            if (wh.length == 2) {
                return new Rectangle(Integer.parseInt(seg[1]), Integer.parseInt(seg[2]),
                        Integer.parseInt(wh[0]), Integer.parseInt(wh[1]));
            }
        }

        return null;
    }
}
