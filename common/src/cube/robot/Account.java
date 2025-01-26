/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.robot;

import cube.common.JSONable;
import org.json.JSONObject;

/**
 * Roboengine 的账号描述。
 */
public class Account implements JSONable {

    /**
     * 账号状态，一般状态。
     */
    public final static int STATE_NORMAL = 0;

    /**
     * 账号状态，被禁用状态。
     */
    public final static int STATE_FORBIDDEN = 3;

    /**
     * 账号 ID 。
     */
    public long id;

    /**
     * 账号名。
     */
    public String name;

    /**
     * 账号密码。
     */
    public String password;

    /**
     * 是否是管理员账号。
     */
    public boolean isAdmin;

    /**
     * 账号头像。
     */
    public String avatar;

    /**
     * 账号全名。
     */
    public String fullName;

    /**
     * 账号创建时间戳。
     */
    public long creationTime;

    /**
     * 账号状态。
     */
    public int state;

    /**
     * 账号的令牌。
     */
    public String token;

    /**
     * 账号最近一次登录时的 IP 地址。
     */
    public String lastAddress;

    /**
     * 账号最近一次的登录时间。
     */
    public long lastLoginTime;

    /**
     * 账号最近一次的登录设备信息。
     */
    public JSONObject lastDevice;

    /**
     * 账号是否在线。
     */
    public boolean online = false;

    /**
     * 当前是否有实时任务正在运行。
     */
    public boolean taskRunning = false;

    public Account(long id, String name, String password, boolean isAdmin, String avatar,
                   String fullName, long creationTime, int state) {
        this.id = id;
        this.name = name;
        this.password = password;
        this.isAdmin = isAdmin;
        this.avatar = avatar;
        this.fullName = fullName;
        this.creationTime = creationTime;
        this.state = state;
    }

    public Account(JSONObject json) {
        this.id = json.has("id") ? json.getLong("id") : 0;
        this.name = json.getString("name");
        this.isAdmin = json.getBoolean("isAdmin");
        this.avatar = json.getString("avatar");
        this.fullName = json.getString("fullName");
        this.creationTime = json.getLong("creationTime");
        this.state = json.getInt("state");
        this.online = json.getBoolean("online");

        if (json.has("lastLoginTime")) {
            this.lastLoginTime = json.getLong("lastLoginTime");
        }

        if (json.has("lastAddress")) {
            this.lastAddress = json.getString("lastAddress");
        }

        if (json.has("lastDevice")) {
            this.lastDevice = json.getJSONObject("lastDevice");
        }

        if (json.has("token")) {
            this.token = json.getString("token");
        }

        if (json.has("taskRunning")) {
            this.taskRunning = json.getBoolean("taskRunning");
        }
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("id", this.id);
        json.put("name", this.name);
        json.put("isAdmin", this.isAdmin);
        json.put("avatar", this.avatar);
        json.put("fullName", this.fullName);
        json.put("creationTime", this.creationTime);
        json.put("state", this.state);
        json.put("online", this.online);
        json.put("lastLoginTime", this.lastLoginTime);

        if (null != this.lastAddress) {
            json.put("lastAddress", this.lastAddress);
        }

        if (null != this.lastDevice) {
            json.put("lastDevice", this.lastDevice);
        }

        if (null != this.token) {
            json.put("token", this.token);
        }

        json.put("taskRunning", this.taskRunning);

        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return toJSON();
    }
}
