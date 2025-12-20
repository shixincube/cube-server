/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

import cube.aigc.ConsultationTheme;
import cube.aigc.psychology.Attribute;
import cube.common.JSONable;
import org.json.JSONObject;

public class CounselingStrategy implements JSONable {

    public int index;

    public Attribute attribute;

    public ConsultationTheme theme;

    public String streamName;

    public String content;

    public CounselingStrategy(int index, Attribute attribute, ConsultationTheme theme, String streamName,
                              String content) {
        this.index = index;
        this.attribute = attribute;
        this.theme = theme;
        this.streamName = streamName;
        this.content = content;
    }

    public CounselingStrategy(JSONObject json) {
        this.index = json.getInt("index");
        this.attribute = new Attribute(json.getJSONObject("attribute"));
        this.theme = ConsultationTheme.parse(json.getString("theme"));
        this.streamName = json.getString("streamName");
        this.content = json.getString("content");
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("index", this.index);
        json.put("attribute", this.attribute.toJSON());
        json.put("theme", this.theme.code);
        json.put("streamName", this.streamName);
        json.put("content", this.content);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
