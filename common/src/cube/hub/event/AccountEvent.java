/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.hub.event;

import cube.common.entity.Contact;
import org.json.JSONObject;

/**
 * 账号信息事件。
 */
public class AccountEvent extends WeChatEvent {

    public final static String NAME = "Account";

    public AccountEvent(Contact account) {
        super(NAME, account);
    }

    public AccountEvent(JSONObject json) {
        super(json);
    }

    @Override
    public JSONObject toCompactJSON() {
        return super.toCompactJSON();
    }
}
