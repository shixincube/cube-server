/**
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Shixin Cube Team.
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

import cell.util.json.JSONArray;
import cell.util.json.JSONException;
import cell.util.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * 群组实体。
 */
public class Group extends Contact implements Comparable<Group> {

    /**
     * 群的所有人。
     */
    private Contact owner;

    /**
     * 创建时间。
     */
    private long creationTime;

    /**
     * 最近一次活跃时间。
     */
    private long lastActiveTime;

    /**
     * 群组的成员列表。
     */
    private Vector<Contact> members;

    /**
     * 群组状态。
     */
    private GroupState state;

    /**
     * 构造函数。
     *
     * @param id 群组 ID 。
     * @param domain 群组域。
     * @param name 群组显示名。
     * @param owner 群组所有人。
     */
    public Group(Long id, String domain, String name, Contact owner, long creationTime) {
        super(id, domain, name);
        this.owner = owner;
        this.creationTime = creationTime;
        this.lastActiveTime = creationTime;
        this.state = GroupState.Normal;
        this.members = new Vector<>();
        this.members.add(owner);
    }

    /**
     * 构造函数。
     *
     * @param json JSON 形式的群组数据。
     */
    public Group(JSONObject json) {
        super(json);
        this.members = new Vector<>();

        try {
            JSONObject ownerJson = json.getJSONObject("owner");
            this.owner = new Contact(ownerJson, this.domain.getName());
            this.members.add(this.owner);

            this.creationTime = json.getLong("creation");
            this.lastActiveTime = json.getLong("lastActive");
            this.state = GroupState.parse(json.getInt("state"));

            if (json.has("members")) {
                JSONArray array = json.getJSONArray("members");
                for (int i = 0, len = array.length(); i < len; ++i) {
                    JSONObject member = array.getJSONObject(i);
                    Contact contact = new Contact(member, this.domain.getName());
                    this.addMember(contact);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * 返回群的所有人。
     *
     * @return
     */
    public Contact getOwner() {
        return this.owner;
    }

    /**
     * 设置群的所有人。
     *
     * @param owner
     */
    public void setOwner(Contact owner) {
        this.owner = owner;
    }

    /**
     * 返回群的创建时间。
     *
     * @return
     */
    public long getCreationTime() {
        return this.creationTime;
    }

    /**
     * 设置创建时间。
     *
     * @param value
     */
    public void setCreationTime(long value) {
        this.creationTime = value;
    }

    /**
     * 返回群的最近一次活跃时间。
     *
     * @return
     */
    public long getLastActiveTime() {
        return this.lastActiveTime;
    }

    /**
     * 设置活跃时间。
     *
     * @param time
     */
    public void setLastActiveTime(long time) {
        this.lastActiveTime = time;
    }

    /**
     * 获取状态。
     *
     * @return 返回状态。
     */
    public GroupState getState() {
        return this.state;
    }

    /**
     * 设置状态。
     *
     * @param state
     */
    public void setState(GroupState state) {
        this.state = state;
    }

    /**
     * 群成员数量。
     *
     * @return
     */
    public int numMembers() {
        return this.members.size();
    }

    /**
     * 群组内是否包含指定成员。
     *
     * @param contactId
     * @return
     */
    public boolean hasMember(Long contactId) {
        for (int i = 0, size = this.members.size(); i < size; ++i) {
            Contact member = this.members.get(i);
            if (member.getId().longValue() == contactId.longValue()) {
                return true;
            }
        }
        return false;
    }

    public void addMember(Contact contact) {
        if (this.members.contains(contact)) {
            return;
        }

        this.members.add(contact);
    }

    public Contact removeMember(Long contactId) {
        Contact contact = this.getMember(contactId);
        if (null == contact) {
            return null;
        }

        return this.removeMember(contact);
    }

    public Contact removeMember(Contact contact) {
        if (contact.getId().longValue() == this.owner.getId().longValue()) {
            return null;
        }

        if (this.members.remove(contact)) {
            return contact;
        }

        return null;
    }

    public List<Contact> getMembers() {
        return new ArrayList<>(this.members);
    }

    public Contact getMember(Long id) {
        for (int i = 0, size = this.members.size(); i < size; ++i) {
            Contact member = this.members.get(i);
            if (member.getId().longValue() == id.longValue()) {
                return member;
            }
        }

        return null;
    }

    @Override
    public boolean equals(Object object) {
        if (null != object && object instanceof Group) {
            Group other = (Group) object;
            if (!other.getId().equals(this.getId()) || !other.getName().equals(this.getName())
                || !other.owner.getId().equals(this.owner.getId())) {
                return false;
            }

            for (int i = 0, size = this.members.size(); i < size; ++i) {
                Contact contact = this.members.get(i);
                Contact otherContact = other.getMember(contact.getId());
                if (null == otherContact) {
                    return false;
                }

                if (!contact.equals(otherContact)) {
                    return false;
                }
            }

            return true;
        }

        return false;
    }

    @Override
    public int hashCode() {
        return (this.getId().hashCode() + this.getDomain().hashCode() * 3);
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        try {
            if (json.has("devices")) {
                json.remove("devices");
            }

            JSONArray array = new JSONArray();
            for (Contact contact : this.members) {
                array.put(contact.toCompactJSON());
            }
            json.put("members", array);

            json.put("owner", this.owner.toCompactJSON());
            json.put("creation", this.creationTime);
            json.put("lastActive", this.lastActiveTime);
            json.put("state", this.state.getCode());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = super.toCompactJSON();
        try {
            json.put("owner", this.owner.toCompactJSON());
            json.put("creation", this.creationTime);
            json.put("lastActive", this.lastActiveTime);
            json.put("state", this.state.getCode());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    public JSONObject toJSON(GroupState state) {
        JSONObject json = this.toJSON();
        json.remove("state");
        try {
            json.put("state", state.getCode());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    @Override
    public int compareTo(Group other) {
        return (int)(other.lastActiveTime - this.lastActiveTime);
    }

    /**
     * 判断是否是群组结构的 JSON 数据格式。
     *
     * @param json 待判断的 JSON 数据。
     * @return 如果 JSON 符合群组数据结构返回 {@code true} 。
     */
    public static boolean isGroup(JSONObject json) {
        if (json.has("members")) {
            return true;
        }
        else {
            return false;
        }
    }
}
