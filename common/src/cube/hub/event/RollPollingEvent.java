/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 Cube Team.
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

import cube.common.entity.ConversationType;
import cube.common.entity.Message;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 轮询会话消息。
 */
public class RollPollingEvent extends WeChatEvent {

    public final static String NAME = "RollPolling";

    private ConversationType conversationType;

    private String conversationName;

    private List<Message> messageList;

    public RollPollingEvent(ConversationType conversationType,
                            String conversationName, List<Message> messageList) {
        super(NAME);
        this.conversationType = conversationType;
        this.conversationName = conversationName;
        this.messageList = messageList;
    }

    public RollPollingEvent(JSONObject json) {
        super(json);
        this.conversationType = ConversationType.parse(json.getInt("conversationType"));
        this.conversationName = json.getString("conversationName");

        this.messageList = new ArrayList<>();
        JSONArray list = json.getJSONArray("messageList");
        for (int i = 0; i < list.length(); ++i) {
            Message message = new Message(list.getJSONObject(i));
            this.messageList.add(message);
        }
    }

    public ConversationType getConversationType() {
        return this.conversationType;
    }

    public String getConversationName() {
        return this.conversationName;
    }

    public void setMessageList(List<Message> list) {
        this.messageList = list;
    }

    public List<Message> getMessageList() {
        return this.messageList;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("conversationType", this.conversationType.code);
        json.put("conversationName", this.conversationName);

        JSONArray array = new JSONArray();
        for (Message message : this.messageList) {
            array.put(message.toJSON());
        }
        json.put("messageList", array);

        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = super.toCompactJSON();
        json.put("conversationType", this.conversationType.code);
        json.put("conversationName", this.conversationName);

        JSONArray array = new JSONArray();
        for (Message message : this.messageList) {
            array.put(message.toJSON());
        }
        json.put("messageList", array);

        return json;
    }
}
