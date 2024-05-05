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

import cube.common.JSONable;
import org.json.JSONObject;

/**
 * 实体盒子。
 */
public class Box implements JSONable {

    public int x0;

    public int y0;

    public int x1;

    public int y1;

    public int width;

    public int height;

    public Box(int x0, int y0, int x1, int y1) {
        this.x0 = x0;
        this.y0 = y0;
        this.x1 = x1;
        this.y1 = y1;
        this.width = x1 - x0;
        this.height = y1 - y0;
    }

    public Box(JSONObject json) {
        this.x0 = json.getInt("x0");
        this.y0 = json.getInt("y0");
        this.x1 = json.getInt("x1");
        this.y1 = json.getInt("y1");
        this.width = x1 - x0;
        this.height = y1 - y0;
    }

    public void refresh(Box box) {
        this.x0 = Math.min(this.x0, box.x0);
        this.y0 = Math.min(this.y0, box.y0);
        this.x1 = Math.max(this.x1, box.x1);
        this.y1 = Math.max(this.y1, box.y1);
        this.width = x1 - x0;
        this.height = y1 - y0;
    }

    public double getAspectRatio() {
        return ((double) this.width) / ((double) this.height);
    }

    public Point getCenterPoint() {
        return new Point((int) Math.round(((double)(this.x1 - this.x0)) * 0.5),
                (int) Math.round(((double)(this.y1 - this.y0)) * 0.5));
    }

    public boolean detectCollision(Box box) {
        int mX1 = Math.max(this.x0, box.x0);
        int mY1 = Math.max(this.y0, box.y0);
        int mX2 = Math.min(this.x1, box.x1);
        int mY2 = Math.min(this.y1, box.y1);
        int w = mX2 - mX1;
        int h = mY2 - mY1;
        if (w > 0 && h > 0) {
            return true;
        }
        else {
            return false;
        }
    }

    public int calculateCollisionArea(Box box) {
        int mX = Math.max(this.x0, box.x0);
        int mY = Math.max(this.y0, box.y0);
        int mX2 = Math.min(this.x1, box.x1);
        int mY2 = Math.min(this.y1, box.y1);
        int w = mX2 - mX;
        int h = mY2 - mY;
        if (w <= 0 || h <= 0) {
            return 0;
        }
        return w * h;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("(")
                .append(x0).append(",")
                .append(y0).append(",")
                .append(x1).append(",")
                .append(y1).append(")");
        return buf.toString();
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("x0", this.x0);
        json.put("y0", this.y0);
        json.put("x1", this.x1);
        json.put("y1", this.y1);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
