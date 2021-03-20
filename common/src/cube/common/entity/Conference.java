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

/**
 * 会议实体。
 */
public class Conference extends Entity {

    /**
     * 会议号。
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
     * 参与者群组。
     */
    private Group participantGroup;

    /**
     * 参与者群组 ID 。
     */
    private Long participantGroupId;

    /**
     * 最大允许参与人数。
     */
    private int maxParticipants = 50;

    /**
     * 通信场域。
     */
    private CommField commField;

    /**
     * 通信场域 ID 。
     */
    private Long commFieldId;

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
    public Conference(Long id, String domainName) {
        super(id, domainName);
        this.code = Utils.randomNumberString(8);
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
        this.code = code;
        this.subject = subject;
        this.password = password;
        this.summary = summary;
        this.founderId = founderId;
        this.creation = creation;
        this.scheduleTime = scheduleTime;
        this.expireTime = expireTime;
        this.participantGroupId = participantGroupId;
        this.commFieldId = commFieldId;
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

    public String getSummary() {
        return this.summary;
    }

    public Contact getFounder() {
        return this.founder;
    }

    public long getCreation() {
        return this.creation;
    }

    public long getScheduleTime() {
        return this.scheduleTime;
    }

    public long getExpireTime() {
        return this.expireTime;
    }

    public int getMaxParticipants() {
        return this.maxParticipants;
    }

    public Group getParticipantGroup() {
        return this.participantGroup;
    }

    public void setParticipantGroup(Group group) {
        this.participantGroup = group;
    }

    public CommField getCommField() {
        return this.commField;
    }

    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
