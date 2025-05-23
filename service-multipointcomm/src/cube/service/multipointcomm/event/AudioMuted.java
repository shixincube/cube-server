/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.multipointcomm.event;

import cube.common.JSONable;
import org.json.JSONObject;

/**
 * 音频通道静音。
 */
public class AudioMuted implements JSONable {

    public final static String NAME = "AudioMuted";

    private JSONObject data;

    public AudioMuted(JSONObject data) {
        this.data = data;
    }

    @Override
    public JSONObject toJSON() {
        return this.data;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.data;
    }

    public static boolean isMuted(JSONObject data) {
        return data.getBoolean("value");
    }
}
