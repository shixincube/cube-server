/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.file.misc;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * 文件属性。
 */
public class MediaAttribute {

    private StreamAttribute[] streamAttributes;

    private FormatAttribute formatAttribute;

    public MediaAttribute(JSONObject json) {
        JSONArray streams = json.getJSONArray("streams");
        this.streamAttributes = new StreamAttribute[streams.length()];
        for (int i = 0; i < streams.length(); ++i) {
            this.streamAttributes[i] = new StreamAttribute(streams.getJSONObject(i));
        }
        this.formatAttribute = new FormatAttribute(json.getJSONObject("format"));
    }

    public void setStreamAttributes(StreamAttribute[] streamAttributes) {
        this.streamAttributes = streamAttributes;
    }

    public StreamAttribute[] getStreamAttributes() {
        return this.streamAttributes;
    }

    public void setFormatAttribute(FormatAttribute formatAttribute) {
        this.formatAttribute = formatAttribute;
    }

    public FormatAttribute getFormatAttribute() {
        return this.formatAttribute;
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();

        JSONArray streams = new JSONArray();
        for (StreamAttribute stream : this.streamAttributes) {
            streams.put(stream.toJSON());
        }

        json.put("streams", streams);
        json.put("format", this.formatAttribute.toJSON());
        return json;
    }
}
