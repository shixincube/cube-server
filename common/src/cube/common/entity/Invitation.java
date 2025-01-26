/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

import cube.common.JSONable;
import org.json.JSONObject;

/**
 * 会议邀请信息。
 */
public class Invitation implements JSONable {

    /**
     * 被邀请人 ID 。
     */
    protected Long id;

    /**
     * 被邀请人。
     */
    protected String invitee;

    /**
     * 被邀请人显示的名称。
     */
    protected String displayName;

    /**
     * 是否接受邀请。
     */
    protected boolean accepted = false;

    /**
     * 接受邀请时间。
     */
    protected long acceptionTime;

    /**
     * 构造函数。
     *
     * @param id
     * @param invitee
     * @param displayName
     */
    public Invitation(Long id, String invitee, String displayName, boolean accepted, long acceptionTime) {
        this.id = id;
        this.invitee = invitee;
        this.displayName = displayName;
        this.accepted = accepted;
        this.acceptionTime = acceptionTime;
    }

    public Invitation(JSONObject json) {
        this.id = json.getLong("id");
        this.invitee = json.getString("invitee");
        this.displayName = json.getString("displayName");
        this.accepted = json.getBoolean("accepted");
        this.acceptionTime = json.getLong("acceptionTime");
    }

    public Long getId() {
        return this.id;
    }

    public String getInvitee() {
        return this.invitee;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public boolean getAccepted() {
        return this.accepted;
    }

    public void setAccepted(boolean value) {
        this.accepted = value;
        this.acceptionTime = System.currentTimeMillis();
    }

    public long getAcceptionTime() {
        return this.acceptionTime;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("id", this.id.longValue());
        json.put("invitee", this.invitee);
        json.put("displayName", this.displayName);
        json.put("accepted", this.accepted);
        json.put("acceptionTime", this.acceptionTime);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
