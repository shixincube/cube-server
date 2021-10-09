/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 Shixin Cube Team.
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
