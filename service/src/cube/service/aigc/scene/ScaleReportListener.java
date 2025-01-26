/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.scene;

import cube.aigc.psychology.ScaleReport;

/**
 * 量表报告事件监听器。
 */
public interface ScaleReportListener {

    void onReportEvaluating(ScaleReport report);

    void onReportEvaluateCompleted(ScaleReport report);

    void onReportEvaluateFailed(ScaleReport report);
}
