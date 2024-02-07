/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 Ambrose Xu.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
