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

import java.util.*;

/**
 * 群组的附录。
 */
public class GroupAppendix extends Entity {

    private Group group;

    /**
     * 群组公告。
     */
    private String notice;

    /**
     * 公告操作员 ID 。
     */
    private Long noticeOperatorId;

    /**
     * 公告时间。
     */
    private long noticeTime;

    /**
     * 每个成员的备注名。公开信息。
     */
    private HashMap<Long, String> memberRemarks;

    /**
     * 成员对该群的备注内容。私有信息。
     */
    private HashMap<Long, String> remarkMap;

    /**
     * 成员对该群是否进行了关注。私有信息。
     */
    private HashMap<Long, Boolean> followingMap;

    /**
     * 成员对该群是否设置了显示成员名。私有信息。
     */
    private HashMap<Long, Boolean> memberNameDisplayedMap;

    /**
     * 成员的上下文。私有信息。
     */
    private HashMap<Long, JSONObject> contextMap;

    /**
     * 当前有效的通讯 ID 。
     */
    private Long commId;

    /**
     * 入群申请人列表。
     */
    private List<JSONObject> applicants;

    /**
     * 构造函数。
     *
     * @param group
     */
    public GroupAppendix(Group group) {
        super();
        this.uniqueKey = group.getUniqueKey() + "_appendix";
        this.group = group;
        this.domain = group.getDomain();
        this.memberRemarks = new HashMap<>();

        this.remarkMap = new HashMap<>();
        this.followingMap = new HashMap<>();
        this.memberNameDisplayedMap = new HashMap<>();
        this.contextMap = new HashMap<>();
        this.applicants = new ArrayList<>();

        this.commId = 0L;
    }

    /**
     * 构造函数。
     *
     * @param group
     * @param json
     */
    public GroupAppendix(Group group, JSONObject json) {
        super();
        this.uniqueKey = group.getUniqueKey() + "_appendix";
        this.group = group;
        this.domain = group.getDomain();
        this.memberRemarks = new HashMap<>();

        this.remarkMap = new HashMap<>();
        this.memberNameDisplayedMap = new HashMap<>();
        this.followingMap = new HashMap<>();
        this.contextMap = new HashMap<>();
        this.applicants = new ArrayList<>();

        this.commId = 0L;

        if (json.has("notice")) {
            this.notice = json.getString("notice");
            this.noticeOperatorId = json.has("noticeOperatorId") ? json.getLong("noticeOperatorId") : null;
            this.noticeTime = json.has("noticeTime") ? json.getLong("noticeTime") : 0;
        }

        if (json.has("memberRemarks")) {
            JSONObject data = json.getJSONObject("memberRemarks");
            Iterator<String> iterator = data.keys();
            while (iterator.hasNext()) {
                String key = iterator.next();
                this.memberRemarks.put(Long.parseLong(key), data.getString(key));
            }
        }

        if (json.has("remarkMap")) {
            JSONObject data = json.getJSONObject("remarkMap");
            Iterator<String> iterator = data.keys();
            while (iterator.hasNext()) {
                String key = iterator.next();
                this.remarkMap.put(Long.parseLong(key), data.getString(key));
            }
        }

        if (json.has("followingMap")) {
            JSONObject data = json.getJSONObject("followingMap");
            Iterator<String> iterator = data.keys();
            while (iterator.hasNext()) {
                String key = iterator.next();
                this.followingMap.put(Long.parseLong(key), data.getBoolean(key));
            }
        }

        if (json.has("memberNameDisplayedMap")) {
            JSONObject data = json.getJSONObject("memberNameDisplayedMap");
            Iterator<String> iterator = data.keys();
            while (iterator.hasNext()) {
                String key = iterator.next();
                this.memberNameDisplayedMap.put(Long.parseLong(key), data.getBoolean(key));
            }
        }

        if (json.has("contextMap")) {
            JSONObject data = json.getJSONObject("contextMap");
            Iterator<String> iterator = data.keys();
            while (iterator.hasNext()) {
                String key = iterator.next();
                this.contextMap.put(Long.parseLong(key), data.getJSONObject(key));
            }
        }

        if (json.has("applicants")) {
            JSONArray array = json.getJSONArray("applicants");
            for (int i = 0; i < array.length(); ++i) {
                this.applicants.add(array.getJSONObject(i));
            }
        }

        if (json.has("commId")) {
            this.commId = json.getLong("commId");
        }
    }

    /**
     * 返回附件所属的群组。
     *
     * @return 返回附件所属的群组。
     */
    public Group getGroup() {
        return this.group;
    }

    /**
     * 设置群组公告。
     *
     * @param notice 指定公告内容。
     * @param contact 指定操作公告的联系人。
     */
    public boolean setNotice(String notice, Contact contact) {
        if (!contact.getId().equals(this.group.getOwnerId())) {
            return false;
        }

        this.resetTimestamp();
        this.notice = notice;
        this.noticeOperatorId = contact.getId();
        this.noticeTime = this.timestamp;
        return true;
    }

    /**
     * 获取指定成员的备注。
     *
     * @param memberId
     * @return
     */
    public String getMemberRemarkName(Long memberId) {
        return this.memberRemarks.get(memberId);
    }

    /**
     * 设置成员的备注。
     *
     * @param memberId
     * @param name
     */
    public void setMemberRemarkName(Long memberId, String name) {
        this.memberRemarks.put(memberId, name);
    }

    /**
     * 指定成员备注该群组的信息。
     *
     * @param member
     * @param content
     */
    public void remarkGroup(Contact member, String content) {
        this.remarkMap.put(member.getId(), content);
        this.resetTimestamp();
    }

    /**
     * 获取指定成员对该群组的备注信息。
     *
     * @param member
     * @return
     */
    public String getRemark(Contact member) {
        this.resetTimestamp();
        return this.remarkMap.get(member.getId());
    }

    /**
     * 指定成员是否关注该群组。
     *
     * @param member
     * @param following
     */
    public void setFollowing(Contact member, boolean following) {
        this.followingMap.put(member.getId(), following);
        this.resetTimestamp();
    }

    /**
     * 获取指定成员是否关注了该群组。
     *
     * @param member
     * @return
     */
    public boolean getFollowing(Contact member) {
        Boolean following = this.followingMap.get(member.getId());
        return (null != following ? following : false);
    }

    /**
     * 添加申请人。
     *
     * @param contactId
     * @param postscript
     */
    public void addApplicant(long contactId, String postscript) {
        for (JSONObject data : this.applicants) {
            long id = data.getLong("id");
            if (id == contactId) {
                // 已经有记录
                data.put("time", System.currentTimeMillis());
                data.put("postscript", postscript);
                return;
            }
        }

        JSONObject data = new JSONObject();
        data.put("id", contactId);
        data.put("time", System.currentTimeMillis());
        data.put("postscript", postscript);
        data.put("agreed", false);
        data.put("agreedTime", 0);
        this.applicants.add(data);
    }

    /**
     * 更新申请人。
     *
     * @param contactId
     * @param agreed
     */
    public void updateApplicant(long contactId, boolean agreed) {
        for (JSONObject data : this.applicants) {
            long id = data.getLong("id");
            if (id == contactId) {
                // 找到记录
                data.put("agreed", agreed);
                data.put("agreedTime", System.currentTimeMillis());
                break;
            }
        }
    }

    /**
     * 设置通讯 ID 。
     *
     * @param commId
     */
    public void setCommId(Long commId) {
        this.commId = commId;
        this.resetTimestamp();
    }

    /**
     * 返回通讯 ID 。
     *
     * @return
     */
    public Long getCommId() {
        return this.commId;
    }

    /**
     * 按照指定成员类型返回数据。
     *
     * @param memberId
     * @return
     */
    public JSONObject packJSON(Long memberId) {
        JSONObject json = new JSONObject();
        json.put("groupId", this.group.getId());

        json.put("notice", (null == this.notice) ? "" : this.notice);
        json.put("noticeOperatorId", (null == this.noticeOperatorId) ? 0L : this.noticeOperatorId.longValue());
        json.put("noticeTime", this.noticeTime);

        JSONArray memberRemarkArray = new JSONArray();
        for (Map.Entry<Long, String> e : this.memberRemarks.entrySet()) {
            JSONObject mr = new JSONObject();
            mr.put("id", e.getKey().longValue());
            mr.put("name", e.getValue());
            memberRemarkArray.put(mr);
        }
        json.put("memberRemarks", memberRemarkArray);

        String remarkContent = this.remarkMap.get(memberId);
        if (null == remarkContent) {
            remarkContent = "";
        }
        json.put("remark", remarkContent);

        Boolean following = this.followingMap.get(memberId);
        if (null != following) {
            json.put("following", following.booleanValue());
        }
        else {
            json.put("following", false);
        }

        Boolean display = this.memberNameDisplayedMap.get(memberId);
        if (null != display) {
            json.put("memberNameDisplayed", display.booleanValue());
        }
        else {
            json.put("memberNameDisplayed", false);
        }

        JSONObject context = this.contextMap.get(memberId);
        if (null != context) {
            json.put("context", context);
        }

        if (this.group.getOwnerId().equals(memberId)) {
            JSONArray array = new JSONArray();
            ArrayList<JSONObject> applicants = new ArrayList<>(this.applicants);
            Collections.reverse(applicants);
            for (JSONObject applicantJson : applicants) {
                array.put(applicantJson);
            }
            json.put("applicants", array);
        }

        json.put("commId", this.commId.longValue());

        return json;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("groupId", this.group.getId());

        json.put("notice", (null == this.notice) ? "" : this.notice);
        json.put("noticeOperatorId", (null == this.noticeOperatorId) ? 0L : this.noticeOperatorId.longValue());
        json.put("noticeTime", this.noticeTime);

        JSONObject memberRemarks = new JSONObject();
        for (Map.Entry<Long, String> e : this.memberRemarks.entrySet()) {
            memberRemarks.put(e.getKey().toString(), e.getValue());
        }
        json.put("memberRemarks", memberRemarks);

        JSONObject remarkMap = new JSONObject();
        for (Map.Entry<Long, String> e : this.remarkMap.entrySet()) {
            remarkMap.put(e.getKey().toString(), e.getValue());
        }
        json.put("remarkMap", remarkMap);

        JSONObject followingMap = new JSONObject();
        for (Map.Entry<Long, Boolean> e : this.followingMap.entrySet()) {
            followingMap.put(e.getKey().toString(), e.getValue().booleanValue());
        }
        json.put("followingMap", followingMap);

        JSONObject memberNameDisplayedMap = new JSONObject();
        for (Map.Entry<Long, Boolean> e : this.memberNameDisplayedMap.entrySet()) {
            memberNameDisplayedMap.put(e.getKey().toString(), e.getValue().booleanValue());
        }
        json.put("memberNameDisplayedMap", memberNameDisplayedMap);

        JSONObject contexts = new JSONObject();
        for (Map.Entry<Long, JSONObject> e : this.contextMap.entrySet()) {
            contexts.put(e.getKey().toString(), e.getValue());
        }
        json.put("contextMap", contexts);

        JSONArray applicantArray = new JSONArray();
        for (JSONObject applicantJson : this.applicants) {
            applicantArray.put(applicantJson);
        }
        json.put("applicants", applicantArray);

        json.put("commId", this.commId.longValue());

        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }

    /**
     * 生成附录的唯一键。
     *
     * @param group
     * @return
     */
    public static String makeUniqueKey(Group group) {
        return group.getUniqueKey() + "_appendix";
    }
}
