/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.hub.signal;

import cube.common.entity.Contact;
import org.json.JSONObject;

/**
 * 添加好友信令。
 */
public class AddFriendSignal extends Signal {

    public final static String NAME = "AddFriend";

    private Contact account;

    private String searchKeyword;

    /**
     * 申请附言。
     */
    private String postscript;

    /**
     * 备注名。
     */
    private String remarkName;

    public AddFriendSignal(String channelCode, String searchKeyword) {
        super(NAME);
        setCode(channelCode);
        this.searchKeyword = searchKeyword;
    }

    public AddFriendSignal(JSONObject json) {
        super(json);
        this.searchKeyword = json.getString("searchKeyword");

        if (json.has("account")) {
            this.account = new Contact(json.getJSONObject("account"));
        }
        if (json.has("postscript")) {
            this.postscript = json.getString("postscript");
        }
        if (json.has("remarkName")) {
            this.remarkName = json.getString("remarkName");
        }
    }

    public String getSearchKeyword() {
        return this.searchKeyword;
    }

    public void setAccount(Contact account) {
        this.account = account;
    }

    public Contact getAccount() {
        return this.account;
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
        json.put("searchKeyword", this.searchKeyword);

        if (null != this.account) {
            json.put("account", this.account.toJSON());
        }
        if (null != this.postscript) {
            json.put("postscript", this.postscript);
        }
        if (null != this.remarkName) {
            json.put("remarkName", this.remarkName);
        }
        return json;
    }
}
