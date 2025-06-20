/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.contact;

import cube.common.entity.Contact;
import cube.common.entity.Membership;
import org.json.JSONObject;

public class MembershipSystem {

    private ContactStorage storage;

    public MembershipSystem(ContactStorage storage) {
        this.storage = storage;
    }

    public Membership getMembership(Contact contact, int state) {
        return this.storage.readMembership(contact.getDomain().getName(), contact.getId(), state);
    }

    public Membership getMembership(String domain, long contactId, int state) {
        return this.storage.readMembership(domain, contactId, state);
    }

    public boolean activateMembership(String domain, long contactId, String name, long duration, String description,
                                      JSONObject context) {
        Membership membership = new Membership(contactId, domain, name,
                Membership.TYPE_ORDINARY, Membership.STATE_NORMAL, System.currentTimeMillis(),
                duration, description, context);
        return this.storage.writeMembership(membership);
    }

    public boolean cancelMembership(String domain, long contactId) {
        Membership membership = this.storage.readMembership(domain, contactId, Membership.STATE_NORMAL);
        if (null == membership) {
            return false;
        }

        membership.state = Membership.STATE_INVALID;
        return this.storage.writeMembership(membership);
    }

    public boolean updateMembership(Membership membership) {
        return this.storage.writeMembership(membership);
    }
}
