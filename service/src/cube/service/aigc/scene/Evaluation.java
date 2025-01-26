/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.scene;

import cube.aigc.psychology.EvaluationReport;
import cube.aigc.psychology.Painting;

/**
 * 评估器。
 */
public abstract class Evaluation {

    protected Painting painting;

    public Evaluation(Painting painting) {
        this.painting = painting;
    }

    /**
     * 生成评估报告。
     *
     * @return
     */
    public abstract EvaluationReport makeEvaluationReport();
}
