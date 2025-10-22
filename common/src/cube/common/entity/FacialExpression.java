/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

import cube.common.JSONable;
import org.json.JSONObject;

/**
 * 面部表情。
 */
public class FacialExpression implements JSONable {

    public final FileLabel file;

    public final Expression expression;

    public final BoundingBox boundingBox;

    public FacialExpression(JSONObject json) {
        this.file = new FileLabel(json.getJSONObject("file"));
        this.expression = Expression.parse(json.getString("expression"));
        this.boundingBox = new BoundingBox(json.getJSONObject("bbox"));
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("file", this.file.toCompactJSON());
        json.put("expression", this.expression.name());
        json.put("bbox", this.boundingBox.toJSON());
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
