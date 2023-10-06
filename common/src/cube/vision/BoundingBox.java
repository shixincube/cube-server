/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 Cube Team.
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
 * 边界盒。
 */
public class BoundingBox implements JSONable {

    public int x;

    public int y;

    public int width;

    public int height;

    public BoundingBox() {
        this(0, 0, 1, 1);
    }

    public BoundingBox(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public BoundingBox(JSONObject json) {
        this.x = json.getInt("x");
        this.y = json.getInt("y");
        this.width = json.getInt("width");
        this.height = json.getInt("height");
    }

    public int getX2() {
        return this.x + this.width;
    }

    public int getY2() {
        return this.y + this.height;
    }

    /**
     * 对点进行碰撞检测。
     *
     * @param x
     * @param y
     * @return
     */
    public boolean detectCollision(int x, int y) {
        return (x > this.x && y > this.y && x < this.x + this.width && y < this.y + this.height);
    }

    /**
     * 计算面积。
     *
     * @return
     */
    public int calculateArea() {
        return this.width * this.height;
    }

    /**
     * 将指定的边界盒与当前边界盒进行空间合并。
     *
     * @param bbox
     * @return
     */
    public BoundingBox merge(BoundingBox bbox) {
        int x = Math.min(bbox.x, this.x);
        int y = Math.min(bbox.y, this.y);
        int x2 = Math.max(bbox.x + bbox.width, this.x + this.width);
        int y2 = Math.max(bbox.y + bbox.height, this.y + this.height);
        int width = x2 - x;
        int height = y2 - y;

        return new BoundingBox(x, y, width, height);
    }

    /**
     * 计算与指定边界盒的碰撞面积。
     *
     * @param box
     * @return
     */
    public int calculateCollisionArea(BoundingBox box) {
        int mX = Math.max(this.x, box.x);
        int mY = Math.max(this.y, box.y);
        int mX2 = Math.min(this.getX2(), box.getX2());
        int mY2 = Math.min(this.getY2(), box.getY2());
        int w = mX2 - mX;
        int h = mY2 - mY;
        if (w <= 0 || h <= 0) {
            return 0;
        }
        return w * h;
    }

    @Override
    public JSONObject toJSON() {
        return this.toCompactJSON();
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = new JSONObject();
        json.put("x", this.x);
        json.put("y", this.y);
        json.put("width", this.width);
        json.put("height", this.height);
        return json;
    }
}
