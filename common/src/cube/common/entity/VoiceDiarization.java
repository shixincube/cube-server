/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class VoiceDiarization extends Entity {

    public long contactId;

    public String title;

    public String remark;

    public String fileCode;

    public FileLabel file;

    public double duration;

    public long elapsed;

    public List<VoiceTrack> tracks;

    public VoiceIndicator indicator;

    public VoiceDiarization(JSONObject json) {
        super(json);
        if (json.has("contactId")) {
            this.contactId = json.getLong("contactId");
        }
        if (json.has("title")) {
            this.title = json.getString("title");
        }
        if (json.has("remark")) {
            this.remark = json.getString("remark");
        }
        if (json.has("fileCode")) {
            this.fileCode = json.getString("fileCode");
        }
        if (json.has("file")) {
            this.file = new FileLabel(json.getJSONObject("file"));
            this.fileCode = this.file.getFileCode();
        }
        this.duration = json.getDouble("duration");
        this.elapsed = json.getLong("elapsed");
        this.tracks = new ArrayList<>();
        JSONArray array = json.getJSONArray("tracks");
        for (int i = 0; i < array.length(); ++i) {
            this.tracks.add(new VoiceTrack(array.getJSONObject(i)));
        }

        if (json.has("indicator")) {
            this.indicator = new VoiceIndicator(json.getJSONObject("indicator"));
        }
    }

    public VoiceDiarization(long id, long timestamp, long contactId, String title, String remark, String fileCode,
                            double duration, long elapsed) {
        super(id, "", timestamp);
        this.contactId = contactId;
        this.title = title;
        this.remark = remark;
        this.fileCode = fileCode;
        this.duration = duration;
        this.elapsed = elapsed;
        this.tracks = new ArrayList<>();
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("contactId", this.contactId);
        if (null != this.title) {
            json.put("title", this.title);
        }
        if (null != this.remark) {
            json.put("remark", this.remark);
        }
        if (null != this.file) {
            json.put("file", this.file.toJSON());
        }
        json.put("fileCode", this.fileCode);
        json.put("duration", this.duration);
        json.put("elapsed", this.elapsed);
        JSONArray array = new JSONArray();
        for (VoiceTrack track : this.tracks) {
            array.put(track.toJSON());
        }
        json.put("tracks", array);

        if (null != this.indicator) {
            json.put("indicator", this.indicator.toJSON());
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
