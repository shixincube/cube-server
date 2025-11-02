/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.scene;

import cube.aigc.psychology.EvaluationReport;
import cube.aigc.psychology.Painting;
import cube.aigc.psychology.composition.PaintingFeatureSet;

/**
 * 评估器。
 */
public abstract class Evaluation {

    protected long contactId;

    protected Painting painting;

    public Evaluation(long contactId, Painting painting) {
        this.contactId = contactId;
        this.painting = painting;
    }

    /**
     * 生成评估报告。
     *
     * @return
     */
    public abstract EvaluationReport makeEvaluationReport();

    /**
     * 获取会话特征数据集。
     *
     * @return
     */
    public abstract PaintingFeatureSet getPaintingFeatureSet();
}
