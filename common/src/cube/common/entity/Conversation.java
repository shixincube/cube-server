/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 Cube Team.
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
     * 最近的消息列表。
     */
    private List<Message> recentMessages;

    /**
     * 未读消息记录数。
     */
    private int unreadCount;

    /**
     * 上下文数据。
     */
    private JSONObject context;

    /**
     * 头像名称。
     */
    private String avatarName;

    /**
     * 头像的 URL 。
     */
    private String avatarURL;

    /**
     * 关键实体。
     */
    private AbstractContact pivotalEntity;

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
     * @param contact
     */
    public Conversation(Contact contact) {
        this(contact, new ArrayList<>());
    }

    /**
     * 构造函数。
     *
     * @param group
     */
    public Conversation(Group group) {
        this(group, new ArrayList<>());
    }

    /**
     * 构造函数。
     *
     * @param contact
     * @param recentMessages
     */
    public Conversation(Contact contact, List<Message> recentMessages) {
        super(Utils.generateSerialNumber(), "", System.currentTimeMillis());
        this.pivotalEntity = contact;
        this.ownerId = 0L;
        this.type = ConversationType.Contact;
        this.state = ConversationState.Normal;
        this.pivotalId = 0L;
        this.remindType = ConversationRemindType.Normal;
        this.unreadCount = 0;
        this.recentMessages = recentMessages;
    }

    /**
     * 构造函数。
     *
     * @param group
     * @param recentMessages
     */
    public Conversation(Group group, List<Message> recentMessages) {
        super(Utils.generateSerialNumber(), "", System.currentTimeMillis());
        this.pivotalEntity = group;
        this.ownerId = 0L;
        this.type = ConversationType.Group;
        this.state = ConversationState.Normal;
        this.pivotalId = 0L;
        this.remindType = ConversationRemindType.Normal;
        this.unreadCount = 0;
        this.recentMessages = recentMessages;
    }

    /**
     * 构造函数。
     *
     * @param json
     */
    public Conversation(JSONObject json) {
        this(json, null);
    }

    /**
     * 构造函数。
     *
     * @param json
     * @param owner
     */
    public Conversation(JSONObject json, Contact owner) {
        super(json);
        this.ownerId = (null != owner) ? owner.getId() : 0L;
        this.pivotalId = json.getLong("pivotal");
        this.type = ConversationType.parse(json.getInt("type"));
        this.state = ConversationState.parse(json.getInt("state"));
        this.remindType = ConversationRemindType.parse(json.getInt("reminding"));

        this.unreadCount = json.has("unread") ? json.getInt("unread") : 0;

        if (json.has("context")) {
            this.context = json.getJSONObject("context");
        }

        if (json.has("pivotalEntity")) {
            if (ConversationType.Contact == this.type) {
                this.pivotalEntity = new Contact(json.getJSONObject("pivotalEntity"));
            }
            else if (ConversationType.Group == this.type) {
                this.pivotalEntity = new Group(json.getJSONObject("pivotalEntity"));
            }
        }

        if (json.has("recentMessage")) {
            this.recentMessages = new ArrayList<>();
            this.recentMessages.add(new Message(json.getJSONObject("recentMessage")));
        }
        else if (json.has("recentMessages")) {
            this.recentMessages = new ArrayList<>();
            JSONArray array = json.getJSONArray("recentMessages");
            for (int i = 0; i < array.length(); ++i) {
                Message message = new Message(array.getJSONObject(i));
                this.recentMessages.add(message);
            }
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

    public JSONObject getContext() {
        return this.context;
    }

    public void setContext(JSONObject context) {
        this.context = context;
    }

    public AbstractContact getPivotalEntity() {
        return this.pivotalEntity;
    }

    public void setPivotalEntity(AbstractContact pivotalEntity) {
        this.pivotalEntity = pivotalEntity;
    }

    public List<Message> getRecentMessages() {
        return this.recentMessages;
    }

    public void addRecentMessage(Message message) {
        if (null == this.recentMessages) {
            this.recentMessages = new ArrayList<>();
        }

        this.recentMessages.add(message);
    }

    public void removeRecentMessage(Message message) {
        if (null == this.recentMessages) {
            return;
        }

        this.recentMessages.remove(message);
    }

    public Message getRecentMessage() {
        if (null == this.recentMessages) {
            return null;
        }

        return this.recentMessages.get(this.recentMessages.size() - 1);
    }

    public void setRecentMessage(Message message) {
        if (null != this.recentMessages) {
            this.recentMessages.clear();
        }
        else {
            this.recentMessages = new ArrayList<>();
        }

        this.recentMessages.add(message);
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

        if (null != this.context) {
            json.put("context", this.context);
        }

        if (null != this.pivotalEntity) {
            json.put("pivotalEntity", this.pivotalEntity.toCompactJSON());
        }

        if (null != this.recentMessages) {
            if (this.recentMessages.size() == 1) {
                json.put("recentMessage", this.recentMessages.get(0).toJSON());
            }
            else {
                JSONArray array = new JSONArray();
                for (Message message : this.recentMessages) {
                    array.put(message.toJSON());
                }
                json.put("recentMessages", array);
            }
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
