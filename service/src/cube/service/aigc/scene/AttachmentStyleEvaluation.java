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
import cube.util.calc.FrameStructure;
import cube.aigc.psychology.composition.PaintingFeatureSet;
import cube.aigc.psychology.composition.SpaceLayout;
import cube.util.FloatUtils;
import cube.util.calc.FrameStructureCalculator;
import cube.util.calc.FrameStructureDescription;
import cube.vision.BoundingBox;
import cube.vision.Size;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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

        SpaceLayout spaceLayout = new SpaceLayout(this.painting);
        results.add(this.evalSpaceStructure(spaceLayout));

        report = new EvaluationReport(this.contactId, this.painting.getAttribute(), this.reference,
                new PaintingConfidence(this.painting), results);
        return report;
    }

    @Override
    public PaintingFeatureSet getPaintingFeatureSet() {
        return null;
    }

    private EvaluationFeature evalSpaceStructure(SpaceLayout spaceLayout) {
        EvaluationFeature result = new EvaluationFeature();

        // 画面大小比例
        double areaRatio = spaceLayout.getAreaRatio();

        if (areaRatio > 0) {
            if (areaRatio <= 0.09) {
                String desc = "画面的画幅相对画布面积非常小";
                result.addFeature(desc, Term.SenseOfSecurity, Tendency.Negative, PerceptronThing.createPictureSize());

                result.addScore(Indicator.AnxiousAttachment, 1, FloatUtils.random(0.2, 0.3));
            }
        }

        FrameStructureCalculator calculator = new FrameStructureCalculator();
        FrameStructureDescription description = calculator.calcFrameStructure(this.painting.getCanvasSize(),
                spaceLayout.getPaintingBox());

        if (description.isNotInCorner()) {
            result.addScore(Indicator.SecureAttachment, 1, FloatUtils.random(0.2, 0.3));
        }
        else {
            result.addScore(Indicator.AvoidantAttachment, 1, FloatUtils.random(0.3, 0.4));
        }

        return result;
    }

    private EvaluationFeature evalHouse(SpaceLayout spaceLayout) {
        EvaluationFeature result = new EvaluationFeature();
        return result;
    }

    private EvaluationFeature evalPerson(SpaceLayout spaceLayout) {
        EvaluationFeature result = new EvaluationFeature();

        return result;
    }
}
