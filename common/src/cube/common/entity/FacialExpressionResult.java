/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

import cube.aigc.psychology.composition.Expression;
import cube.common.JSONable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 面部表情。
 */
public class FacialExpressionResult implements JSONable {

    public final FileLabel file;

    public final long elapsed;

    public final List<FacialExpression> list;

    public FacialExpressionResult(JSONObject json) {
        this.file = new FileLabel(json.getJSONObject("file"));
        this.elapsed = json.getLong("elapsed");
        this.list = new ArrayList<>();
        JSONArray array = json.getJSONArray("list");
        for (int i = 0; i < array.length(); ++i) {

        }
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
