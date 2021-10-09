/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 Shixin Cube Team.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package cube.service.client.event;

import cube.common.JSONable;
import cube.common.entity.Contact;
import cube.common.entity.Group;
import cube.common.entity.Message;
import cube.service.client.Events;
import org.json.JSONObject;

/**
 * 发送消息事件。
 */
public class MessageSendEvent implements JSONable {

    public final static String NAME = Events.SendMessage;

    private Contact contact;

    private Group group;

    private Message message;

    /**
     * 构造函数。
     *
     * @param contact
     */
    public MessageSendEvent(Contact contact) {
        this.contact = contact;
    }

    /**
     * 构造函数。
     *
     * @param group
     */
    public MessageSendEvent(Group group) {
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
        if (null != object && object instanceof MessageSendEvent) {
            MessageSendEvent other = (MessageSendEvent) object;
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
