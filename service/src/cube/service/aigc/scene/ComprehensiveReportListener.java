/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.service.aigc.scene;

import cube.aigc.psychology.ComprehensiveReport;
import cube.common.entity.FileLabel;

public interface ComprehensiveReportListener {

    void onPredicting(ComprehensiveReport report, FileLabel file);

    void onEvaluating(ComprehensiveReport report);

    void onEvaluateCompleted(ComprehensiveReport report);

    void onEvaluateFailed(ComprehensiveReport report);
}
