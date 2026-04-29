/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.service.aigc.scene.evaluation;

import cube.aigc.psychology.EvaluationFeature;
import cube.aigc.psychology.EvaluationReport;
import cube.aigc.psychology.Painting;
import cube.aigc.psychology.composition.PaintingFeatureSet;

import java.util.ArrayList;
import java.util.List;

public class KineticFamilyDrawingEvaluation extends Evaluation {

    public KineticFamilyDrawingEvaluation(long contactId, Painting painting) {
        super(contactId, painting);
    }

    @Override
    public EvaluationReport makeEvaluationReport() {
        List<EvaluationFeature> results = new ArrayList<>();
        return null;
    }

    @Override
    public PaintingFeatureSet getPaintingFeatureSet() {
        return null;
    }
}
