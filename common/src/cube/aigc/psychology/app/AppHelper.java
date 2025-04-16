/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology.app;

import cube.common.entity.Contact;
import cube.common.entity.Point;
import cube.i10n.I10n;

public final class AppHelper {

    private AppHelper() {
    }

    public static Point createNewUserPoint(Contact contact, int value) {
        return new Point(contact.getDomain().getName(), System.currentTimeMillis(),
                contact.getId(), value, "NewUser", I10n.getApp(I10n.CN, "PointCommentNewUser"));
    }
}
