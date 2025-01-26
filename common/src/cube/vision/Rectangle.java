/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
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
