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

package cube.common.entity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * 群组实体。
 */
public class Group extends AbstractContact implements Comparable<Group> {

    /**
     * 群组所有人的 ID 。
     */
    private Long ownerId;

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
    private List<Long> memberIdList;

    /**
     * 群组的成员实体列表。
     * 该属性仅供客户端使用。
     */
    private List<Contact> memberList;

    /**
     * 群组的标签。
     */
    private String tag = GroupTag.Public;

    /**
     * 群组状态。
     */
    private GroupState state;

    /**
     * 构造函数。
     */
    public Group() {
        super();
        this.ownerId = 0L;
        this.creationTime = System.currentTimeMillis();
        this.lastActiveTime = System.currentTimeMillis();
        this.state = GroupState.Normal;
        this.memberIdList = new Vector<>();
    }

    /**
     * 构造函数。
     *
     * @param id 群组 ID 。
     * @param domain 群组域。
     * @param name 群组显示名。
     * @param ownerId 群组的群主 ID 。
     * @param creationTime 创建时间。
     */
    public Group(Long id, String domain, String name, Long ownerId, long creationTime) {
        super(id, domain, name);
        this.ownerId = ownerId;
        this.creationTime = creationTime;
        this.lastActiveTime = creationTime;
        this.state = GroupState.Normal;
        this.memberIdList = new Vector<>();
        this.memberIdList.add(this.ownerId);
    }

    /**
     * 构造函数。
     *
     * @param json JSON 形式的群组数据。
     */
    public Group(JSONObject json) {
        super(json, null);
        this.memberIdList = new Vector<>();

        this.tag = json.getString("tag");
        this.ownerId = json.getLong("ownerId");
        this.creationTime = json.getLong("creation");
        this.lastActiveTime = json.getLong("lastActive");
        this.state = GroupState.parse(json.getInt("state"));

        this.memberIdList.add(this.ownerId);

        if (json.has("members")) {
            JSONArray array = json.getJSONArray("members");
            for (int i = 0, len = array.length(); i < len; ++i) {
                this.addMember(array.getLong(i));
            }
        }

        if (json.has("memberContacts")) {
            JSONArray array = json.getJSONArray("memberContacts");
            for (int i = 0; i < array.length(); ++i) {
                this.addMember(new Contact(array.getJSONObject(i)));
            }
        }
    }

    /**
     * 返回群的标签。
     *
     * @return 返回群的标签。
     */
    public String getTag() {
        return this.tag;
    }

    /**
     * 设置群的标签。
     *
     * @param tag 指定新标签。
     */
    public void setTag(String tag) {
        this.tag = tag;
    }

    /**
     * 返回群的群主 ID 。
     *
     * @return 返回群的群主 ID 。
     */
    public Long getOwnerId() {
        return this.ownerId;
    }

    /**
     * 设置群的群主 ID 。
     *
     * @param ownerId 指定群的群主 ID 。
     */
    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
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
        return this.memberIdList.size();
    }

    /**
     * 群组内是否包含指定成员。
     *
     * @param contactId 指定待判断的成员 ID 。
     * @return 如果包含指定成员返回 {@code true} ，否则返回 {@code false} 。
     */
    public boolean hasMember(Long contactId) {
        return this.memberIdList.contains(contactId);
    }

    public void addMemberId(Long memberId) {
        if (this.memberIdList.contains(memberId)) {
            return;
        }

        this.memberIdList.add(memberId);
    }

    /**
     * 添加成员。
     *
     * @param contactId 指定待添加联系人 ID 。
     * @return 如果添加成功返回添加的联系人 ID ，否则返回 {@code null} 值。
     */
    public Long addMember(Long contactId) {
        if (this.memberIdList.contains(contactId)) {
            return null;
        }

        this.memberIdList.add(contactId);
        return contactId;
    }

    /**
     * 移除成员。
     * 注意：群主无法被移除。
     *
     * @param contactId 指定待移除的联系人 ID 。
     * @return 如果移除成功返回被移除的联系人，否则返回 {@code null} 值。
     */
    public Long removeMember(Long contactId) {
        if (this.ownerId.equals(contactId)) {
            return null;
        }

        return this.memberIdList.remove(contactId) ? contactId : null;
    }

    /**
     * 获取成员 ID 列表。
     *
     * @return 返回成员 ID 列表的副本。
     */
    public List<Long> getMembers() {
        return new ArrayList<>(this.memberIdList);
    }

    public void addMember(Contact contact) {
        if (null == this.memberList) {
            this.memberList = new ArrayList<>();
        }

        this.memberList.add(contact);
    }

    public void removeMember(Contact contact) {
        if (null == this.memberList) {
            return;
        }

        this.memberList.remove(contact);
    }

    public List<Contact> getMemberList() {
        return this.memberList;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (null != object && object instanceof Group) {
            Group other = (Group) object;
            if (other.getId().equals(this.getId()) && other.ownerId.equals(this.ownerId)) {
                return true;
            }
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

        JSONArray array = new JSONArray();
        for (Long memberId : this.memberIdList) {
            array.put(memberId);
        }
        json.put("members", array);

        json.put("tag", this.tag);
        json.put("ownerId", this.ownerId.longValue());
        json.put("creation", this.creationTime);
        json.put("lastActive", this.lastActiveTime);
        json.put("state", this.state.code);

        if (null != this.memberList) {
            JSONArray contacts = new JSONArray();
            for (Contact contact : this.memberList) {
                contacts.put(contact.toJSON());
            }
            json.put("memberContacts", contacts);
        }

        return json;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = super.toCompactJSON();
        json.put("tag", this.tag);
        json.put("ownerId", this.ownerId.longValue());
        json.put("creation", this.creationTime);
        json.put("lastActive", this.lastActiveTime);
        json.put("state", this.state.code);
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
        json.put("state", state.code);
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
        if (json.has("members") || json.has("ownerId")) {
            return true;
        }
        else {
            return false;
        }
    }
}
