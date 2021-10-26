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

package cube.app.server.account;

import cube.app.server.util.PhoneNumbers;
import cube.common.JSONable;
import org.json.JSONObject;

/**
 * 账号实体。
 */
public class Account implements JSONable {

    public final long id;

    public final String account;

    public final String phone;

    public final String password;

    public String name;

    public String avatar;

    public final int state;

    public String region;

    public String department;

    public long registration;

    protected long timestamp = System.currentTimeMillis();

    public Account(long id, String account, String phone, String password, String name, String avatar, int state) {
        this.id = id;
        this.account = account;
        this.phone = phone;
        this.password = password;
        this.name = name;
        this.avatar = avatar;
        this.state = state;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("id", this.id);
        json.put("account", this.account);
        json.put("phone", PhoneNumbers.desensitize(this.phone));
        json.put("name", this.name);
        json.put("avatar", this.avatar);
        json.put("state", this.state);
        json.put("region", null != this.region ? this.region : "");
        json.put("department", null != this.department ? this.department : "");
        json.put("registration", this.registration);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = this.toJSON();
        json.remove("account");
        json.remove("registration");
        return json;
    }

    public JSONObject toFullJSON() {
        JSONObject json = new JSONObject();
        json.put("id", this.id);
        json.put("account", this.account);
        json.put("phone", this.phone);
        json.put("password", this.password);
        json.put("name", this.name);
        json.put("avatar", this.avatar);
        json.put("state", this.state);
        json.put("region", null != this.region ? this.region : "");
        json.put("department", null != this.department ? this.department : "");
        json.put("registration", this.registration);
        return json;
    }
}
