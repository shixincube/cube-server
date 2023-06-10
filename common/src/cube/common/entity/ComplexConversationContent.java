/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 Cube Team.
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

package cube.common.entity;

import org.json.JSONObject;

/**
 * 复合会话内容。
 */
public class ComplexConversationContent extends Entity {

    public final static String TYPE_RAW = "raw";

    public final static String TYPE_PLAIN = "plain";

    public final static String TYPE_PAGE = "page";

    public final static String TYPE_IMAGE = "image";

    public final static String TYPE_VIDEO = "video";

    public final static String TYPE_AUDIO = "audio";

    public final static String TYPE_OTHER = "other";

    public final String url;

    public String mimeType;

    public String type;

    public String content;

    public String title;

    public String illustration;

    public String path;

    public long size;

    public int numWords;

    public int width;
    public int height;
    public String format;

    public ComplexConversationContent(String text) {
        this.url = "";
        this.mimeType = "text/plain";
        this.type = TYPE_RAW;
        this.content = text;
    }

    public ComplexConversationContent(JSONObject json) {
        super();
        this.url = json.getString("url");
        this.type = json.getString("metaType");
        this.mimeType = json.getString("mimeType");

        if (json.has("title")) {
            this.title = json.getString("title");
        }
        if (json.has("content")) {
            this.content = json.getString("content");
        }
        if (json.has("illustration")) {
            this.illustration = json.getString("illustration");
        }

        if (json.has("path")) {
            this.path = json.getString("path");
        }
        if (json.has("size")) {
            this.size = json.getLong("size");
        }

        if (json.has("numWords")) {
            this.numWords = json.getInt("numWords");
        }

        if (json.has("width")) {
            this.width = json.getInt("width");
        }
        if (json.has("height")) {
            this.height = json.getInt("height");
        }
        if (json.has("format")) {
            this.format = json.getString("format");
        }
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("url", this.url);
        json.put("type", this.type);
        json.put("mimeType", this.mimeType);
        if (null != this.title) {
            json.put("title", this.title);
        }
        if (null != this.content) {
            json.put("content", this.content);
        }
        if (null != this.illustration) {
            json.put("illustration", this.illustration);
        }

        if (this.numWords > 0) {
            json.put("numWords", this.numWords);
        }

        if (null != this.path) {
            json.put("path", this.path);
            json.put("size", this.size);
        }

        if (null != this.format) {
            json.put("width", this.width);
            json.put("height", this.height);
            json.put("format", this.format);
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = this.toJSON();
        if (json.has("path")) {
            json.remove("path");
        }
        return json;
    }
}
