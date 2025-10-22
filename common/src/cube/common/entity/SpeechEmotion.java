/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

import cube.common.JSONable;
import org.json.JSONObject;

/**
 * 语音情绪。
 */
public class SpeechEmotion implements JSONable {

    public FileLabel file;

    public long elapsed;

    public Emotion emotion;

    public double score = 0.0;

    public double duration = 0.0;

    public SpeechEmotion(JSONObject json) {
        this.file = json.has("file") ? new FileLabel(json.getJSONObject("file")) : null;
        this.elapsed = json.has("elapsed") ? json.getLong("elapsed") : 0;

        if (json.has("emotion")) {
            Object obj = json.get("emotion");
            if (obj instanceof String) {
                this.emotion = Emotion.parse(obj.toString());
            }
            else {
                this.emotion = Emotion.parse(json.getJSONObject("emotion").getString("name"));
            }
        }
        else {
            this.emotion = Emotion.None;
        }
        this.score = json.getDouble("score");
        this.duration = json.getDouble("duration");
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        if (null != this.file) {
            json.put("file", this.file.toJSON());
        }
        json.put("elapsed", this.elapsed);
        json.put("emotion", this.emotion.toJSON());
        json.put("score", this.score);
        json.put("duration", this.duration);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
