/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

import cube.auth.AuthToken;
import org.json.JSONObject;

public class User extends Entity {

    private String name;

    private String appAgent;

    private String displayName;

    private String phoneNumber;

    private String avatar;

    private AuthToken authToken;

    public User(long id, String name, String appAgent) {
        super(id);
        this.name = name;
        this.appAgent = appAgent;
        this.displayName = "";
        this.phoneNumber = "";
        this.avatar = "";
    }

    public User(JSONObject json) {
        super(json);
        this.name = json.getString("name");
        this.appAgent = json.getString("appAgent");
        this.displayName = json.getString("displayName");
        this.phoneNumber = json.getString("phoneNumber");
        this.avatar = json.getString("avatar");
        if (json.has("authToken")) {
            this.authToken = new AuthToken(json.getJSONObject("authToken"));
        }
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setAuthToken(AuthToken authToken) {
        this.authToken = authToken;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("name", this.name);
        json.put("appAgent", this.appAgent);
        json.put("displayName", this.displayName);
        json.put("phoneNumber", this.phoneNumber);
        json.put("avatar", this.avatar);
        if (null != this.authToken) {
            json.put("authToken", this.authToken.toJSON());
        }
        return json;
    }
}
