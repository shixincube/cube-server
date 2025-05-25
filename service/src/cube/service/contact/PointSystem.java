/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.contact;

import cell.util.log.Logger;
import cube.auth.AuthToken;
import cube.common.entity.Contact;
import cube.common.entity.Point;

import java.util.List;

public class PointSystem {

    private ContactStorage storage;

    public PointSystem(ContactStorage storage) {
        this.storage = storage;
    }

    public int total(Contact contact) {
        return this.storage.totalPoints(contact.getId(), contact.getDomain().getName());
    }

    public int total(AuthToken authToken) {
        return this.storage.totalPoints(authToken.getContactId(), authToken.getDomain());
    }

    public List<Point> listPoints(Contact contact) {
        return this.storage.readPoints(contact.getId(), contact.getDomain().getName());
    }

    public int insert(Point point) {
        if (!this.storage.writePoint(point)) {
            Logger.e(this.getClass(), "#insert - write data to database error: " + point.getContactId());
        }

        return this.storage.totalPoints(point.getContactId(), point.getDomain().getName());
    }

    public int increment(Contact contact, int value, String source, String comment) {
        Point point = new Point(contact.getDomain().getName(), System.currentTimeMillis(),
                contact.getId(), value, source, comment);
        if (!this.storage.writePoint(point)) {
            Logger.e(this.getClass(), "#increment - write data to database error: " + contact.getId());
        }

        return this.storage.totalPoints(contact.getId(), contact.getDomain().getName());
    }

    public int decrement(Contact contact, int value, String source, String comment) {
        Point point = new Point(contact.getDomain().getName(), System.currentTimeMillis(),
                contact.getId(), -value, source, comment);
        if (!this.storage.writePoint(point)) {
            Logger.e(this.getClass(), "#decrement - write data to database error: " + contact.getId());
        }

        return this.storage.totalPoints(contact.getId(), contact.getDomain().getName());
    }
}
