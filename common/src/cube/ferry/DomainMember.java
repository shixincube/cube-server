/*
 * This source file is part of Cube.
 * <p>
 * The MIT License (MIT)
 * <p>
 * Copyright (c) 2020-2024 Ambrose Xu.
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
