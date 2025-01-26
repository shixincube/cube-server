/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

import cube.common.JSONable;
import cube.vision.Color;
import org.json.JSONObject;

/**
 * 文本约束。
 */
public class TextConstraint implements JSONable {

    public String font = null;

    public int pointSize = 24;

    public Color color = new Color(0, 0, 0);

    public TextConstraint() {
    }

    public TextConstraint(JSONObject json) {
        this.pointSize = json.getInt("pointSize");
        this.color = new Color(json.getJSONObject("color"));
        if (json.has("font")) {
            this.font = json.getString("font");
        }
    }

    public TextConstraint setFont(String font) {
        this.font = font;
        return this;
    }

    public TextConstraint setPointSize(int size) {
        this.pointSize = size;
        return this;
    }

    public TextConstraint setColor(Color color) {
        this.color = color;
        return this;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("pointSize", this.pointSize);
        json.put("color", this.color.toJSON());
        if (null != this.font) {
            json.put("font", this.font);
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
