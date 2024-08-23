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
 * 点描述。
 */
public class Point implements JSONable {

    public double x;

    public double y;

    public Point(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public Point(JSONObject json) {
        this.x = json.getDouble("x");
        this.y = json.getDouble("y");
    }

    public double distance(Point other) {
        return Math.sqrt((other.x - x) * (other.x - x) + (other.y - y) * (other.y - y));
    }

    public Point scale(double value) {
        return new Point(this.x * value, this.y * value);
    }

    /**
     * 返回多边形质心。
     *
     * @param points 多边形各个点。
     * @return 返回多边形质心。
     */
    public static Point getCentroid(Point[] points) {
        double sumX = 0;
        double sumY = 0;

        for (Point p : points) {
            sumX += p.x;
            sumY += p.y;
        }

        return new Point(sumX / (double) points.length, sumY / (double) points.length);
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(this.x);
        buf.append(",");
        buf.append(this.y);
        return buf.toString();
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("x", this.x);
        json.put("y", this.y);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
