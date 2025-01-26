/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.messaging;

import cube.common.JSONable;
import cube.common.entity.Message;
import cube.common.state.MessagingStateCode;
import org.json.JSONObject;

/**
 * 推送消息的结果记录。
 */
public class PushResult implements JSONable {

    public final Message message;

    public final MessagingStateCode stateCode;

    public PushResult(Message message, MessagingStateCode stateCode) {
        this.message = message;
        this.stateCode = stateCode;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("message", this.message.toCompactJSON());
        json.put("state", this.stateCode.code);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return toJSON();
    }
}
