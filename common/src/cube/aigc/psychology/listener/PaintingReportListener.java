/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology.listener;

import cube.aigc.psychology.Painting;
import cube.aigc.psychology.PaintingReport;
import cube.common.entity.AIGCUnit;
import cube.common.entity.FileLabel;

/**
 * 绘画报告事件监听器。
 *
 * <p>位于 common 而非 service：报告生成由心理学业务模块执行，而宿主门面
 * 需要以本接口接收回调，属宿主与模块共享的契约。</p>
 */
public interface PaintingReportListener {

    void onPaintingPredicting(PaintingReport report, FileLabel file);

    void onPaintingPredictCompleted(PaintingReport report, FileLabel file, Painting painting);

    void onPaintingPredictFailed(PaintingReport report);

    void onReportEvaluating(PaintingReport report);

    void onReportEvaluateCompleted(PaintingReport report, AIGCUnit unit);

    void onReportEvaluateFailed(PaintingReport report);
}
