/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
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

    public static Point vector(Point startPoint, Point endPoint) {
        return new Point(endPoint.x - startPoint.x,
                endPoint.y - startPoint.y);
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

    public static double pointLocation(Point startPoint, Point endPoint, Point basePoint) {
        Point vector = Point.vector(startPoint, endPoint);
        Point sV = Point.vector(startPoint, basePoint);
        return vector.x * sV.y - vector.y * sV.x;
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
