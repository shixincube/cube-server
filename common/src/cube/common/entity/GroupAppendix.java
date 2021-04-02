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

import java.util.*;

/**
 * 群组的附录。
 */
public class GroupAppendix extends Entity {

    private Group owner;

    private String notice;

    /**
     * 每个成员的备注名。公开信息。
     */
    private HashMap<Long, String> memberRemarks;

    /**
     * 成员对该群的备注内容。私有信息。
     */
    private HashMap<Long, String> remarks;

    /**
     * 成员的上下文。私有信息。
     */
    private HashMap<Long, JSONObject> contexts;

    /**
     * 入群申请人列表。
     */
    private List<JSONObject> applicants;

    /**
     * 构造函数。
     *
     * @param owner
     */
    public GroupAppendix(Group owner) {
        super();
        this.uniqueKey = owner.getUniqueKey() + "_appendix";
        this.owner = owner;
        this.memberRemarks = new HashMap<>();

        this.remarks = new HashMap<>();
        this.contexts = new HashMap<>();
        this.applicants = new ArrayList<>();
    }

    /**
     * 构造函数。
     *
     * @param owner
     * @param json
     */
    public GroupAppendix(Group owner, JSONObject json) {
        super();
        this.uniqueKey = owner.getUniqueKey() + "_appendix";
        this.owner = owner;
        this.memberRemarks = new HashMap<>();

        this.remarks = new HashMap<>();
        this.contexts = new HashMap<>();
        this.applicants = new ArrayList<>();

        if (json.has("notice")) {
            this.notice = json.getString("notice");
        }

        if (json.has("memberRemarks")) {
            JSONArray array = json.getJSONArray("memberRemarks");
            for (int i = 0; i < array.length(); ++i) {
                JSONObject item = array.getJSONObject(i);
                Long id = item.getLong("id");
                String name = item.getString("name");
                this.memberRemarks.put(id, name);
            }
        }

        if (json.has("remarks")) {
            JSONArray array = json.getJSONArray("remarks");
            for (int i = 0; i < array.length(); ++i) {
                JSONObject item = array.getJSONObject(i);
                Long id = item.getLong("id");
                String remark = item.getString("remark");
                this.remarks.put(id, remark);
            }
        }

        if (json.has("contexts")) {
            JSONArray array = json.getJSONArray("contexts");
            for (int i = 0; i < array.length(); ++i) {
                JSONObject item = array.getJSONObject(i);
                Long id = item.getLong("id");
                JSONObject context = item.getJSONObject("context");
                this.contexts.put(id, context);
            }
        }

        if (json.has("applicants")) {
            JSONArray array = json.getJSONArray("applicants");
            for (int i = 0; i < array.length(); ++i) {
                this.applicants.add(array.getJSONObject(i));
            }
        }
    }

    /**
     * 返回附件所属的群组。
     *
     * @return 返回附件所属的群组。
     */
    public Group getOwner() {
        return this.owner;
    }

    /**
     * 设置群组公告。
     *
     * @param notice
     */
    public void setNotice(String notice) {
        this.resetTimestamp();
        this.notice = notice;
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
    public void remark(Contact member, String content) {
        this.remarks.put(member.getId(), content);
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
        return this.remarks.get(member.getId());
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
     * 按照指定成员类型返回数据。
     *
     * @param member
     * @return
     */
    public JSONObject packJSON(Contact member) {
        JSONObject json = new JSONObject();
        json.put("owner", this.owner.toCompactJSON());

        json.put("notice", (null == this.notice) ? "" : this.notice);

        JSONArray memberRemarkArray = new JSONArray();
        for (Map.Entry<Long, String> e : this.memberRemarks.entrySet()) {
            JSONObject mr = new JSONObject();
            mr.put("id", e.getKey().longValue());
            mr.put("name", e.getValue());
            memberRemarkArray.put(mr);
        }
        json.put("memberRemarks", memberRemarkArray);

        String remarkContent = this.remarks.get(member.getId());
        if (null == remarkContent) {
            remarkContent = "";
        }
        json.put("remark", remarkContent);

        JSONObject context = this.contexts.get(member.getId());
        if (null != context) {
            json.put("context", context);
        }

        if (this.owner.getOwner().equals(member)) {
            JSONArray array = new JSONArray();
            ArrayList<JSONObject> applicants = new ArrayList<>(this.applicants);
            Collections.reverse(applicants);
            for (JSONObject applicantJson : applicants) {
                array.put(applicantJson);
            }
            json.put("applicants", array);
        }

        return json;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("ownerId", this.owner.getId());

        json.put("notice", (null == this.notice) ? "" : this.notice);

        JSONArray memberRemarkArray = new JSONArray();
        for (Map.Entry<Long, String> e : this.memberRemarks.entrySet()) {
            JSONObject mr = new JSONObject();
            mr.put("id", e.getKey().longValue());
            mr.put("name", e.getValue());
            memberRemarkArray.put(mr);
        }
        json.put("memberRemarks", memberRemarkArray);

        JSONArray remarksArray = new JSONArray();
        for (Map.Entry<Long, String> e : this.remarks.entrySet()) {
            JSONObject item = new JSONObject();
            item.put("id", e.getKey().longValue());
            item.put("remark", e.getValue());
            remarksArray.put(item);
        }
        json.put("remarks", remarksArray);

        JSONArray contextsArray = new JSONArray();
        for (Map.Entry<Long, JSONObject> e : this.contexts.entrySet()) {
            JSONObject item = new JSONObject();
            item.put("id", e.getKey().longValue());
            item.put("context", e.getValue());
            contextsArray.put(item);
        }
        json.put("contexts", contextsArray);

        JSONArray applicantArray = new JSONArray();
        for (JSONObject applicantJson : this.applicants) {
            applicantArray.put(applicantJson);
        }
        json.put("applicants", applicantArray);

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
