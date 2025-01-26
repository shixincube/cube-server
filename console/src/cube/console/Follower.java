/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.console;

import cube.common.JSONable;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 从属主机。
 */
public class Follower implements JSONable {

    public String name;

    public String listening;

    public String address;

    public int port;

    public Follower(String name, String listening, String address, int port) {
        this.name = name;
        this.listening = listening;
        this.address = address;
        this.port = port;
    }

    public Follower(JSONObject json) {
        try {
            this.name = json.getString("name");
            this.listening = json.getString("listening");
            this.address = json.getString("address");
            this.port = json.getInt("port");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("name", this.name);
            json.put("listening", this.listening);
            json.put("address", this.address);
            json.put("port", this.port);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
