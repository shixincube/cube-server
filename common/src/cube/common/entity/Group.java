/**
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

package cube.common.entity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * 群组实体。
 */
public class Group extends Contact implements Comparable<Group> {

    /**
     * 群的群组。
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
     * @param owner 群组的群主。
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
    }

    /**
     * 返回群的群主。
     *
     * @return 返回群的群主。
     */
    public Contact getOwner() {
        return this.owner;
    }

    /**
     * 设置群的群主。
     *
     * @param owner 指定群的群主。
     */
    public void setOwner(Contact owner) {
        this.owner = owner;
    }

    /**
     * 返回群的创建时间。
     *
     * @return 返回群的创建时间。
     */
    public long getCreationTime() {
        return this.creationTime;
    }

    /**
     * 设置创建时间。
     *
     * @param value 设置创建时间。
     */
    public void setCreationTime(long value) {
        this.creationTime = value;
    }

    /**
     * 返回群的最近一次活跃时间。
     *
     * @return 返回群的最近一次活跃时间。
     */
    public long getLastActiveTime() {
        return this.lastActiveTime;
    }

    /**
     * 设置活跃时间。
     *
     * @param time 指定活跃时间。
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
     * @param state 群组状态。
     */
    public void setState(GroupState state) {
        this.state = state;
    }

    /**
     * 群成员数量。
     *
     * @return 返回群成员数量。
     */
    public int numMembers() {
        return this.members.size();
    }

    /**
     * 群组内是否包含指定成员。
     *
     * @param contactId 指定待判断的成员 ID 。
     * @return 如果包含指定成员返回 {@code true} ，否则返回 {@code false} 。
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

    /**
     * 添加成员。
     *
     * @param contact 指定待添加联系人。
     * @return 如果添加成功返回添加的联系人，否则返回 {@code null} 值。
     */
    public Contact addMember(Contact contact) {
        if (this.members.contains(contact)) {
            return null;
        }

        this.members.add(contact);
        return contact;
    }

    /**
     * 移除成员。
     * 注意：群组无法被移除。
     *
     * @param contactId 指定待移除的联系人 ID 。
     * @return 如果移除成功返回被移除的联系人，否则返回 {@code null} 值。
     */
    public Contact removeMember(Long contactId) {
        Contact contact = this.getMember(contactId);
        if (null == contact) {
            return null;
        }

        return this.removeMember(contact);
    }

    /**
     * 移除成员。
     * 注意：群组无法被移除。
     *
     * @param contact 指定待移除的联系人。
     * @return 如果移除成功返回被移除的联系人，否则返回 {@code null} 值。
     */
    public Contact removeMember(Contact contact) {
        if (contact.getId().longValue() == this.owner.getId().longValue()) {
            return null;
        }

        if (this.members.remove(contact)) {
            return contact;
        }

        return null;
    }

    /**
     * 获取成员列表。
     *
     * @return 返回成员列表的副本。
     */
    public List<Contact> getMembers() {
        return new ArrayList<>(this.members);
    }

    /**
     * 获取指定 ID 的成员。
     *
     * @param id 指定成员 ID 。
     * @return 返回指定 ID 的成员实例。没有该成员时返回 {@code null} 值。
     */
    public Contact getMember(Long id) {
        for (int i = 0, size = this.members.size(); i < size; ++i) {
            Contact member = this.members.get(i);
            if (member.getId().longValue() == id.longValue()) {
                return member;
            }
        }

        return null;
    }

    /**
     * 更新指定的成员数据。
     *
     * @param member 指定新的成员数据。
     * @return 如果更新成功返回成员实例，否则返回 {@code null} 值。
     */
    public Contact updateMember(Contact member) {
        if (!this.members.remove(member)) {
            return null;
        }

        this.members.add(member);

        if (member.getId().longValue() == this.owner.getId().longValue()) {
            this.owner = member;
        }

        return member;
    }

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return (this.getId().hashCode() + this.getDomain().hashCode() * 3);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
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
        return json;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = super.toCompactJSON();
        json.put("owner", this.owner.toCompactJSON());
        json.put("creation", this.creationTime);
        json.put("lastActive", this.lastActiveTime);
        json.put("state", this.state.getCode());
        return json;
    }

    /**
     * 序列化为 JSON 格式，并使用指定的状态设置序列化的状态字段。
     *
     * @param state 指定序列化时的状态字段数据。
     * @return 返回 JSON 格式。
     */
    public JSONObject toJSON(GroupState state) {
        JSONObject json = this.toJSON();
        json.remove("state");
        json.put("state", state.getCode());
        return json;
    }

    /**
     * {@inheritDoc}
     */
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
        if (json.has("members") || json.has("owner")) {
            return true;
        }
        else {
            return false;
        }
    }
}
