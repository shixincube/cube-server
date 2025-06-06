/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.contact;

import cube.common.entity.Contact;
import cube.common.entity.Membership;

public class MembershipSystem {

    private ContactStorage storage;

    public MembershipSystem(ContactStorage storage) {
        this.storage = storage;
    }

    public Membership getMembership(Contact contact) {
        return this.storage.readMembership(contact.getDomain().getName(), contact.getId());
    }

    public Membership getMembership(String domain, long contactId) {
        return this.storage.readMembership(domain, contactId);
    }

    public boolean setMembership(Membership membership) {
        return this.storage.writeMembership(membership);
    }


}
