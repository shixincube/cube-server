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

import cell.util.Utils;
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
     * 状态。
     */
    public ContactZoneState state;

    /**
     * 是否工作在对等模式下。
     * 在对等模式下，添加 Contact 时，会在对方的同名 Zone 下也添加自己的 Contact ，且状态为 Pending 。
     */
    public boolean peerMode = false;

    /**
     * 分区的联系人列表。
     */
    private final List<ContactZoneParticipant> participants;

    public ContactZone(Long id, String domain, long owner, String name, long timestamp,
                       String displayName, ContactZoneState state, boolean peerMode) {
        super(id, domain);
        this.setTimestamp(timestamp);
        this.owner = owner;
        this.name = name;
        this.state = state;
        this.displayName = displayName;
        this.peerMode = peerMode;
        this.participants = new ArrayList<>();
    }

    /**
     * 构造函数。
     *
     * @param name
     */
    public ContactZone(String name) {
        this(Utils.generateSerialNumber(), "", 0, name, System.currentTimeMillis(),
                name, ContactZoneState.Normal, false);
    }

    public ContactZone(JSONObject json) {
        super(json);
        this.owner = json.getLong("owner");
        this.name = json.getString("name");
        this.displayName = json.getString("displayName");
        this.state = ContactZoneState.parse(json.getInt("state"));
        this.peerMode = json.getBoolean("peerMode");
        this.participants = new ArrayList<>();

        if (json.has("participants")) {
            JSONArray array = json.getJSONArray("participants");
            for (int i = 0; i < array.length(); ++i) {
                JSONObject data = array.getJSONObject(i);
                ContactZoneParticipant participant = new ContactZoneParticipant(data);
                this.addParticipant(participant);
            }
        }
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

    public ContactZoneParticipant getParticipant(Long id) {
        for (ContactZoneParticipant participant : this.participants) {
            if (participant.id.equals(id)) {
                return participant;
            }
        }

        return null;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = ContactZone.this.toCompactJSON();

        JSONArray participants = new JSONArray();

        for (ContactZoneParticipant participant : this.participants) {
            participants.put(participant.toJSON());
        }

        json.put("participants", participants);

        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = super.toCompactJSON();
        json.put("owner", this.owner);
        json.put("name", this.name);
        json.put("displayName", this.displayName);
        json.put("state", this.state.code);
        json.put("peerMode", this.peerMode);
        return json;
    }
}
