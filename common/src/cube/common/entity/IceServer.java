/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

import cube.common.JSONable;
import org.json.JSONObject;

/**
 * ICE 服务器。
 */
public class IceServer implements JSONable {

    public final String url;

    public final String username;

    public final String credential;

    public IceServer(String url, String username, String credential) {
        this.url = url;
        this.username = username;
        this.credential = credential;
    }

    public IceServer(JSONObject json) {
        this.url = json.getString("urls");
        this.username = json.getString("username");
        this.credential = json.getString("credential");
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("urls", this.url);
        json.put("username", this.username);
        json.put("credential", this.credential);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return toJSON();
    }
}
