/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.client.event;

import cube.common.JSONable;
import cube.common.entity.Contact;
import cube.common.entity.Group;
import cube.common.entity.Message;
import cube.service.client.Events;
import org.json.JSONObject;

/**
 * 接收消息事件。
 */
public class MessageReceiveEvent implements JSONable {

    public final static String NAME = Events.ReceiveMessage;

    private Contact contact;

    private Group group;

    private Message message;

    /**
     * 构造函数。
     *
     * @param contact
     */
    public MessageReceiveEvent(Contact contact) {
        this.contact = contact;
    }

    /**
     * 构造函数。
     *
     * @param group
     */
    public MessageReceiveEvent(Group group) {
        this.group = group;
    }

    /**
     * 获取联系人。
     *
     * @return
     */
    public Contact getContact() {
        return this.contact;
    }

    /**
     * 获取群组。
     *
     * @return
     */
    public Group getGroup() {
        return this.group;
    }

    /**
     * 获取唯一键。
     *
     * @return
     */
    public String getUniqueKey() {
        return (null != this.contact) ? this.contact.getUniqueKey() : this.group.getUniqueKey();
    }

    /**
     * 设置消息。
     *
     * @param message
     */
    public void setMessage(Message message) {
        this.message = message;
    }

    @Override
    public boolean equals(Object object) {
        if (null != object && object instanceof MessageReceiveEvent) {
            MessageReceiveEvent other = (MessageReceiveEvent) object;
            if (null != this.contact && null != other.contact) {
                return this.contact.equals(other.contact);
            }
            else if (null != this.group && null != other.group) {
                return this.group.equals(other.group);
            }
        }

        return false;
    }

    @Override
    public int hashCode() {
        return (null != this.contact) ? this.contact.hashCode() : this.group.hashCode();
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();

        if (null != this.contact) {
            json.put("contact", this.contact.toCompactJSON());
        }
        else if (null != this.group) {
            json.put("group", this.group.toCompactJSON());
        }

        if (null != this.message) {
            json.put("message", this.message.toJSON());
        }

        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return toJSON();
    }
}
