/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.hub.event;

import cube.common.entity.Contact;
import org.json.JSONObject;

/**
 * 已分配账号事件。
 */
public class AllocatedEvent extends WeChatEvent {

    public final static String NAME = "Allocated";

    public AllocatedEvent(String code, Contact account) {
        super(NAME, account);
        this.setCode(code);
    }

    public AllocatedEvent(JSONObject json) {
        super(json);
    }
}
