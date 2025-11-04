/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.scene;

import cube.aigc.psychology.*;
import cube.aigc.psychology.algorithm.PaintingConfidence;
import cube.aigc.psychology.algorithm.PerceptronThing;
import cube.aigc.psychology.algorithm.Tendency;
import cube.aigc.psychology.composition.PaintingFeatureSet;
import cube.aigc.psychology.composition.SpaceLayout;
import cube.util.FloatUtils;

import java.util.ArrayList;
import java.util.List;

public class AttachmentStyleEvaluation extends Evaluation {

    private Reference reference = Reference.Normal;

    public AttachmentStyleEvaluation(long contactId, Painting painting) {
        super(contactId, painting);
    }

    @Override
    public EvaluationReport makeEvaluationReport() {
        EvaluationReport report = null;
        List<EvaluationFeature> results = new ArrayList<>();
        results.add(this.evalSpaceStructure());
        report = new EvaluationReport(this.contactId, this.painting.getAttribute(), this.reference,
                new PaintingConfidence(this.painting), results);
        return report;
    }

    @Override
    public PaintingFeatureSet getPaintingFeatureSet() {
        return null;
    }

    private EvaluationFeature evalSpaceStructure() {
        EvaluationFeature result = new EvaluationFeature();

        SpaceLayout spaceLayout = new SpaceLayout(this.painting);

        // 画面大小比例
        double areaRatio = spaceLayout.getAreaRatio();

        if (areaRatio > 0) {
            if (areaRatio <= 0.09) {
                String desc = "画面的画幅相对画布面积非常小";
                result.addFeature(desc, Term.SelfConfidence, Tendency.Negative, PerceptronThing.createPictureSize());
                result.addFeature(desc, Term.SelfExistence, Tendency.Negative, PerceptronThing.createPictureSize());
                result.addFeature(desc, Term.SenseOfSecurity, Tendency.Negative, PerceptronThing.createPictureSize());

                result.addScore(Indicator.Psychosis, 1, FloatUtils.random(0.2, 0.3));
                result.addScore(Indicator.Confidence, -1, FloatUtils.random(0.5, 0.6));
                result.addScore(Indicator.Depression, 1, FloatUtils.random(0.6, 0.7));
            }
        }

        return result;
    }
}
