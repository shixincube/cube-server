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
import org.json.JSONObject;

/**
 * 会议房间。
 */
public class Room implements JSONable {

    /**
     * 最大允许参与人数。
     */
    protected int maxParticipants = 50;

    /**
     * 参与者群组。
     */
    protected Group participantGroup;

    /**
     * 参与者群组 ID 。
     */
    protected Long participantGroupId = 0L;

    /**
     * 通信场域。
     */
    protected CommField commField;

    /**
     * 通信场域 ID 。
     */
    protected Long commFieldId = 0L;


    /**
     * 构造函数。
     *
     * @param participantGroupId
     * @param commFieldId
     */
    public Room(Long participantGroupId, Long commFieldId) {
        this.participantGroupId = participantGroupId;
        this.commFieldId = commFieldId;
    }

    /**
     * 构造函数。
     *
     * @param json
     */
    public Room(JSONObject json) {
        this.maxParticipants = json.getInt("maxParticipants");
        this.participantGroupId = json.getLong("participantGroupId");
        this.commFieldId = json.getLong("commFieldId");
    }

    public int getMaxParticipants() {
        return this.maxParticipants;
    }

    public void setMaxParticipants(int max) {
        this.maxParticipants = max;
    }

    public Long getParticipantGroupId() {
        return this.participantGroupId;
    }

    public void setParticipantGroup(Group group) {
        this.participantGroup = group;
        this.participantGroupId = group.getId();
    }

    public Group getParticipantGroup() {
        return this.participantGroup;
    }

    public Long getCommFieldId() {
        return this.commFieldId;
    }

    public void setCommField(CommField commField) {
        this.commField = commField;
        this.commFieldId = commField.getId();
    }

    public CommField getCommField() {
        return this.commField;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("maxParticipants", this.maxParticipants);
        json.put("participantGroupId", this.participantGroupId.longValue());
        json.put("commFieldId", this.commFieldId.longValue());
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
