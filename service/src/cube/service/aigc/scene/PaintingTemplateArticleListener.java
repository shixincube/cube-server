/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.service.aigc.scene;

import cube.aigc.psychology.PaintingReport;
import cube.aigc.psychology.composition.ReportArticle;
import cube.common.entity.AIGCChannel;
import cube.common.entity.FileLabel;

/**
 * 绘画测试模板内容监听器。
 */
public interface PaintingTemplateArticleListener {

    void onPaintingPredicted(PaintingReport report);

    void onCompleted(PaintingReport report, ReportArticle article);

    void onFailed(AIGCChannel channel, FileLabel fileLabel);
}
