/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.multipointcomm.signaling;

import cube.common.action.MultipointCommAction;
import cube.common.entity.CommField;
import cube.common.entity.Contact;
import cube.common.entity.Device;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 邀请信令。
 */
public class InviteSignaling extends Signaling {

    /**
     * 被邀请进行通话的联系人列表。
     */
    private List<Long> invitees;

    private Contact invitee;

    public InviteSignaling(CommField commField, Contact contact, Device device) {
        super(MultipointCommAction.Invite.name, commField, contact, device);
    }

    public InviteSignaling(JSONObject json) {
        super(json);

        if (json.has("invitees")) {
            this.invitees = new ArrayList<>();

            JSONArray array = json.getJSONArray("invitees");
            for (int i = 0; i < array.length(); ++i) {
                this.invitees.add(array.getLong(i));
            }
        }

        if (json.has("invitee")) {
            this.invitee = new Contact(json.getJSONObject("invitee"));
        }
    }

    /**
     * 被邀请列表。
     *
     * @return
     */
    public List<Long> getInvitees() {
        return this.invitees;
    }

    public void setInvitee(Contact invitee) {
        this.invitee = invitee;
    }

    public Contact getInvitee() {
        return this.invitee;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();

        if (null != this.invitees) {
            JSONArray array = new JSONArray();
            for (Long contactId : this.invitees) {
                array.put(contactId.longValue());
            }
            json.put("invitees", array);
        }

        if (null != this.invitee) {
            json.put("invitee", this.invitee.toBasicJSON());
        }

        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
