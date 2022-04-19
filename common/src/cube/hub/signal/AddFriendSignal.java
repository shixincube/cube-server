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

    public AddFriendSignal(Contact account, String searchKeyword) {
        super(NAME);
        this.account = account;
        this.searchKeyword = searchKeyword;
    }

    public AddFriendSignal(JSONObject json) {
        super(json);
        this.account = new Contact(json.getJSONObject("account"));
        this.searchKeyword = json.getString("searchKeyword");
        if (json.has("postscript")) {
            this.postscript = json.getString("postscript");
        }
        if (json.has("remarkName")) {
            this.remarkName = json.getString("remarkName");
        }
    }

    public Contact getAccount() {
        return this.account;
    }

    public String getSearchKeyword() {
        return this.searchKeyword;
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
        json.put("account", this.account.toJSON());
        json.put("searchKeyword", this.searchKeyword);
        if (null != this.postscript) {
            json.put("postscript", this.postscript);
        }
        if (null != this.remarkName) {
            json.put("remarkName", this.remarkName);
        }
        return json;
    }
}
