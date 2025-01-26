/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.hub.event;

import cube.common.entity.Contact;
import org.json.JSONObject;

/**
 * 添加朋友事件。
 */
public class AddFriendEvent extends WeChatEvent {

    public final static String NAME = "AddFriend";

    private Contact friend;

    private String postscript;

    private String remarkName;

    public AddFriendEvent(Contact friend) {
        super(NAME);
        this.friend = friend;
    }

    public AddFriendEvent(JSONObject json) {
        super(json);
        this.friend = new Contact(json.getJSONObject("friend"));
        if (json.has("postscript")) {
            this.postscript = json.getString("postscript");
        }
        if (json.has("remarkName")) {
            this.remarkName = json.getString("remarkName");
        }
    }

    public Contact getFriend() {
        return this.friend;
    }

    public void setPostscript(String postscript) {
        this.postscript = postscript;
    }

    public String getPostscript() {
        return this.postscript;
    }

    public void setRemarkName(String remarkName) {
        this.remarkName = remarkName;
    }

    public String getRemarkName() {
        return this.remarkName;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("friend", this.friend.toJSON());

        if (null != this.postscript) {
            json.put("postscript", this.postscript);
        }

        if (null != this.remarkName) {
            json.put("remarkName", this.remarkName);
        }

        return json;
    }
}
