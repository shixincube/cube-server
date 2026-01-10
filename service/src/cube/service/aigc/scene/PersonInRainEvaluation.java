/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.scene;

import cell.util.log.Logger;
import cube.aigc.psychology.*;
import cube.aigc.psychology.algorithm.PaintingConfidence;
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

    public enum RainIntensity {
        /**
         * 雨势大。
         */
        Dense,

        /**
         * 雨势小。
         */
        Sparse,

        /**
         * 雨势一般。
         */
        Normal
    }

    private static final double sDenseThresholdStandardDeviation = 0.5;
    private static final double sDenseThresholdHierarchy = 0.3;
    private static final Texture sDenseRain = new Texture(5.1562, 2.7135, 1.3571, 1.1649,
            0.390625, 0.5);

    private static final double sSparseThresholdStandardDeviation = 0.22;
    private static final double sSparseThresholdHierarchy = 0.06;
    private static final Texture sSparseRain = new Texture(0.8125, 0.3512, 0.0556, 0.2358,
            0.0615, 0.1666);

    private static final Texture sBlankRain = new Texture(0.0208, 0.0013, 0.00002, 0.005,
            0.0058, 0.1666);

    private RainIntensity rainIntensity;

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
        results.add(this.evalPerson(spaceLayout));

        report = new EvaluationReport(this.contactId, this.painting.getAttribute(), Reference.Normal,
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

        // 雨势判断
        this.rainIntensity = RainIntensity.Normal;
        RainIntensity areaRainIntensity1;
        RainIntensity areaRainIntensity2;

        Texture quadrant1 = quadrants.get(0);
        if (Math.abs(quadrant1.standardDeviation - sDenseRain.standardDeviation) < sDenseThresholdStandardDeviation
                && Math.abs(quadrant1.hierarchy - sDenseRain.hierarchy) < sDenseThresholdHierarchy) {
            areaRainIntensity1 = RainIntensity.Dense;
        }
        else if (Math.abs(quadrant1.standardDeviation - sSparseRain.standardDeviation) < sSparseThresholdStandardDeviation
                && Math.abs(quadrant1.hierarchy - sSparseRain.hierarchy) < sSparseThresholdHierarchy) {
            areaRainIntensity1 = RainIntensity.Sparse;
        }
        else {
            areaRainIntensity1 = RainIntensity.Normal;
        }

        Texture quadrant2 = quadrants.get(1);
        if (Math.abs(quadrant2.standardDeviation - sDenseRain.standardDeviation) < sDenseThresholdStandardDeviation
                && Math.abs(quadrant2.hierarchy - sDenseRain.hierarchy) < sDenseThresholdHierarchy) {
            areaRainIntensity2 = RainIntensity.Dense;
        }
        else if (Math.abs(quadrant2.standardDeviation - sSparseRain.standardDeviation) < sSparseThresholdStandardDeviation
                && Math.abs(quadrant2.hierarchy - sSparseRain.hierarchy) < sSparseThresholdHierarchy) {
            areaRainIntensity2 = RainIntensity.Sparse;
        }
        else {
            areaRainIntensity2 = RainIntensity.Normal;
        }

        // 先判断大雨势，再判断小雨势
        if (areaRainIntensity1 == RainIntensity.Dense || areaRainIntensity2 == RainIntensity.Dense) {
            this.rainIntensity = RainIntensity.Dense;
            result.addScore(Indicator.Stress, 1, FloatUtils.random(0.7, 0.8));
        }
        else if (areaRainIntensity1 == RainIntensity.Sparse || areaRainIntensity2 == RainIntensity.Sparse) {
            this.rainIntensity = RainIntensity.Sparse;
            result.addScore(Indicator.Stress, 1, FloatUtils.random(0.1, 0.2));
        }
        else {
            result.addScore(Indicator.Stress, 1, FloatUtils.random(0.4, 0.5));
        }

        Logger.d(this.getClass(), "#evalTracesDensity - rain intensity: " + this.rainIntensity.name());

        return result;
    }

    private EvaluationFeature evalPerson(SpaceLayout spaceLayout) {
        EvaluationFeature result = new EvaluationFeature();

        return result;
    }
}
