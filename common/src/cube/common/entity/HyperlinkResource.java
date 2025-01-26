/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

import cube.util.TextUtils;
import org.json.JSONObject;

/**
 * 超链接资源。
 */
public class HyperlinkResource extends ComplexResource {

    public final static String TYPE_PAGE = "page";

    public final static String TYPE_PLAIN = "plain";

    public final static String TYPE_IMAGE = "image";

    public final static String TYPE_VIDEO = "video";

    public final static String TYPE_AUDIO = "audio";

    public final static String TYPE_OTHER = "other";

    public final static String TYPE_FAILURE = "failure";

    public final String url;

    public String mimeType;

    public String metaType;

    public String site;

    public String content;

    public String title;

    public String illustration;

    public String path;

    public long size;

    public int numWords;

    public int width;
    public int height;
    public String format;

    public String thumbnail;

    public String rawText;

    public HyperlinkResource(String url, String metaType) {
        super(Subject.Hyperlink);
        this.url = url;
        this.metaType = metaType;
        this.mimeType = "text/plain";
        this.site = TextUtils.extractDomain(url);
        this.content = "[" + this.site + "](" + url + ")";
    }

    public HyperlinkResource(JSONObject json) {
        super(Subject.Hyperlink, json);

        JSONObject payload = json.getJSONObject("payload");

        this.url = payload.has("url") ? payload.getString("url") : "";
        this.metaType = payload.getString("metaType");
        this.mimeType = payload.getString("mimeType");

        if (payload.has("site")) {
            this.site = payload.getString("site");
        }
        else {
            this.site = TextUtils.extractDomain(this.url);
        }

        if (payload.has("title")) {
            this.title = payload.getString("title");
        }
        if (payload.has("content")) {
            this.content = payload.getString("content");
        }

        if (payload.has("illustration")) {
            this.illustration = payload.getString("illustration");
        }

        if (payload.has("path")) {
            this.path = payload.getString("path");
        }
        if (payload.has("size")) {
            this.size = payload.getLong("size");
        }

        if (payload.has("numWords")) {
            this.numWords = payload.getInt("numWords");
        }

        if (payload.has("width")) {
            this.width = payload.getInt("width");
        }
        if (payload.has("height")) {
            this.height = payload.getInt("height");
        }
        if (payload.has("format")) {
            this.format = payload.getString("format");
        }

        if (payload.has("thumbnail")) {
            this.thumbnail = payload.getString("thumbnail");
        }

        if (payload.has("rawText")) {
            this.rawText = payload.getString("rawText");
        }
    }

    public int getNumWords() {
        if (this.numWords == 0 && null != this.content) {
            return this.content.length();
        }
        else {
            return this.numWords;
        }
    }

    public void fixContent() {
        if (null != this.content) {
            this.content = this.filterContent(this.content);
        }
    }

    private String filterContent(String content) {
        StringBuilder result = new StringBuilder();
        String[] array = content.split("\n");
        for (String line : array) {
            if (line.trim().length() < 2) {
                continue;
            }
            result.append(line.trim()).append("\n");
        }
        if (result.length() > 1) {
            result.delete(result.length() - 1, result.length());
        }
        return result.toString();
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();

        JSONObject payload = new JSONObject();
        payload.put("url", this.url);
        payload.put("site", this.site);
        payload.put("metaType", this.metaType);
        payload.put("mimeType", this.mimeType);

        if (null != this.title) {
            payload.put("title", this.title);
        }
        if (null != this.content) {
            payload.put("content", this.content);
        }
        if (null != this.illustration) {
            payload.put("illustration", this.illustration);
        }

        if (this.numWords > 0) {
            payload.put("numWords", this.numWords);
        }

        if (null != this.path) {
            payload.put("path", this.path);
        }

        if (this.size > 0) {
            payload.put("size", this.size);
        }

        if (null != this.format) {
            payload.put("width", this.width);
            payload.put("height", this.height);
            payload.put("format", this.format);
        }

        if (null != this.thumbnail) {
            payload.put("thumbnail", this.thumbnail);
        }

        json.put("payload", payload);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = this.toJSON();
        if (json.getJSONObject("payload").has("path")) {
            json.getJSONObject("payload").remove("path");
        }
        return json;
    }
}
