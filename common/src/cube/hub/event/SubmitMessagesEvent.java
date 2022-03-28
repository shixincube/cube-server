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
import cube.common.entity.Message;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 新消息事件。
 */
public class SubmitMessagesEvent extends WeChatEvent {

    public final static String NAME = "SubmitMessages";

    private Group group;

    private Contact partner;

    private List<Message> messages;

    public SubmitMessagesEvent(Contact account, Group group, List<Message> messages) {
        super(NAME, account);
        this.group = group;
        this.messages = messages;
    }

    public SubmitMessagesEvent(Contact account, Contact partner, List<Message> messages) {
        super(NAME, account);
        this.partner = partner;
        this.messages = messages;
    }

    public SubmitMessagesEvent(JSONObject json) {
        super(json);

        if (json.has("group")) {
            this.group = new Group(json.getJSONObject("group"));
        }

        if (json.has("partner")) {
            this.partner = new Contact(json.getJSONObject("partner"));
        }

        this.messages = new ArrayList<>();
        JSONArray array = json.getJSONArray("messages");
        for (int i = 0; i < array.length(); ++i) {
            this.messages.add(new Message(array.getJSONObject(i)));
        }
    }

    public Group getGroup() {
        return this.group;
    }

    public Contact getPartner() {
        return this.partner;
    }

    public List<Message> getMessages() {
        return this.messages;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();

        if (null != this.group) {
            json.put("group", this.group.toCompactJSON());
        }

        if (null != this.partner) {
            json.put("partner", this.partner.toCompactJSON());
        }

        JSONArray array = new JSONArray();
        for (Message message : this.messages) {
            array.put(message.toJSON());
        }
        json.put("messages", array);

        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = super.toCompactJSON();

        if (null != this.group) {
            json.put("group", this.group.toCompactJSON());
        }

        if (null != this.partner) {
            json.put("partner", this.partner.toCompactJSON());
        }

        JSONArray array = new JSONArray();
        for (Message message : this.messages) {
            array.put(message.toJSON());
        }
        json.put("messages", array);

        return json;
    }
}
