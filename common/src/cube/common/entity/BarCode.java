/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

import cube.vision.Rectangle;
import org.json.JSONObject;

/**
 * 条形码。
 */
public class BarCode extends Entity {

    public static Container Ultra = new Container(500, 250, 32);

    public static Container Normal = new Container(252, 126, 16);

    public static Container Small = new Container(200, 100, 13);

    public String data;

    public String type = "CODE128";

    public Rectangle rect;

    public int width;

    public int height;

    public String header;

    public String footer;

    public int fontSize;

    public BarCode(JSONObject json) {
        super(json);
        this.data = json.getString("data");
        if (json.has("type")) {
            this.type = json.getString("type");
        }
        if (json.has("rect")) {
            this.rect = new Rectangle(json.getJSONObject("rect"));
        }

        this.width = json.has("width") ? json.getInt("width") : 500;
        this.height = json.has("height") ? json.getInt("height") : 250;
        this.header = json.has("header") ? json.getString("header") : null;
        this.footer = json.has("footer") ? json.getString("footer") : null;
        this.fontSize = json.has("fontSize") ? json.getInt("fontSize") : 32;
    }

    public void setContainer(Container container) {
        this.width = container.width;
        this.height = container.height;
        this.fontSize = container.fontSize;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("data", this.data);
        json.put("type", this.type);

        if (null != this.rect) {
            json.put("rect", this.rect.toJSON());
        }

        json.put("width", this.width);
        json.put("height", this.height);

        if (null != this.header) {
            json.put("header", this.header);
        }
        if (null != this.footer) {
            json.put("footer", this.footer);
        }

        json.put("fontSize", this.fontSize);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = super.toCompactJSON();
        json.put("data", this.data);
        json.put("type", this.type);
        if (null != this.rect) {
            json.put("rect", this.rect.toJSON());
        }
        return json;
    }

    public static Container parseContainer(String text) {
        if (text.equalsIgnoreCase("Ultra")) {
            return BarCode.Ultra;
        }
        else if (text.equalsIgnoreCase("Normal")) {
            return BarCode.Normal;
        }
        else {
            return BarCode.Small;
        }
    }

    public static class Container {

        public int width;

        public int height;

        public int fontSize;

        public Container(int width, int height, int fontSize) {
            this.width = width;
            this.height = height;
            this.fontSize = fontSize;
        }
    }
}
