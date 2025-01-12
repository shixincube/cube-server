/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2020-2025 Ambrose Xu.
 */

package cube.service.cv;

import cell.core.talk.TalkContext;
import cube.common.entity.Contact;

public class CVEndpoint {

    public final Contact contact;

    public final TalkContext talkContext;

    public CVEndpoint(Contact contact, TalkContext talkContext) {
        this.contact = contact;
        this.talkContext = talkContext;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CVEndpoint) {
            CVEndpoint other = (CVEndpoint) obj;
            if (other.contact.getId().longValue() == this.contact.getId().longValue()) {
                return true;
            }
        }

        return false;
    }

    @Override
    public int hashCode() {
        return this.contact.getId().hashCode();
    }
}
