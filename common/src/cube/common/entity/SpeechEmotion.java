/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

import cube.aigc.psychology.composition.Emotion;
import cube.common.JSONable;
import org.json.JSONObject;

/**
 * 物体检测结果。
 */
public class SpeechEmotion implements JSONable {

    public FileLabel file;

    public long elapsed;

    public Emotion emotion;

    public double score;

    public SpeechEmotion(JSONObject json) {
        this.file = new FileLabel(json.getJSONObject("file"));
        this.elapsed = json.getLong("elapsed");
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
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("file", this.file.toJSON());
        json.put("elapsed", this.elapsed);
        json.put("emotion", this.emotion.toJSON());
        json.put("score", this.score);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
