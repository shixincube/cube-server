/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.console.mgmt;

import cube.common.JSONable;
import org.json.JSONObject;

/**
 * 用户。
 */
public class User implements JSONable {

    public final Long id;

    public final String name;

    public final String avatar;

    public final String displayName;

    public final int role;

    public final String group;

    protected String password;

    public User(Long id, String name, String avatar, String displayName, int role, String group) {
        this(id, name, avatar, displayName, role, group, null);
    }

    public User(Long id, String name, String avatar, String displayName, int role, String group, String password) {
        this.id = id;
        this.name = name;
        this.avatar = avatar;
        this.displayName = displayName;
        this.role = role;
        this.group = group;
        this.password = password;
    }

    public boolean validatePassword(String password) {
        return password.equalsIgnoreCase(this.password);
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("name", this.name);
        json.put("avatar", this.avatar);
        json.put("displayName", this.displayName);
        json.put("role", this.role);
        json.put("group", this.group);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
