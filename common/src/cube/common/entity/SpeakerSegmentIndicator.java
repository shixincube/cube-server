/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

import cube.aigc.Sentiment;
import cube.common.JSONable;
import org.json.JSONObject;

public class SpeakerSegmentIndicator implements JSONable {

    public final String track;

    public final int rhythm;

    public final Sentiment sentiment;

    public SpeakerSegmentIndicator(String track, int rhythm, Sentiment sentiment) {
        this.track = track;
        this.rhythm = rhythm;
        this.sentiment = sentiment;
    }

    public SpeakerSegmentIndicator(JSONObject json) {
        this.track = json.getString("track");
        this.rhythm = json.getInt("rhythm");
        this.sentiment = Sentiment.parse(json.getString("sentiment"));
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("track", this.track);
        json.put("rhythm", this.rhythm);
        json.put("sentiment", this.sentiment.code);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
