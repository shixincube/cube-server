/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.app.server.account;

import cube.app.server.util.PhoneNumbers;
import cube.common.JSONable;
import org.json.JSONObject;

/**
 * 账号实体。
 */
public class Account implements JSONable {

    public final static int STATE_NORMAL = 0;

    /**
     * 账号 ID 。
     */
    public final long id;

    /**
     * 账号域。
     */
    public final String domain;

    /**
     * 账号。
     */
    public final String account;

    /**
     * 电话号码。
     */
    public final String phone;

    /**
     * 密码。
     */
    public final String password;

    /**
     * 显示名。
     */
    public String name;

    /**
     * 头像。
     */
    public String avatar;

    /**
     * 账号状态。
     */
    public final int state;

    public String region;

    public String department;

    public long registration;

    protected long timestamp = System.currentTimeMillis();

    public Account(long id, String domain, String account, String phone, String password,
                   String name, String avatar, int state) {
        this.id = id;
        this.domain = domain;
        this.account = account;
        this.phone = phone;
        this.password = password;
        this.name = name;
        this.avatar = avatar;
        this.state = state;
        this.registration = this.timestamp;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("id", this.id);
        json.put("domain", this.domain);
//        json.put("account", this.account);
        json.put("phone", PhoneNumbers.desensitize(this.phone));
        json.put("name", this.name);
        json.put("avatar", this.avatar);
        json.put("state", this.state);
        json.put("region", null != this.region ? this.region : "");
        json.put("department", null != this.department ? this.department : "");
        json.put("registration", this.registration);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = this.toJSON();
        json.remove("registration");
        return json;
    }

    public JSONObject toFullJSON() {
        JSONObject json = new JSONObject();
        json.put("id", this.id);
        json.put("domain", this.domain);
        json.put("account", this.account);
        json.put("phone", this.phone);
        json.put("name", this.name);
        json.put("avatar", this.avatar);
        json.put("state", this.state);
        json.put("region", null != this.region ? this.region : "");
        json.put("department", null != this.department ? this.department : "");
        json.put("registration", this.registration);
        return json;
    }
}
