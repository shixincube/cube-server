/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

import cube.common.JSONable;
import org.json.JSONObject;

public class VoiceSegment implements JSONable {

    public final double start;

    public final double end;

    public final double duration;

    public VoiceSegment(JSONObject json) {
        this.start = json.getDouble("start");
        this.end = json.getDouble("end");
        this.duration = json.getDouble("duration");
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("start", this.start);
        json.put("end", this.end);
        json.put("duration", this.duration);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
