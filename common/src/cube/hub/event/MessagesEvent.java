/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 Ambrose Xu.
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
import cube.hub.data.DataHelper;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 消息数据事件。
 */
public class MessagesEvent extends WeChatEvent {

    public final static String NAME = "Messages";

    private Contact partner;

    private Group group;

    private int beginIndex;

    private int endIndex;

    private List<Message> messages;

    public MessagesEvent(Group group, int beginIndex, int endIndex, List<Message> messages) {
        super(NAME);
        this.group = group;
        this.beginIndex = beginIndex;
        this.endIndex = endIndex;
        this.messages = messages;
    }

    public MessagesEvent(Contact partner, int beginIndex, int endIndex, List<Message> messages) {
        super(NAME);
        this.partner = partner;
        this.beginIndex = beginIndex;
        this.endIndex = endIndex;
        this.messages = messages;
    }

    public MessagesEvent(JSONObject json) {
        super(json);
        if (json.has("group")) {
            this.group = new Group(json.getJSONObject("group"));
        }
        else if (json.has("partner")) {
            this.partner = new Contact(json.getJSONObject("partner"));
        }

        this.beginIndex = json.getInt("begin");
        this.endIndex = json.getInt("end");

        this.messages = new ArrayList<>();
        JSONArray array = json.getJSONArray("messages");
        for (int i = 0; i < array.length(); ++i) {
            this.messages.add(new Message(array.getJSONObject(i)));
        }
    }

    public List<Message> getMessages() {
        return this.messages;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        if (null != this.group) {
            json.put("group", this.group.toJSON());
        }
        else if (null != this.partner) {
            json.put("partner",
                    DataHelper.filterContactAvatarFileLabel(this.partner.toJSON()));
        }
        json.put("begin", this.beginIndex);
        json.put("end", this.endIndex);

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
            json.put("group",
                    DataHelper.filterContactAvatarFileLabel(this.group.toCompactJSON()));
        }
        else if (null != this.partner) {
            json.put("partner",
                    DataHelper.filterContactAvatarFileLabel(this.partner.toCompactJSON()));
        }
        json.put("begin", this.beginIndex);
        json.put("end", this.endIndex);

        JSONArray array = new JSONArray();
        for (Message message : this.messages) {
            array.put(message.toJSON());
        }
        json.put("messages", array);
        return json;
    }
}
