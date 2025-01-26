/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.multipointcomm.event;

import cube.common.JSONable;
import org.json.JSONObject;

/**
 * 麦克风音量。
 */
public class MicrophoneVolume implements JSONable {

    public final static String NAME = "MicrophoneVolume";

    private JSONObject data;

    public MicrophoneVolume(JSONObject data) {
        this.data = data;
    }

    public int getVolume() {
        return this.data.getInt("value");
    }

    public long getTimestamp() {
        return this.data.getLong("timestamp");
    }

    @Override
    public JSONObject toJSON() {
        return this.data;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.data;
    }

    /**
     * 获取事件数据里的音量值。
     *
     * @param data
     * @return
     */
    public static int getVolume(JSONObject data) {
        return data.getInt("value");
    }
}
