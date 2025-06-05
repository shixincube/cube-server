/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology.app;

import cube.auth.AuthToken;
import cube.common.entity.Point;

public class PointTable {

    public final static String Evaluation = "Evaluation";
    public final static int EvaluationPoints = 100;

    public PointTable() {
    }

    public static Point createEvaluationPoint(AuthToken authToken) {
        return new Point(authToken.getDomain(), System.currentTimeMillis(),
                authToken.getContactId(), EvaluationPoints, Evaluation, "Psychology");
    }
}
