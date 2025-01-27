/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.ferry;

import cube.common.entity.Entity;
import org.json.JSONObject;

/**
 * 域成员。
 */
public class DomainMember extends Entity {
    
    public final static int NORMAL = 0;

    public final static int QUIT = 1;

    public final static int DISABLED = 2;

    private Long contactId;

    private JoinWay joinWay;

    private long joinTime;

    private Role role;

    private int state;

    public DomainMember(String domainName, Long contactId, JoinWay joinWay, long joinTime, Role role, int state) {
        super(contactId, domainName);
        this.contactId = contactId;
        this.joinWay = joinWay;
        this.joinTime = joinTime;
        this.role = role;
        this.state = state;
    }

    public DomainMember(JSONObject json) {
        super(json);
        this.contactId = json.getLong("contactId");
        this.joinWay = JoinWay.parse(json.getInt("joinWay"));
        this.joinTime = json.getLong("joinTime");
        this.role = Role.parse(json.getInt("role"));
        this.state = json.getInt("state");
    }

    public Long getContactId() {
        return this.contactId;
    }

    public long getJoinTime() {
        return this.joinTime;
    }

    public void setJoinWay(JoinWay joinWay) {
        this.joinWay = joinWay;
    }

    public JoinWay getJoinWay() {
        return this.joinWay;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Role getRole() {
        return this.role;
    }

    public void setState(int state) {
        this.state = state;
    }

    public int getState() {
        return this.state;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("contactId", this.contactId.longValue());
        json.put("joinWay", this.joinWay.code);
        json.put("joinTime", this.joinTime);
        json.put("role", this.role.code);
        json.put("state", this.state);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
