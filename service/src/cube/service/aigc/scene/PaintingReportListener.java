/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.scene;

import cube.aigc.psychology.Painting;
import cube.aigc.psychology.PaintingReport;
import cube.common.entity.FileLabel;

/**
 * 绘画报告事件监听器。
 */
public interface PaintingReportListener {

    void onPaintingPredicting(PaintingReport report, FileLabel file);

    void onPaintingPredictCompleted(PaintingReport report, FileLabel file, Painting painting);

    void onPaintingPredictFailed(PaintingReport report);

    void onReportEvaluating(PaintingReport report);

    void onReportEvaluateCompleted(PaintingReport report);

    void onReportEvaluateFailed(PaintingReport report);
}
