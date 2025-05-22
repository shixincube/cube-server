/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology.app;

import cube.common.entity.Contact;
import cube.common.entity.Point;

public class PointTable {

    public final static String Evaluation = "Evaluation";

    public PointTable() {
    }

    public static Point createEvaluationPoint(Contact contact) {
        return new Point(contact.getDomain().getName(), System.currentTimeMillis(),
                contact.getId(), -200, Evaluation, "Psychology");
    }
}
