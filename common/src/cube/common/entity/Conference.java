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

import cell.util.Utils;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 会议实体。
 */
public class Conference extends Entity {

    /**
     * 会议主题。
     */
    private String subject;

    /**
     * 会议密码。
     */
    private String password = "";

    /**
     * 会议摘要。
     */
    private String summary = "";

    /**
     * 创建人。
     */
    private Contact founder;

    /**
     * 创建人 ID 。
     */
    private Long founderId;

    /**
     * 主持人。
     */
    private Contact presenter;

    /**
     * 主持人 ID 。
     */
    private Long presenterId;

    /**
     * 创建时间。
     */
    private long creation;

    /**
     * 计划时间。
     */
    private long scheduleTime;

    /**
     * 过期时间。
     */
    private long expireTime;

    /**
     * 邀请列表。
     */
    private List<Invitation> invitees;

    /**
     * 会议房间。
     */
    private Room room;

    /**
     * 是否已取消。
     */
    private boolean cancelled = false;

    /**
     * 构造函数。
     *
     * @param id
     * @param domainName
     */
    public Conference(Long id, String domainName, Contact founder) {
        super(id, domainName);
        this.uniqueKey = Utils.randomNumberString(8);
        this.founder = founder;
        this.founderId = founder.getId();
        this.invitees = new ArrayList<>();
    }

    /**
     * 构造函数。
     *
     * @param id
     * @param domainName
     * @param code
     * @param subject
     * @param password
     * @param summary
     * @param founderId
     * @param creation
     * @param scheduleTime
     * @param expireTime
     * @param participantGroupId
     * @param commFieldId
     */
    public Conference(Long id, String domainName, String code, String subject, String password, String summary,
        Long founderId, long creation, long scheduleTime, long expireTime, Long participantGroupId, Long commFieldId) {
        super(id, domainName);
        this.uniqueKey = code;
        this.subject = subject;
        this.password = password;
        this.summary = summary;
        this.founderId = founderId;
        this.presenterId = founderId;
        this.creation = creation;
        this.scheduleTime = scheduleTime;
        this.expireTime = expireTime;
        this.room = new Room(participantGroupId, commFieldId);
    }

    /**
     * 构造函数。
     *
     * @param json
     */
    public Conference(JSONObject json) {
        super(json.getLong("id"), json.getString("domain"));
        this.uniqueKey = json.getString("code");
        this.subject = json.getString("subject");
        this.password = json.getString("password");
        this.summary = json.getString("summary");
        this.founder = new Contact(json.getJSONObject("founder"));
        this.founderId = this.founder.getId();
        this.creation = json.getLong("creation");
        this.scheduleTime = json.getLong("scheduleTime");
        this.expireTime = json.getLong("expireTime");
//        this.participantGroup = new Group(json.getJSONObject("participantGroup"));
//        this.participantGroupId = this.participantGroup.getId();
//        if (json.has("commField")) {
//            this.commField = new CommField(json.getJSONObject("commField"));
//        }
    }

    public String getCode() {
        return this.uniqueKey;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getSubject() {
        return this.subject;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSummary() {
        return this.summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public Contact getFounder() {
        return this.founder;
    }

    public void setFounder(Contact founder) {
        this.founder = founder;
    }

    public Long getFounderId() {
        return this.founderId;
    }

    public long getCreation() {
        return this.creation;
    }

    public long getScheduleTime() {
        return this.scheduleTime;
    }

    public void setScheduleTime(long time) {
        this.scheduleTime = time;
    }

    public long getExpireTime() {
        return this.expireTime;
    }

    public void setExpireTime(long time) {
        this.expireTime = time;
    }

    public Room getRoom() {
        return this.room;
    }

    public boolean isCancelled() {
        return this.cancelled;
    }

    public void setCancelled(boolean value) {
        this.cancelled = value;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("id", this.id.longValue());
        json.put("domain", this.domain.getName());
        json.put("code", this.uniqueKey);
        json.put("subject", this.subject);
        json.put("password", this.password);
        json.put("summary", this.summary);
        json.put("founder", this.founder.toCompactJSON());
        json.put("creation", this.creation);
        json.put("scheduleTime", this.scheduleTime);
        json.put("expireTime", this.expireTime);
        json.put("room", this.room.toJSON());
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}