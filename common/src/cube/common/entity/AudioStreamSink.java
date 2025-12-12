/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

import cube.common.JSONable;
import org.json.JSONObject;

public class AudioStreamSink implements JSONable {

    private String streamName;

    private int index;

    private long timestamp;

    private VoiceDiarization diarization;

    private FileLabel fileLabel;

    public AudioStreamSink(String streamName, int index) {
        this.streamName = streamName;
        this.index = index;
    }

    public AudioStreamSink(JSONObject json) {
        this.streamName = json.getString("streamName");
        this.index = json.getInt("index");
        this.timestamp = json.getLong("timestamp");
        if (json.has("diarization")) {
            this.diarization = new VoiceDiarization(json.getJSONObject("diarization"));
        }
        if (json.has("file")) {
            this.fileLabel = new FileLabel(json.getJSONObject("file"));
        }
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setDiarization(VoiceDiarization diarization) {
        this.diarization = diarization;
    }

    public void setFileLabel(FileLabel fileLabel) {
        this.fileLabel = fileLabel;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("streamName", this.streamName);
        json.put("index", this.index);
        json.put("timestamp", this.timestamp);
        if (null != this.diarization) {
            json.put("diarization", this.diarization.toJSON());
        }
        if (null != this.fileLabel) {
            json.put("file", this.fileLabel.toCompactJSON());
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
