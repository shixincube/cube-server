/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
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
