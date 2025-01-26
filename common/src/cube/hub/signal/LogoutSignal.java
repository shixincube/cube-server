/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.hub.signal;

import cube.common.entity.Contact;
import org.json.JSONObject;

/**
 * 账号退出登录。
 */
public class LogoutSignal extends Signal {

    public final static String NAME = "Logout";

    private Contact account;

    public LogoutSignal(String channelCode) {
        super(NAME);
        setCode(channelCode);
    }

    public LogoutSignal(Contact account, String channelCode) {
        super(NAME);
        this.account = account;
        setCode(channelCode);
    }

    public LogoutSignal(JSONObject json) {
        super(json);
        if (json.has("account")) {
            this.account = new Contact(json.getJSONObject("account"));
        }
    }

    public Contact getAccount() {
        return this.account;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        if (null != this.account) {
            json.put("account", this.account.toCompactJSON());
        }
        return json;
    }
}
