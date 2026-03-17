/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.aigc.psychology;

import cube.common.JSONable;
import cube.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ComprehensiveSection implements JSONable {

    public final String title;

    public final List<String> contents;

    public ComprehensiveSection(String title, String content) {
        this.title = title;
        this.contents = new ArrayList<>();
        this.contents.add(content);
    }

    public ComprehensiveSection(JSONObject json) {
        this.title = json.getString("title");
        JSONArray array = json.getJSONArray("contents");
        this.contents = JSONUtils.toStringList(array);
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("title", this.title);
        json.put("contents", JSONUtils.toStringArray(this.contents));
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
