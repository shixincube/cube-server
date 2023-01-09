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

package cube.robot;

import cell.core.talk.TalkContext;
import cube.common.JSONable;
import org.json.JSONObject;

/**
 * Roboengine 的账号描述。
 */
public class Account implements JSONable {

    public final static int STATE_NORMAL = 0;

    public final static int STATE_FORBIDDEN = 3;

    public long id;

    public String name;

    public String password;

    public boolean isAdmin;

    public String avatar;

    public String fullName;

    public long creationTime;

    public int state;

    public String token;

    public String lastAddress;

    public long lastLoginTime;

    public JSONObject lastDevice;

    public TalkContext talkContext;

    public boolean online = false;

    public Account(long id, String name, String password, boolean isAdmin, String avatar,
                   String fullName, long creationTime, int state) {
        this.id = id;
        this.name = name;
        this.password = password;
        this.isAdmin = isAdmin;
        this.avatar = avatar;
        this.fullName = fullName;
        this.creationTime = creationTime;
        this.state = state;
    }

    public Account(JSONObject json) {
        this.id = json.has("id") ? json.getLong("id") : 0;
        this.name = json.getString("name");
        this.isAdmin = json.getBoolean("isAdmin");
        this.avatar = json.getString("avatar");
        this.fullName = json.getString("fullName");
        this.creationTime = json.getLong("creationTime");
        this.state = json.getInt("state");
        if (json.has("lastLoginTime")) {
            this.lastLoginTime = json.getLong("lastLoginTime");
        }
        if (json.has("lastDevice")) {
            this.lastDevice = json.getJSONObject("lastDevice");
        }
        if (json.has("token")) {
            this.token = json.getString("token");
        }
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("id", this.id);
        json.put("name", this.name);
        json.put("isAdmin", this.isAdmin);
        json.put("avatar", this.avatar);
        json.put("fullName", this.fullName);
        json.put("creationTime", this.creationTime);
        json.put("state", this.state);
        json.put("online", this.online);
        json.put("lastLoginTime", this.lastLoginTime);
        if (null != this.lastAddress) {
            json.put("lastAddress", this.lastAddress);
        }
        if (null != this.lastDevice) {
            json.put("lastDevice", this.lastDevice);
        }
        if (null != this.token) {
            json.put("token", this.token);
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return toJSON();
    }
}
