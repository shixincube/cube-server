/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

import cube.aigc.psychology.composition.Expression;
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
        this.file = null;
        this.expression = null;
        this.boundingBox = null;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();

        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
