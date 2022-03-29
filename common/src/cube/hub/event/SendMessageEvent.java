/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 Cube Team.
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

package cube.hub.event;

import cube.common.entity.Contact;
import cube.common.entity.Group;
import cube.hub.data.wechat.PlainMessage;
import org.json.JSONObject;

/**
 * 发送消息事件。
 */
public class SendMessageEvent extends WeChatEvent {

    public final static String NAME = "SendMessage";

    private Contact partner;

    private Group group;

    private PlainMessage plainMessage;

    public SendMessageEvent(Contact account, Contact partner, PlainMessage plainMessage) {
        super(NAME, account);
        this.partner = partner;
        this.plainMessage = plainMessage;
    }

    public SendMessageEvent(Contact account, Group group, PlainMessage plainMessage) {
        super(NAME, account);
        this.group = group;
        this.plainMessage = plainMessage;
    }

    public SendMessageEvent(JSONObject json) {
        super(json);

        if (json.has("partner")) {
            this.partner = new Contact(json.getJSONObject("partner"));
        }

        if (json.has("group")) {
            this.group = new Group(json.getJSONObject("group"));
        }

        if (json.has("plainMessage")) {
            this.plainMessage = new PlainMessage(json.getJSONObject("plainMessage"));
        }
    }

    public Contact getPartner() {
        return this.partner;
    }

    public Group getGroup() {
        return this.group;
    }

    public PlainMessage getPlainMessage() {
        return this.plainMessage;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        return make(json);
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = super.toCompactJSON();
        return make(json);
    }

    private JSONObject make(JSONObject json) {
        if (null != this.partner) {
            json.put("partner", this.partner.toCompactJSON());
        }

        if (null != this.group) {
            json.put("group", this.group.toCompactJSON());
        }

        if (null != this.plainMessage) {
            json.put("plainMessage", this.plainMessage.toJSON());
        }

        return json;
    }
}
