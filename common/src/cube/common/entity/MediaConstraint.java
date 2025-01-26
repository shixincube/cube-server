/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

import cube.common.JSONable;
import org.json.JSONObject;

/**
 * 媒体约束描述。
 */
public class MediaConstraint implements JSONable {

    private final boolean video;

    private final boolean audio;

    private final JSONObject dimension;

    public MediaConstraint() {
        this.video = true;
        this.audio = true;
        this.dimension = new JSONObject();
        this.dimension.put("width", 640);
        this.dimension.put("height", 480);
        this.dimension.put("constraints", new JSONObject("{ \"width\": { \"exact\": 640 }, \"height\": { \"exact\": 480 } }"));
    }

    public MediaConstraint(JSONObject json) {
        this.video = json.getBoolean("video");
        this.audio = json.getBoolean("audio");
        this.dimension = json.getJSONObject("dimension");
    }

    public boolean videoEnabled() {
        return this.video;
    }

    public boolean audioEnabled() {
        return this.audio;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("video", this.video);
        json.put("audio", this.audio);
        json.put("dimension", this.dimension);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
