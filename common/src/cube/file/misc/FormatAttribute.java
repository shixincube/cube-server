/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.file.misc;

import org.json.JSONObject;

/**
 * 文件格式属性。
 */
public class FormatAttribute {

    public String filename;

    /**
     * Number of streams as indicated in the file metadata
     */
    public int numStreams;

    public String formatName;

    public String formatLongName;

    public double duration;

    public long size;

    public long bitRate;

    public JSONObject tags;

    public FormatAttribute(JSONObject json) {
        this.filename = json.getString("filename");
        this.numStreams = json.getInt("nb_streams");
        this.formatName = json.getString("format_name");
        this.formatLongName = json.getString("format_long_name");
        this.duration = Double.parseDouble(json.getString("duration"));
        this.size = Long.parseLong(json.getString("size"));
        this.bitRate = Long.parseLong(json.getString("bit_rate"));
        this.tags = json.getJSONObject("tags");
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("filename", this.filename);
        json.put("nb_streams", this.numStreams);
        json.put("format_name", this.formatName);
        json.put("format_long_name", this.formatLongName);
        json.put("duration", Double.toString(this.duration));
        json.put("size", Long.toString(this.size));
        json.put("bit_rate", Long.toString(this.bitRate));
        json.put("tags", this.tags);
        return json;
    }
}
