/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.aigc.psychology;

import cube.aigc.psychology.indicator.Indicable;
import cube.aigc.psychology.indicator.SRBCIndicator;
import cube.common.JSONable;
import cube.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ComprehensiveSection implements JSONable {

    public final String title;

    public final List<String> contents;

    public Indicable indicator;

    public ComprehensiveSection(String title, String content) {
        this(title, content, null);
    }

    public ComprehensiveSection(String title, String content, Indicable indicator) {
        this.title = title;
        this.contents = new ArrayList<>();
        this.contents.add(content);
        this.indicator = indicator;
    }

    public ComprehensiveSection(JSONObject json) {
        this.title = json.getString("title");
        JSONArray array = json.getJSONArray("contents");
        this.contents = JSONUtils.toStringList(array);
        if (json.has("indicator")) {
            String value = json.getString("indicator");
            this.indicator = SRBCIndicator.parse(value);
        }
    }

    public String getContent() {
        return this.contents.get(0);
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("title", this.title);
        json.put("contents", JSONUtils.toStringArray(this.contents));
        if (null != this.indicator) {
            json.put("indicator", this.indicator.getCode());
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
