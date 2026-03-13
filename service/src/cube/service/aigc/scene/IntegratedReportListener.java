/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.service.aigc.scene;

import cube.aigc.psychology.IntegratedReport;
import cube.common.entity.FileLabel;

public interface IntegratedReportListener {

    void onPredicting(IntegratedReport report, FileLabel file);

    void onEvaluating(IntegratedReport report);

    void onEvaluateCompleted(IntegratedReport report);

    void onEvaluateFailed(IntegratedReport report);
}
