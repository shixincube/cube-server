/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.scene;

import cube.aigc.psychology.*;
import cube.aigc.psychology.algorithm.PerceptronThing;
import cube.aigc.psychology.algorithm.Tendency;
import cube.aigc.psychology.composition.PaintingFeatureSet;
import cube.aigc.psychology.composition.SpaceLayout;
import cube.aigc.psychology.composition.Texture;
import cube.util.FloatUtils;

import java.util.ArrayList;
import java.util.List;

public class PersonInRainEvaluation extends Evaluation {

    private static Indicator[] sIndicators = new Indicator[] {
            Indicator.Stress,
            Indicator.Confidence,
            Indicator.Repression
    };

    private static final Texture sDenseRain = new Texture(5.1562, 2.7135, 1.3571, 1.1649,
            0.390625, 0.5);

    private static final Texture sSparseRain = new Texture(0.8125, 0.3512, 0.0556, 0.2358,
            0.0615, 0.1666);

    private static final Texture sBlankRain = new Texture(0.0208, 0.0013, 0.00002, 0.005,
            0.0058, 0.1666);

    public PersonInRainEvaluation(long contactId, Painting painting) {
        super(contactId, painting);
    }

    @Override
    public EvaluationReport makeEvaluationReport() {
        EvaluationReport report = null;
        List<EvaluationFeature> results = new ArrayList<>();

        SpaceLayout spaceLayout = new SpaceLayout(this.painting);
        results.add(this.evalSpaceStructure(spaceLayout));
        results.add(this.evalTracesDensity(spaceLayout));

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

                result.addScore(Indicator.Confidence, -1, FloatUtils.random(0.2, 0.3));
            }
        }

        return result;
    }

    private EvaluationFeature evalTracesDensity(SpaceLayout spaceLayout) {
        EvaluationFeature result = new EvaluationFeature();

        Texture texture = this.painting.getWhole();
        if (null != texture) {
            // 判断整体稠密度

        }

        List<Texture> quadrants = this.painting.getQuadrants();
        for (Texture quadrant : quadrants) {
            System.out.println("XJW t:\n" + quadrant.toJSON().toString(4));
        }

        return result;
    }
}
