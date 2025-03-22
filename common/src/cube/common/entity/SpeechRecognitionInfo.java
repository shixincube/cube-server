/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

import cube.common.JSONable;
import org.json.JSONObject;

/**
 * 语音识别结果。
 */
public class SpeechRecognitionInfo implements JSONable {

    private FileLabel file;

    private long elapsed;

    private String text;

    private double durationInSeconds;

    public SpeechRecognitionInfo(JSONObject json) {
        this.file = new FileLabel(json.getJSONObject("file"));
        this.elapsed = json.getLong("elapsed");
        this.text = json.getString("text");
        this.durationInSeconds = json.getDouble("duration");
    }

    public String getFileCode() {
        return this.file.getFileCode();
    }

    public String getText() {
        return this.text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("file", this.file.toJSON());
        json.put("elapsed", this.elapsed);
        json.put("text", this.text);
        json.put("duration", this.durationInSeconds);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
