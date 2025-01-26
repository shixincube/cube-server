/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.hub.signal;

import org.json.JSONObject;

/**
 * 进入静默状态。
 */
public class SilenceSignal extends Signal {

    public final static String NAME = "Silence";

    private long duration;

    public SilenceSignal(long duration) {
        super(NAME);
        this.duration = duration;
    }

    public SilenceSignal(JSONObject json) {
        super(json);
        this.duration = json.getLong("duration");
    }

    public long getDuration() {
        return this.duration;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("duration", this.duration);
        return json;
    }
}
