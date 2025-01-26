/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.messaging;

/**
 * 消息记录的主键。
 */
public class MessageKey {

    protected Long contactId;

    protected Long messageId;

    protected int hash = 0;

    protected MessageKey(Long contactId, Long messageId) {
        this.contactId = contactId;
        this.messageId = messageId;
        this.hash = contactId.hashCode() * 3 + messageId.hashCode() * 7;
    }

    @Override
    public boolean equals(Object object) {
        if (null != object && object instanceof MessageKey) {
            MessageKey other = (MessageKey) object;
            if (other.contactId.equals(this.contactId) && other.messageId.equals(this.messageId)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public int hashCode() {
        return this.hash;
    }
}
