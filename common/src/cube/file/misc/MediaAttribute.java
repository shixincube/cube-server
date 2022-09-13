/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 Cube Team.
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
