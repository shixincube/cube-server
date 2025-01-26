/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

import org.json.JSONObject;

/**
 * 客户端描述。
 */
public class ClientDescription extends Entity {

    private String name;

    private String password;

    private ClientState state;

    private Contact pretender;

    public ClientDescription(String name, String password) {
        this.name = name;
        this.password = password;
        this.state = ClientState.Normal;
    }

    public ClientDescription(String name, String password, ClientState state) {
        this.name = name;
        this.password = password;
        this.state = state;
    }

    public ClientDescription(JSONObject json) {
        this.name = json.getString("name");
        this.password = json.has("password") ? json.getString("password") : null;
        this.state = ClientState.parse(json.getInt("state"));

        if (json.has("pretender")) {
            this.pretender = new Contact(json.getJSONObject("pretender"));
        }
    }

    public String getName() {
        return this.name;
    }

    public String getPassword() {
        return this.password;
    }

    public ClientState getState() {
        return this.state;
    }

    public Contact getPretender() {
        return this.pretender;
    }

    public void setPretender(Contact pretender) {
        this.pretender = pretender;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("(");
        buf.append(this.name);
        if (null != this.pretender) {
            buf.append("|");
            buf.append(this.pretender.getId());
        }
        buf.append(")");
        return buf.toString();
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = this.toCompactJSON();
        json.put("password", this.password);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = new JSONObject();
        json.put("name", this.name);
        json.put("state", this.state.code);
        if (null != this.pretender) {
            json.put("pretender", this.pretender.toCompactJSON());
        }
        return json;
    }
}
