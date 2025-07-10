/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

import cell.util.Utils;
import cube.aigc.psychology.composition.Palette;
import cube.aigc.psychology.composition.Texture;
import cube.common.JSONable;
import cube.vision.BoundingBox;
import cube.vision.Box;
import org.json.JSONObject;

/**
 * 图画里的素材描述。
 */
public class Material implements JSONable {

    public long sn;

    public String label;

    public double prob;

    public BoundingBox boundingBox;

    public Box box;

    public int area;

    public String color;

    public Texture texture;

    public Palette palette;

    public Material(String label) {
        this.sn = Utils.generateSerialNumber();
        this.label = label;
        this.palette = new Palette();
    }

    public Material(Material other) {
        this.sn = other.sn;
        this.label = other.label;
        this.prob = other.prob;
        this.boundingBox = other.boundingBox;
        this.box = other.box;
        this.area = other.area;
        this.color = other.color;
        this.texture = other.texture;
        this.palette = other.palette;
    }

    public Material(String label, BoundingBox boundingBox, Box box) {
        this.sn = Utils.generateSerialNumber();
        this.label = label;
        this.boundingBox = boundingBox;
        this.box = box;
        this.area = Math.round(boundingBox.width * boundingBox.height * 0.76f);
        this.prob = 0.8;
        this.color = "#FF0000";
        this.texture = new Texture();
        this.palette = new Palette();
    }

    public Material(JSONObject json) {
        if (json.has("sn")) {
            this.sn = json.getLong("sn");
        } else {
            this.sn = Utils.generateSerialNumber();
        }

        this.label = json.getString("label");
        this.prob = json.getDouble("prob");
        this.boundingBox = new BoundingBox(json.getJSONObject("bbox"));

        if (json.has("box")) {
            this.box = new Box(json.getJSONObject("box"));
        }
        else {
            this.box = new Box(this.boundingBox.x, this.boundingBox.y,
                    this.boundingBox.getX2(), this.boundingBox.getY2());
        }

        this.area = json.getInt("area");
        this.color = json.getString("color");

        if (json.has("texture")) {
            this.texture = new Texture(json.getJSONObject("texture"));
        }
        else {
            this.texture = new Texture();
        }

        if (json.has("palette")) {
            this.palette = new Palette(json.getJSONObject("palette"));
        }
        else {
            this.palette = new Palette();
        }
    }

    public boolean isDoodle() {
        // 涂鸦特征：轮廓密度高，层密度低
        if (this.texture.max > 0 && this.texture.hierarchy > 0) {
            // 判断最大值
            if (this.texture.avg > 2.0) {
                // 判断标准差和层密度
                if (this.texture.standardDeviation >= 0.4 && this.texture.hierarchy <= 0.03) {
                    return true;
                }
            }
            else if (this.texture.max >= 1.0 && this.texture.max < 2.0) {
                if (this.texture.hierarchy <= 0.03 && this.texture.density >= 0.3 && this.texture.density < 0.99) {
                    return true;
                }
//            else if (this.texture.density >= 0.5) {
//                return true;
//            }
            }
        }
        return false;
    }

    /**
     * 返回两个 Material 间的最短距离。
     *
     * @param other
     * @return 如果返回负数表示与指定的 Material 发生碰撞，没有间距，返回的值是碰撞面积。
     */
    public int distance(Material other) {
        int area = this.box.calculateCollisionArea(other.box);
        if (area > 0) {
            return -area;
        }

        int minDist = -1;

        // 计算中心点
        int c1x = (int) Math.round(this.box.x0 + (this.box.width * 0.5));
        int c1y = (int) Math.round(this.box.y0 + (this.box.height * 0.5));
        int c2x = (int) Math.round(other.box.x0 + (other.box.width * 0.5));
        int c2y = (int) Math.round(other.box.y0 + (other.box.height * 0.5));
        int dx = Math.abs(c2x - c1x);
        int dy = Math.abs(c2y - c1y);

        if ((dx < ((this.box.width + other.box.width) * 0.5)) && (dy >= ((this.box.height + other.box.height) * 0.5))) {
            // 两矩形不相交，在X轴方向有部分重合的两个矩形，最小距离是上矩形的下边线与下矩形的上边线之间的距离
            minDist = dy - (int) Math.round((this.box.height + other.box.height) * 0.5);
        }
        else if ((dx >= ((this.box.width + other.box.width) * 0.5)) && (dy < ((this.box.height + other.box.height) * 0.5))) {
            // 两矩形不相交，在Y轴方向有部分重合的两个矩形，最小距离是左矩形的右边线与右矩形的左边线之间的距离
            minDist = dx - (int) Math.round((this.box.width + other.box.width) * 0.5);
        }
        else if ((dx >= ((this.box.width + other.box.width) * 0.5)) && (dy >= ((this.box.height + other.box.height) * 0.5))) {
            // 两矩形不相交，在X轴和Y轴方向无重合的两个矩形，最小距离是距离最近的两个顶点之间的距离，使用勾股定理
            double deltaX = dx - ((this.box.width + other.box.width) * 0.5);
            double deltaY = dy - ((this.box.height + other.box.height) * 0.5);
            minDist = (int) Math.round(Math.sqrt(deltaX * deltaX + deltaY * deltaY));
        }
        else {
            minDist = 0;
        }

        return minDist;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("sn", this.sn);
        json.put("label", this.label);
        json.put("prob", this.prob);
        json.put("bbox", this.boundingBox.toJSON());
        json.put("box", this.box.toJSON());
        json.put("area", this.area);
        json.put("color", this.color);
        json.put("texture", this.texture.toJSON());
        json.put("palette", this.palette.toJSON());
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
