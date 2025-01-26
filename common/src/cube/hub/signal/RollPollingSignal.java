/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.hub.signal;

import cube.common.entity.ConversationType;
import org.json.JSONObject;

/**
 * 轮询信令。
 */
public class RollPollingSignal extends Signal {

    public final static String NAME = "RollPolling";

    private ConversationType conversationType;

    private String conversationName;

    private int limit = 5;

    public RollPollingSignal(String code, ConversationType conversationType, String conversationName) {
        super(NAME);
        setCode(code);
        this.conversationType = conversationType;
        this.conversationName = conversationName;
    }

    public RollPollingSignal(JSONObject json) {
        super(json);
        this.conversationType = ConversationType.parse(json.getInt("conversationType"));
        this.conversationName = json.getString("conversationName");
        this.limit = json.getInt("limit");
    }

    public ConversationType getConversationType() {
        return this.conversationType;
    }

    public String getConversationName() {
        return this.conversationName;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public int getLimit() {
        return this.limit;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("conversationType", this.conversationType.code);
        json.put("conversationName", this.conversationName);
        json.put("limit", this.limit);
        return json;
    }
}
