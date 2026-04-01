/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

import cube.common.JSONable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class VoiceIndicator implements JSONable {

    protected final static String sPromptSentiment = "已知内容：\n\n%s。\n\n判断以上内容的情绪倾向属于以下哪种：\n\n* 积极的\n* 消极的\n* 中立的\n* 积极和消极均有\n\n仅回答属于以上那种情绪倾向。";

    protected static final Map<Emotion, Double> sEmotionWeight = new HashMap<>();

    static {
        if (sEmotionWeight.isEmpty()) {
            sEmotionWeight.put(Emotion.Angry, -0.2);
            sEmotionWeight.put(Emotion.Fearful, -0.3);
            sEmotionWeight.put(Emotion.Happy, 0.8);
            sEmotionWeight.put(Emotion.Neutral, 0.0);
            sEmotionWeight.put(Emotion.Sad, -0.5);
            sEmotionWeight.put(Emotion.Surprise, 0.2);
        }
    }

    public enum Tense {
        Future,
        Past
    }

    public long id;

    public long timestamp;

    public double silenceDuration = 0;

    public Map<String, SpeakerIndicator> speakerIndicators = new HashMap<>();

    public VoiceIndicator(long id) {
        this.id = id;
        this.timestamp = System.currentTimeMillis();
    }

    public VoiceIndicator(long id, long timestamp, double silenceDuration) {
        this.id = id;
        this.timestamp = timestamp;
        this.silenceDuration = silenceDuration;
    }

    public VoiceIndicator(JSONObject json) {
        this.id = json.getLong("id");
        this.timestamp = json.getLong("timestamp");
        this.silenceDuration = json.getDouble("silenceDuration");

        JSONArray speakers = json.getJSONArray("speakers");
        for (int i = 0; i < speakers.length(); ++i) {
            SpeakerIndicator indicator = new SpeakerIndicator(speakers.getJSONObject(i));
            this.speakerIndicators.put(indicator.label, indicator);
        }
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("id", this.id);
        json.put("timestamp", this.timestamp);
        json.put("silenceDuration", this.silenceDuration);

        JSONArray speakers = new JSONArray();
        for (Map.Entry<String, SpeakerIndicator> e : this.speakerIndicators.entrySet()) {
            speakers.put(e.getValue().toJSON());
        }
        json.put("speakers", speakers);

        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
