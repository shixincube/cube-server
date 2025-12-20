/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

import cube.common.JSONable;
import org.json.JSONObject;

/**
 * 语音轨道记录。
 */
public class VoiceTrack implements JSONable {

    public String track;

    public String label;

    public VoiceSegment segment;

    public SpeechEmotion emotion;

    public SpeechRecognitionInfo recognition;

    public VoiceTrack(JSONObject json) {
        this.track = json.getString("track");
        this.label = json.getString("label");
        this.segment = new VoiceSegment(json.getJSONObject("segment"));
        this.emotion = new SpeechEmotion(json.getJSONObject("emotion"));
        this.recognition = new SpeechRecognitionInfo(json.getJSONObject("recognition"));
    }

    public VoiceTrack(String track, String label, VoiceSegment segment,
                      SpeechEmotion emotion, SpeechRecognitionInfo recognition) {
        this.track = track;
        this.label = label;
        this.segment = segment;
        this.emotion = emotion;
        this.recognition = recognition;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("track", this.track);
        json.put("segment", this.segment.toJSON());
        json.put("label", this.label);
        json.put("emotion", this.emotion.toJSON());
        json.put("recognition", this.recognition.toJSON());
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
