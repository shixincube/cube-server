/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

import cell.util.Utils;
import cube.common.JSONable;
import org.json.JSONObject;

/**
 * 联系人分区参与人。
 */
public class ContactZoneParticipant implements JSONable {

    public final Long id;

    public long timestamp;

    public final ContactZoneParticipantType type;

    public final Long inviterId;

    public final String postscript;

    public ContactZoneParticipantState state;

    public AbstractContact linkedContact;

    public ContactZoneParticipant(JSONObject json) {
        this(json.getLong("id"), ContactZoneParticipantType.parse(json.getInt("type")),
                json.getLong("timestamp"),
                json.getLong("inviterId"),
                json.has("postscript") ? json.getString("postscript") : null,
                ContactZoneParticipantState.parse(json.getInt("state")));

        if (json.has("linkedContact")) {
            if (this.type == ContactZoneParticipantType.Contact) {
                this.linkedContact = new Contact(json.getJSONObject("linkedContact"));
            }
            else if (this.type == ContactZoneParticipantType.Group) {
                this.linkedContact = new Group(json.getJSONObject("linkedContact"));
            }
        }
    }

    public ContactZoneParticipant(Long id, ContactZoneParticipantType type, long timestamp,
                                  Long inviterId, String postscript, ContactZoneParticipantState state) {
        this.id = id;
        this.type = type;
        this.timestamp = timestamp;
        this.inviterId = inviterId;
        this.postscript = (null != postscript) ? postscript : "";
        this.state = state;
    }

    /**
     * 构造函数。
     *
     * @param linkedContact
     */
    public ContactZoneParticipant(AbstractContact linkedContact) {
        this(Utils.generateSerialNumber(),
                linkedContact instanceof Contact ?
                        ContactZoneParticipantType.Contact : ContactZoneParticipantType.Group,
                System.currentTimeMillis(),
                0L,
                "",
                ContactZoneParticipantState.Normal);
        this.linkedContact = linkedContact;
    }

    /**
     * 设置连接的联系人实例。
     *
     * @param linkedContact
     */
    public void setLinkedContact(AbstractContact linkedContact) {
        this.linkedContact = linkedContact;
    }

    public AbstractContact getLinkedContact() {
        return this.linkedContact;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (null != object && object instanceof ContactZoneParticipant) {
            ContactZoneParticipant other = (ContactZoneParticipant) object;
            if (other.id.equals(this.id) && other.type.code == this.type.code) {
                return true;
            }
        }

        return false;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = this.toCompactJSON();
        json.put("postscript", this.postscript);

        if (null != this.linkedContact) {
            json.put("linkedContact", this.linkedContact.toJSON());
        }

        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = new JSONObject();
        json.put("id", this.id.longValue());
        json.put("type", this.type.code);
        json.put("timestamp", this.timestamp);
        json.put("state", this.state.code);
        json.put("inviterId", this.inviterId.longValue());
        return json;
    }
}
