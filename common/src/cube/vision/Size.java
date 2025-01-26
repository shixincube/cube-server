/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.vision;

import cube.common.JSONable;
import org.json.JSONObject;

/**
 * 大小描述。
 */
public class Size implements JSONable {

    public int width;

    public int height;

    public Size() {
    }

    public Size(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public Size(JSONObject json) {
        this.width = json.getInt("width");
        this.height = json.getInt("height");
    }

    /**
     * 计算面积。
     *
     * @return
     */
    public int calculateArea() {
        return this.width * this.height;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(this.width);
        buf.append("x");
        buf.append(this.height);
        return buf.toString();
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("width", this.width);
        json.put("height", this.height);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }

    /**
     * 从字符串形式解析数据。
     *
     * @param string
     * @return
     */
    public static Size parse(String string) {
        String[] data = string.split("x");
        if (data.length == 2) {
            return new Size(Integer.parseInt(data[0]), Integer.parseInt(data[1]));
        }

        return null;
    }
}
