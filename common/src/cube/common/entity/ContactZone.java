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

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 联系人分区。
 */
public class ContactZone extends Entity {

    /**
     * 所属的联系人的 ID 。
     */
    public final long owner;

    /**
     * 分区名称。
     */
    public final String name;

    /**
     * 显示名。
     */
    public String displayName = "";

    /**
     * 是否需要验证。
     */
    public boolean needsVerify = false;

    /**
     * 状态。
     */
    public ContactZoneState state;

    /**
     * 分区的联系人列表。
     */
    private final List<ContactZoneParticipant> participants;

    public ContactZone(Long id, String domain, long owner, String name, long timestamp, ContactZoneState state) {
        super(id, domain);
        this.setTimestamp(timestamp);
        this.owner = owner;
        this.name = name;
        this.state = state;
        this.participants = new ArrayList<>();
    }

    public void addParticipant(ContactZoneParticipant participant) {
        if (participant.type == ContactZoneParticipantType.Contact
                && participant.id.longValue() == this.owner) {
            // 过滤分区所有人
            return;
        }

        if (!this.participants.contains(participant)) {
            this.participants.add(participant);
        }
    }

    public void removeParticipant(Long id) {
        for (ContactZoneParticipant participant : this.participants) {
            if (participant.id.equals(id)) {
                this.participants.remove(participant);
                break;
            }
        }
    }

    public List<ContactZoneParticipant> getParticipants() {
        return this.participants;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = this.toCompactJSON();

        JSONArray participants = new JSONArray();

        for (ContactZoneParticipant participant : this.participants) {
            participants.put(participant.toJSON());
        }

        json.put("participants", participants);

        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = new JSONObject();
        json.put("id", this.id.longValue());
        json.put("domain", this.domain.getName());
        json.put("owner", this.owner);
        json.put("name", this.name);
        json.put("displayName", this.displayName);
        json.put("timestamp", this.getTimestamp());
        json.put("state", this.state.code);
        json.put("needsVerify", this.needsVerify);
        return json;
    }
}
