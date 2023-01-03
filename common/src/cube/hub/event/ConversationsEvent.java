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

import cube.common.entity.Conversation;
import cube.common.entity.ConversationType;
import cube.hub.data.DataHelper;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 会话数据事件。
 */
public class ConversationsEvent extends WeChatEvent {

    public final static String NAME = "Conversations";

    private List<Conversation> conversations;

    public ConversationsEvent(List<Conversation> conversations) {
        super(NAME);
        this.conversations = conversations;
    }

    public ConversationsEvent(JSONObject json) {
        super(json);
        this.conversations = new ArrayList<>();
        JSONArray list = json.getJSONArray("conversations");
        for (int i = 0; i < list.length(); ++i) {
            Conversation conversation = new Conversation(list.getJSONObject(i));
            this.conversations.add(conversation);
        }
    }

    public List<Conversation> getConversations() {
        return this.conversations;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        JSONArray list = new JSONArray();
        for (Conversation conversation : this.conversations) {
            list.put(conversation.toJSON());
        }
        json.put("conversations", list);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = super.toCompactJSON();
        JSONArray list = new JSONArray();
        for (Conversation conversation : this.conversations) {
            JSONObject conversationJson = conversation.toJSON();

            if (conversationJson.has("pivotalEntity")) {
                conversationJson.put("pivotalEntity",
                        DataHelper.filterContactAvatarFileLabel(conversationJson.getJSONObject("pivotalEntity")));
            }

            list.put(conversationJson);
        }
        json.put("conversations", list);
        return json;
    }
}
