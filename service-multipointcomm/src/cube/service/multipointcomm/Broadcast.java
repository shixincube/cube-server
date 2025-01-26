/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.multipointcomm;

import cube.common.JSONable;
import cube.common.entity.CommField;
import cube.common.entity.CommFieldEndpoint;
import cube.service.multipointcomm.event.AudioMuted;
import cube.service.multipointcomm.event.MicrophoneVolume;
import cube.service.multipointcomm.event.VideoMuted;
import org.json.JSONObject;

/**
 * 广播数据。
 */
public class Broadcast implements JSONable {

    private CommField commField;

    private CommFieldEndpoint source;

    private CommFieldEndpoint target;

    private JSONObject data;

    public Broadcast(CommField commField, CommFieldEndpoint source, CommFieldEndpoint target, JSONObject data) {
        this.commField = commField;
        this.source = source;
        this.target = target;
        this.data = data;
    }

    public Broadcast(JSONObject json) {
        this.commField = new CommField(json.getJSONObject("field"));
        this.source = new CommFieldEndpoint(json.getJSONObject("source"));
        this.target = new CommFieldEndpoint(json.getJSONObject("target"));
        this.data = json.getJSONObject("data");
    }

    public CommFieldEndpoint getSource() {
        return this.source;
    }

    public CommFieldEndpoint getTarget() {
        return this.target;
    }

    public boolean isMicrophoneVolume() {
        if (!this.data.has("event")) {
            return false;
        }

        return this.data.getString("event").equals(MicrophoneVolume.NAME);
    }

    public MicrophoneVolume extractMicrophoneVolume() {
        if (this.isMicrophoneVolume()) {
            return new MicrophoneVolume(this.data);
        }

        return null;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("field", this.commField.toCompactJSON());
        json.put("source", this.source.toJSON());
        json.put("target", this.target.toJSON());
        json.put("data", this.data);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return toJSON();
    }

    /**
     * 指定的数据格式时候是麦克风音量事件。
     *
     * @param data
     * @return
     */
    public static boolean isMicrophoneVolume(JSONObject data) {
        if (data.has("event") && data.getString("event").equals(MicrophoneVolume.NAME)) {
            return true;
        }

        return false;
    }

    /**
     * 指定的数据格式是否是音频静音事件。
     *
     * @param data
     * @return
     */
    public static boolean isAudioMuted(JSONObject data) {
        if (data.has("event") && data.getString("event").equals(AudioMuted.NAME)) {
            return true;
        }

        return false;
    }

    /**
     * 指定的数据格式是否是视频静音事件。
     *
     * @param data
     * @return
     */
    public static boolean isVideoMuted(JSONObject data) {
        if (data.has("event") && data.getString("event").equals(VideoMuted.NAME)) {
            return true;
        }

        return false;
    }
}
