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
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 会议实体。
 */
public class Conference extends Entity {

    /**
     * 会议码。
     */
    private String code;

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
        this.code = Utils.randomNumberString(8);
        this.founder = founder;
        this.founderId = founder.getId();
        this.presenter = founder;
        this.presenterId = this.presenter.getId();
        this.creation = this.getTimestamp();
        this.invitees = new ArrayList<>();
        this.room = new Room(id, 0L);
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
     * @param presenterId
     * @param creation
     * @param scheduleTime
     * @param expireTime
     * @param participantGroupId
     * @param commFieldId
     */
    public Conference(Long id, String domainName, String code, String subject, String password, String summary,
        Long founderId, Long presenterId, long creation, long scheduleTime, long expireTime,
        Long participantGroupId, Long commFieldId) {
        super(id, domainName);
        this.code = code;
        this.subject = subject;
        this.password = password;
        this.summary = summary;
        this.founderId = founderId;
        this.presenterId = presenterId;
        this.creation = creation;
        this.scheduleTime = scheduleTime;
        this.expireTime = expireTime;
        this.invitees = new ArrayList<>();
        this.room = new Room(participantGroupId, commFieldId);
    }

    /**
     * 构造函数。
     *
     * @param json
     */
    public Conference(JSONObject json) {
        super(json.getLong("id"), json.getString("domain"));
        this.code = json.getString("code");
        this.subject = json.getString("subject");
        this.password = json.getString("password");
        this.summary = json.getString("summary");
        this.founder = new Contact(json.getJSONObject("founder"));
        this.founderId = this.founder.getId();
        this.presenter = new Contact(json.getJSONObject("presenter"));
        this.presenterId = this.presenter.getId();
        this.creation = json.getLong("creation");
        this.scheduleTime = json.getLong("scheduleTime");
        this.expireTime = json.getLong("expireTime");

        this.invitees = new ArrayList<>();
        JSONArray array = json.getJSONArray("invitees");
        for (int i = 0; i < array.length(); ++i) {
            JSONObject data = array.getJSONObject(i);
            Invitation invitation = new Invitation(data);
            this.invitees.add(invitation);
        }

        this.room = new Room(json.getJSONObject("room"));
    }

    public String getCode() {
        return this.code;
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

    public boolean hasPassword() {
        return (null != this.password && this.password.length() > 0);
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
        this.founderId = founder.getId();
    }

    public Long getFounderId() {
        return this.founderId;
    }

    public Contact getPresenter() {
        return this.presenter;
    }

    public void setPresenter(Contact contact) {
        this.presenter = contact;
        this.presenterId = contact.getId();
    }

    public Long getPresenterId() {
        return this.presenterId;
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

    public Invitation getInvitee(Long id) {
        for (Invitation invitation : this.invitees) {
            if (invitation.id.longValue() == id.longValue()) {
                return invitation;
            }
        }
        return null;
    }

    public List<Invitation> getInvitees() {
        return this.invitees;
    }

    public void setInvitees(List<Invitation> list) {
        this.invitees = list;
    }

    public void addInvitee(Invitation invitation) {
        this.invitees.add(invitation);
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
        json.put("code", this.code);
        json.put("subject", this.subject);
        json.put("password", this.password);
        json.put("summary", this.summary);
        json.put("founder", this.founder.toCompactJSON());
        json.put("presenter", this.presenter.toCompactJSON());
        json.put("creation", this.creation);
        json.put("scheduleTime", this.scheduleTime);
        json.put("expireTime", this.expireTime);

        JSONArray array = new JSONArray();
        for (Invitation invitation : this.invitees) {
            array.put(invitation.toJSON());
        }
        json.put("invitees", array);

        json.put("room", this.room.toJSON());

        json.put("cancelled", this.cancelled);

        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = this.toJSON();
        json.remove("password");
        json.put("existingPwd", this.hasPassword());
        return json;
    }
}
