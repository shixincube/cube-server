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

package cube.console.mgmt;

import cube.common.JSONable;
import org.json.JSONObject;

/**
 * 用户。
 */
public class User implements JSONable {

    public final Long id;

    public final String name;

    public final String avatar;

    public final String displayName;

    public final int role;

    public final String group;

    protected String password;

    public User(Long id, String name, String avatar, String displayName, int role, String group) {
        this(id, name, avatar, displayName, role, group, null);
    }

    public User(Long id, String name, String avatar, String displayName, int role, String group, String password) {
        this.id = id;
        this.name = name;
        this.avatar = avatar;
        this.displayName = displayName;
        this.role = role;
        this.group = group;
        this.password = password;
    }

    public boolean validatePassword(String password) {
        return password.equalsIgnoreCase(this.password);
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("name", this.name);
        json.put("avatar", this.avatar);
        json.put("displayName", this.displayName);
        json.put("role", this.role);
        json.put("group", this.group);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
