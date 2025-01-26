/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.app.server.notice;

import cube.common.JSONable;
import org.json.JSONObject;

/**
 * 通知。
 */
public class Notice implements JSONable {

    private long id;

    private String domain;

    private String title;

    private String content;

    private String contentURL;

    private int type;

    /**
     * 创建时间。
     */
    private long creation;

    /**
     * 通知的到期时间。
     */
    private long expires;

    public Notice(long id, String domain, String title, String content, int type, long creation, long expires) {
        this.id = id;
        this.domain = domain;
        this.title = title;
        this.content = content;
        this.type = type;
        this.creation = creation;
        this.expires = expires;
    }

    public Notice(JSONObject json) {
        this.id = json.getLong("id");
        this.domain = json.getString("domain");
        this.title = json.getString("title");
        this.content = json.getString("content");
        this.type = json.getInt("type");
        this.creation = json.getLong("creation");
        this.expires = json.getLong("expires");

        if (json.has("contentURL")) {
            this.contentURL = json.getString("contentURL");
        }
    }

    public long getId() {
        return this.id;
    }

    public String getDomain() {
        return this.domain;
    }

    public String getTitle() {
        return this.title;
    }

    public String getContent() {
        return this.content;
    }

    public String getContentURL() {
        return this.contentURL;
    }

    public void setContentURL(String value) {
        this.contentURL = value;
    }

    public int getType() {
        return this.type;
    }

    public long getCreation() {
        return this.creation;
    }

    public long getExpires() {
        return this.expires;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("id", this.id);
        json.put("domain", this.domain);
        json.put("title", this.title);
        json.put("content", this.content);
        json.put("type", this.type);
        json.put("creation", this.creation);
        json.put("expires", this.expires);

        if (null != this.contentURL) {
            json.put("contentURL", this.contentURL);
        }

        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
