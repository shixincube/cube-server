/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 Ambrose Xu.
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

package cube.common.entity;

import cube.common.JSONable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 联系人搜索结果。
 */
public class ContactSearchResult implements JSONable {

    private long timestamp;

    private String keyword;

    private List<Contact> contactList;

    private List<Group> groupList;

    public ContactSearchResult(String keyword) {
        this.timestamp = System.currentTimeMillis();
        this.keyword = keyword;
        this.contactList = new ArrayList<>();
        this.groupList = new ArrayList<>();
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public List<Contact> getContactList() {
        return this.contactList;
    }

    public void addContact(Contact contact) {
        if (this.contactList.contains(contact)) {
            return;
        }

        this.contactList.add(contact);
    }

    public List<Group> getGroupList() {
        return this.groupList;
    }

    public void addGroup(Group group) {
        if (this.groupList.contains(group)) {
            return;
        }

        this.groupList.add(group);
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();

        json.put("keyword", this.keyword);

        JSONArray contacts = new JSONArray();
        for (Contact contact : this.contactList) {
            contacts.put(contact.toCompactJSON());
        }
        json.put("contactList", contacts);

        JSONArray groups = new JSONArray();
        for (Group group : this.groupList) {
            groups.put(group.toCompactJSON());
        }
        json.put("groupList", groups);

        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
