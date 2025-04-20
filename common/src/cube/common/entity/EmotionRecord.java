/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

import cube.aigc.psychology.composition.Emotion;
import org.json.JSONObject;

public class EmotionRecord extends Entity {

    public final static String SOURCE_SPEECH = "speech";

    public final static String SOURCE_CHAT = "chat";

    public final long contactId;

    public final Emotion emotion;

    public final String source;

    public JSONObject sourceData;

    public EmotionRecord(long id, long contactId, Emotion emotion, long timestamp, String source) {
        super(id, "", timestamp);
        this.contactId = contactId;
        this.emotion = emotion;
        this.source = source;
    }

    public EmotionRecord(long contactId, Emotion emotion, String source) {
        super();
        this.contactId = contactId;
        this.emotion = emotion;
        this.source = source;
    }

    public EmotionRecord(JSONObject json) {
        super(json);
        this.contactId = json.getLong("contactId");
        this.emotion = Emotion.parse(json.getString("emotion"));
        this.source = json.getString("source");
        if (json.has("data")) {
            this.sourceData = json.getJSONObject("data");
        }
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("contactId", this.contactId);
        json.put("emotion", this.emotion.name());
        json.put("source", this.source);
        if (null != this.sourceData) {
            json.put("data", this.sourceData);
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = super.toCompactJSON();
        json.put("contactId", this.contactId);
        json.put("emotion", this.emotion.name());
        json.put("source", this.source);
        return json;
    }
}
