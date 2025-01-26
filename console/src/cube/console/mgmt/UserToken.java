/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.console.mgmt;

import cube.common.JSONable;
import org.json.JSONObject;

/**
 * 访问令牌。
 */
public class UserToken implements JSONable {

    public final long userId;

    public final String token;

    public final long creation;

    public final long expire;

    public User user;

    public UserToken(long userId, String token, long creation, long expire) {
        this.userId = userId;
        this.token = token;
        this.creation = creation;
        this.expire = expire;
    }

    public int getAgeInSeconds() {
        return (int) Math.round((this.expire - this.creation) / 1000.0);
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("token", this.token);
        json.put("creation", this.creation);
        json.put("expire", this.expire);
        json.put("user", this.user.toJSON());
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
