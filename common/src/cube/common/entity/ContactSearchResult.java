/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
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
