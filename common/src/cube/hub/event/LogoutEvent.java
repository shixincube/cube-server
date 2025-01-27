/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.hub.event;

import cube.common.entity.Contact;
import org.json.JSONObject;

/**
 * 退出登录事件。
 */
public class LogoutEvent extends WeChatEvent {

    public final static String NAME = "Logout";

    public LogoutEvent(long sn, String channelCode, Contact account) {
        super(sn, NAME, account);
        this.setCode(channelCode);
    }

    public LogoutEvent(JSONObject json) {
        super(json);
    }
}
