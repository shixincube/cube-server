/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.messaging;

import cube.common.entity.MessageState;

/**
 * 消息状态束。
 */
public class MessageStateBundle {

    protected Long messageId;

    protected Long contactId;

    protected MessageState state;

    protected long timestamp;

    public MessageStateBundle(Long messageId, Long contactId, MessageState state) {
        this.messageId = messageId;
        this.contactId = contactId;
        this.state = state;
        this.timestamp = System.currentTimeMillis();
    }
}
