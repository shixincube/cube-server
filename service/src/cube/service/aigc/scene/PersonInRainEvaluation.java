/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.scene;

import cell.util.log.Logger;
import cube.aigc.psychology.*;
import cube.aigc.psychology.algorithm.Attention;
import cube.aigc.psychology.algorithm.PaintingConfidence;
import cube.aigc.psychology.algorithm.PerceptronThing;
import cube.aigc.psychology.algorithm.Tendency;
import cube.aigc.psychology.composition.PaintingFeatureSet;
import cube.aigc.psychology.composition.SpaceLayout;
import cube.aigc.psychology.composition.Texture;
import cube.aigc.psychology.material.Label;
import cube.aigc.psychology.material.Person;
import cube.aigc.psychology.material.Thing;
import cube.aigc.psychology.material.Tree;
import cube.aigc.psychology.material.other.DrawingSet;
import cube.aigc.psychology.material.other.Umbrella;
import cube.service.tokenizer.Tokenizer;
import cube.util.FloatUtils;

import java.util.ArrayList;
import java.util.List;

public class PersonInRainEvaluation extends Evaluation {

    private static Indicator[] sIndicators = new Indicator[] {
            Indicator.Stress,
            Indicator.Confidence,
            Indicator.Repression,
            Indicator.SelfConsciousness
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

    public enum DetailLevel {
        /**
         * 细节丰富。
         */
        Rich,

        /**
         * 细节一般。
         */
        Normal,

        /**
         * 细节匮乏。
         */
        Lack
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

    private Tokenizer tokenizer;

    private RainIntensity rainIntensity;

    private boolean rainShelterEffect = false;

    private boolean hasShelter = false;

    private DetailLevel detailLevel;

    private PaintingFeatureSet paintingFeatureSet;

    public PersonInRainEvaluation(long contactId, Painting painting, Tokenizer tokenizer) {
        super(contactId, painting);
        this.tokenizer = tokenizer;
    }

    @Override
    public EvaluationReport makeEvaluationReport() {
        List<EvaluationFeature> results = new ArrayList<>();

        SpaceLayout spaceLayout = new SpaceLayout(this.painting);
        results.add(this.evalSpaceStructure(spaceLayout));
        results.add(this.evalTracesDensity(spaceLayout));
        results.add(this.evalPerson(spaceLayout));
        results.add(this.evalRainShelteringMethods(spaceLayout));

        EvaluationReport report = new EvaluationReport(this.contactId, Theme.PersonInRain, this.painting.getAttribute(),
                Reference.Normal, new PaintingConfidence(this.painting), results);

        this.paintingFeatureSet = new PaintingFeatureSet(results, report.getRepresentationList());

        // 设置关注等级
        if (!this.hasShelter && !this.rainShelterEffect && this.rainIntensity == RainIntensity.Dense) {
            report.setAttention(Attention.FocusedAttention);
        }
        else if (!this.hasShelter && !this.rainShelterEffect) {
            report.setAttention(Attention.GeneralAttention);
        }
        else {
            report.setAttention(Attention.NoAttention);
        }

        return report;
    }

    @Override
    public PaintingFeatureSet getPaintingFeatureSet() {
        return this.paintingFeatureSet;
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
            else {
                String desc = "画面的画幅相对画布面积适中";
                result.addFeature(desc, Term.SelfConfidence, Tendency.Positive, PerceptronThing.createPictureSize());
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

            String desc = "画面里的雨势大";
            result.addFeature(desc, Term.MentalStress, Tendency.Positive, PerceptronThing.createPictureSense());
        }
        else if (areaRainIntensity1 == RainIntensity.Sparse || areaRainIntensity2 == RainIntensity.Sparse) {
            this.rainIntensity = RainIntensity.Sparse;
            result.addScore(Indicator.Stress, 1, FloatUtils.random(0.1, 0.2));

            String desc = "画面里的雨势小";
            result.addFeature(desc, Term.MentalStress, Tendency.Negative, PerceptronThing.createPictureSense());
        }
        else {
            result.addScore(Indicator.Stress, 1, FloatUtils.random(0.4, 0.5));

            String desc = "画面里的雨势一般";
            result.addFeature(desc, Term.MentalStress, Tendency.Normal, PerceptronThing.createPictureSense());
        }

        Logger.d(this.getClass(), "#evalTracesDensity - rain intensity: " + this.rainIntensity.name());

        // 根据雨势加入关键特征
        String title = "";
        switch (this.rainIntensity) {
            case Dense:
                title = "雨中人绘画中雨势大";
                break;
            case Sparse:
                title = "雨中人绘画中雨势小";
                break;
            default:
                title = "雨中人绘画中雨势一般";
                break;
        }
        String content = ContentTools.extract(title, this.tokenizer);
        if (null != content) {
            KeyFeature feature = new KeyFeature(title, content);
            result.addKeyFeature(feature);
        }
        else {
            Logger.w(this.getClass(), "#evalTracesDensity - NO title: " + title);
        }

        // 对画面下半部空间判断雨势
        Texture quadrant3 = quadrants.get(2);
        Texture quadrant4 = quadrants.get(3);
        double dSD1 = quadrant1.standardDeviation - quadrant3.standardDeviation;
        double dSD2 = quadrant2.standardDeviation - quadrant4.standardDeviation;

        if ((dSD1 < 0 && dSD2 < 0) || (dSD1 > 0.4 || dSD2 > 0.4)) {
            this.rainShelterEffect = true;
        }

        if (this.rainShelterEffect) {
            title = "雨中人绘画避雨有效";
        }
        else {
            title = "雨中人绘画避雨无效";
        }
        content = ContentTools.extract(title, this.tokenizer);
        if (null != content) {
            KeyFeature feature = new KeyFeature(title, content);
            result.addKeyFeature(feature);
        }
        else {
            Logger.w(this.getClass(), "#evalTracesDensity - NO title: " + title);
        }

        return result;
    }

    private EvaluationFeature evalPerson(SpaceLayout spaceLayout) {
        EvaluationFeature result = new EvaluationFeature();

        Person person = this.painting.getPerson();
        if (null != person) {
            // 大小比例
            long paintingArea = spaceLayout.getPaintingArea();
            long personArea = person.area;
            double areaRatio = ((double) personArea) / ((double) paintingArea);
            if (areaRatio > 0.21) {
                result.addScore(Indicator.Confidence, 1, FloatUtils.random(0.6, 0.7));
            }
            else if (areaRatio > 0.12) {
                result.addScore(Indicator.Confidence, 1, FloatUtils.random(0.5, 0.6));
            }
            else if (areaRatio > 0.04) {
                result.addScore(Indicator.Confidence, 1, FloatUtils.random(0.3, 0.4));
            }
            else {
                result.addScore(Indicator.Confidence, 1, FloatUtils.random(0.1, 0.2));
            }
        }

        this.detailLevel = DetailLevel.Lack;

        List<Person> personList = this.painting.getPersons();
        // 人的细节
        if (null != personList && !personList.isEmpty()) {
            for (Person p : personList) {
                int count = 0;
                if (p.hasEye()) {
                    ++count;
                }
                if (p.hasEyebrow()) {
                    ++count;
                }
                if (p.hasNose()) {
                    ++count;
                }
                if (p.hasMouth()) {
                    ++count;
                }
                if (p.hasEar()) {
                    ++count;
                }

                if (p.hasHair() || p.hasHairAccessory()) {
                    ++count;
                }

                if (p.hasBody()) {
                    ++count;
                }
                if (p.hasArm()) {
                    ++count;
                }

                if (count >= 5) {
                    this.detailLevel = DetailLevel.Rich;

                    String desc = "画面中的人物有绘制细节";
                    result.addFeature(desc, Term.SelfExistence, Tendency.Positive, p);
                }
                else if (count > 2) {
                    this.detailLevel = DetailLevel.Normal;
                    String desc = "画面中的人物细节一般";
                    result.addFeature(desc, Term.SelfExistence, Tendency.Normal, p);
                }
                else {
                    String desc = "画面中的人物没有细节";
                    result.addFeature(desc, Term.SelfExistence, Tendency.Negative, p);
                }

                if (this.detailLevel != DetailLevel.Lack) {
                    break;
                }
            }
        }

        Logger.d(this.getClass(), "#evalPerson - person detail level: " + this.detailLevel.name());

        String title = "";
        switch (this.detailLevel) {
            case Rich:
                title = "雨中人绘画中人物细节丰富";
                break;
            case Lack:
                title = "雨中人绘画中人物细节匮乏";
                break;
            default:
                title = "雨中人绘画中人物细节一般";
                break;
        }

        String content = ContentTools.extract(title, this.tokenizer);
        if (null != content) {
            KeyFeature feature = new KeyFeature(title, content);
            result.addKeyFeature(feature);
        }
        else {
            Logger.w(this.getClass(), "#evalPerson - NO title: " + title);
        }

        // 涂鸦情况
        if (null != personList && !personList.isEmpty()) {
            for (Person p : personList) {
                if (p.isDoodle()) {
                    result.addScore(Indicator.Repression, 1, FloatUtils.random(0.2, 0.3));
                }
            }
        }

        return result;
    }

    private EvaluationFeature evalRainShelteringMethods(SpaceLayout spaceLayout) {
        EvaluationFeature result = new EvaluationFeature();

        boolean agreement = false;

        DrawingSet drawingSet = this.painting.getDrawingSet();
        List<Thing> thingList = drawingSet.getList(Label.Umbrella);
        if (null != thingList && !thingList.isEmpty()) {
            this.hasShelter = true;

            List<Person> personList = this.painting.getPersons();
            if (null != personList && !personList.isEmpty()) {
                for (Thing thing : thingList) {
                    Umbrella umbrella = (Umbrella) thing;
                    Person selectedPerson = null;

                    int maxArea = 0;
                    for (Person person : personList) {
                        int area = person.boundingBox.calculateCollisionArea(umbrella.boundingBox);
                        if (area > maxArea) {
                            maxArea = area;
                            selectedPerson = person;
                        }
                    }

                    if (null != selectedPerson) {
                        if (umbrella.boundingBox.getCenterPoint().y > selectedPerson.boundingBox.getCenterPoint().y) {
                            agreement = true;
                            break;
                        }
                    }
                }
            }
        }

        if (!this.hasShelter) {
            List<Tree> treeList = this.painting.getTrees();
            List<Person> personList = this.painting.getPersons();
            if (null != treeList && !treeList.isEmpty() && null != personList && !personList.isEmpty()) {
                for (Tree tree : treeList) {
                    Person selectedPerson = null;
                    int maxArea = 0;
                    for (Person person : personList) {
                        int area = person.boundingBox.calculateCollisionArea(tree.boundingBox);
                        if (area > maxArea) {
                            maxArea = area;
                            selectedPerson = person;
                        }
                    }

                    if (null != selectedPerson) {
                        if (maxArea > selectedPerson.area * 0.02) {
                            this.hasShelter = true;
                            agreement = true;
                        }
                    }
                }
            }
        }

        String title = "";
        if (this.hasShelter) {
            title = "雨中人绘画出现避雨方式";
        }
        else {
            title = "雨中人绘画没有避雨方式";
        }

        String content = ContentTools.extract(title, this.tokenizer);
        if (null != content) {
            KeyFeature keyFeature = new KeyFeature(title, content);
            result.addKeyFeature(keyFeature);
        }
        else {
            Logger.w(this.getClass(), "#evalRainShelteringMethods - No title: " + title);
        }

        return result;
    }
}
