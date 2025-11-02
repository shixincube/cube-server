/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.scene;

import cube.aigc.psychology.EvaluationFeature;
import cube.aigc.psychology.EvaluationReport;
import cube.aigc.psychology.Painting;
import cube.aigc.psychology.Reference;
import cube.aigc.psychology.algorithm.PaintingConfidence;
import cube.aigc.psychology.composition.PaintingFeatureSet;

public class AttachmentStyleEvaluation extends Evaluation {

    private Reference reference;

    public AttachmentStyleEvaluation(long contactId, Painting painting) {
        super(contactId, painting);
    }

    @Override
    public EvaluationReport makeEvaluationReport() {
        EvaluationReport report = null;
        EvaluationFeature result = new EvaluationFeature();
        report = new EvaluationReport(this.contactId, this.painting.getAttribute(), this.reference,
                new PaintingConfidence(this.painting), result);
        return report;
    }

    @Override
    public PaintingFeatureSet getPaintingFeatureSet() {
        return null;
    }
}
