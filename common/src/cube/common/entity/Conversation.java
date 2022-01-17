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

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 会话。
 */
public class Conversation extends Entity {

    /**
     * 所有人 ID 。
     */
    private Long ownerId;

    /**
     * 会话类型。
     */
    private ConversationType type;

    /**
     * 会话状态。
     */
    private ConversationState state;

    /**
     * 会话提醒类型。
     */
    private ConversationRemindType remindType;

    /**
     * 与会话相关的关键实体的 ID 。
     */
    private Long pivotalId;

    /**
     * 最近的一条消息。
     */
    private Message recentMessage;

    /**
     * 未读消息记录数。
     */
    private int unreadCount;

    /**
     * 头像名称。
     */
    private String avatarName;

    /**
     * 头像的 URL 。
     */
    private String avatarURL;

    /**
     * 构造函数。
     *
     * @param id
     * @param domain
     * @param timestamp
     * @param ownerId
     * @param type
     * @param state
     * @param pivotalId
     * @param remindType
     */
    public Conversation(Long id, String domain, long timestamp, Long ownerId, ConversationType type,
                        ConversationState state, Long pivotalId, ConversationRemindType remindType) {
        super(id, domain, timestamp);
        this.ownerId = ownerId;
        this.type = type;
        this.state = state;
        this.pivotalId = pivotalId;
        this.remindType = remindType;
        this.unreadCount = 0;
    }

    /**
     * 构造函数。
     *
     * @param json
     * @throws JSONException
     */
    public Conversation(JSONObject json, Contact owner) throws JSONException {
        super(json);
        this.ownerId = owner.getId();
        this.pivotalId = json.getLong("pivotal");
        this.type = ConversationType.parse(json.getInt("type"));
        this.state = ConversationState.parse(json.getInt("state"));
        this.remindType = ConversationRemindType.parse(json.getInt("reminding"));

        this.unreadCount = json.has("unread") ? json.getInt("unread") : 0;

        if (json.has("recentMessage")) {
            this.recentMessage = new Message(json.getJSONObject("recentMessage"));
        }

        if (json.has("avatarName")) {
            this.avatarName = json.getString("avatarName");
        }

        if (json.has("avatarURL")) {
            this.avatarURL = json.getString("avatarURL");
        }
    }

    public Long getOwnerId() {
        return this.ownerId;
    }

    public ConversationType getType() {
        return this.type;
    }

    public Long getPivotalId() {
        return this.pivotalId;
    }

    public ConversationState getState() {
        return this.state;
    }

    public ConversationRemindType getRemindType() {
        return this.remindType;
    }

    public Message getRecentMessage() {
        return this.recentMessage;
    }

    public void setRecentMessage(Message message) {
        this.recentMessage = message;
    }

    public void setUnreadCount(int count) {
        this.unreadCount = count;
    }

    public void setAvatarName(String avatarName) {
        this.avatarName = avatarName;
    }

    public void setAvatarURL(String avatarURL) {
        this.avatarURL = avatarURL;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("owner", this.ownerId.longValue());
        json.put("type", this.type.code);
        json.put("state", this.state.code);
        json.put("reminding", this.remindType.code);
        json.put("pivotal", this.pivotalId.longValue());
        json.put("unread", this.unreadCount);

        if (null != this.recentMessage) {
            json.put("recentMessage", this.recentMessage.toJSON());
        }

        if (null != this.avatarName) {
            json.put("avatarName", this.avatarName);
        }
        if (null != this.avatarURL) {
            json.put("avatarURL", this.avatarURL);
        }

        return json;
    }
}
