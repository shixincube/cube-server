/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 Shixin Cube Team.
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

package cube.service.contact;

import cube.common.JSONable;
import cube.common.entity.Contact;
import cube.common.entity.Group;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 群操作时用户捆绑相关成员的元素。
 */
public class GroupBundle implements JSONable {

    public Group group;

    public List<Contact> members;

    public Contact operator;

    public GroupBundle(Group group, Contact member) {
        this.group = group;
        this.members = new ArrayList<>();
        this.members.add(member);
    }

    public GroupBundle(Group group, List<Contact> members) {
        this.group = group;
        this.members = new ArrayList<>(members);
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            // group
            json.put("group", this.group.toJSON());

            // members
            JSONArray memberList = new JSONArray();
            for (Contact member : this.members) {
                memberList.put(member.toCompactJSON());
            }
            json.put("modified", memberList);

            // operator
            if (null != this.operator) {
                json.put("operator", this.operator.toCompactJSON());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return json;
    }

    public JSONObject toCompactJSON() {
        JSONObject json = new JSONObject();
        try {
            // group
            json.put("group", this.group.toCompactJSON());

            // members
            JSONArray memberList = new JSONArray();
            for (Contact member : this.members) {
                memberList.put(member.toCompactJSON());
            }
            json.put("modified", memberList);

            // operator
            if (null != this.operator) {
                json.put("operator", this.operator.toCompactJSON());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return json;
    }
}
