/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.hub.signal;

import org.json.JSONObject;

/**
 * 获取消息信令。
 */
public class GetConversationsSignal extends Signal {

    public final static String NAME = "GetConversations";

    private int numConversations = 10;

    private int numRecentMessages = 5;

    public GetConversationsSignal(String channelCode) {
        super(NAME);
        setCode(channelCode);
    }

    public GetConversationsSignal(JSONObject json) {
        super(json);
        if (json.has("numConversations")) {
            this.numConversations = json.getInt("numConversations");
        }
        if (json.has("numRecentMessages")) {
            this.numRecentMessages = json.getInt("numRecentMessages");
        }
    }

    public int getNumConversations() {
        return this.numConversations;
    }

    public void setNumConversations(int value) {
        this.numConversations = value;
    }

    public int getNumRecentMessages() {
        return this.numRecentMessages;
    }

    public void setNumRecentMessages(int value) {
        this.numRecentMessages = value;
    }

    @Override
    public JSONObject toJSON() {
         JSONObject json = super.toJSON();
         json.put("numConversations", this.numConversations);
         json.put("numRecentMessages", this.numRecentMessages);
         return json;
    }
}
