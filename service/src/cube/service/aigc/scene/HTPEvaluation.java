/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.scene;

import cell.util.Utils;
import cell.util.log.Logger;
import cube.aigc.psychology.*;
import cube.aigc.psychology.algorithm.PaintingConfidence;
import cube.aigc.psychology.algorithm.PerceptronThing;
import cube.aigc.psychology.algorithm.Score;
import cube.aigc.psychology.algorithm.Tendency;
import cube.aigc.psychology.composition.*;
import cube.aigc.psychology.material.*;
import cube.aigc.psychology.material.house.Window;
import cube.aigc.psychology.material.other.OtherSet;
import cube.aigc.psychology.material.person.Leg;
import cube.util.FloatUtils;
import cube.vision.BoundingBox;
import cube.vision.Point;
import cube.vision.Size;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * HTP 评估器。
 */
public class HTPEvaluation extends Evaluation {

    private double houseAreaRatioThreshold = 0.069;

    private final double treeAreaRatioThreshold = 0.049;

    private final double personAreaRatioThreshold = 0.015;

    private Size canvasSize;

    private SpaceLayout spaceLayout;

    private Reference reference;

    private PaintingFeatureSet paintingFeatureSet;

    private boolean printBigFive = false;

    public HTPEvaluation(long contactId, Attribute attribute) {
        this(contactId, new Painting(attribute));
    }

    public HTPEvaluation(long contactId, Painting painting) {
        super(contactId, painting);
        this.contactId = contactId;
        this.canvasSize = painting.getCanvasSize();
        this.spaceLayout = new SpaceLayout(painting);
        this.reference = Reference.Normal;
    }

    @Override
    public EvaluationReport makeEvaluationReport() {
        EvaluationReport report = null;

        if (null != this.painting && null != this.spaceLayout) {
            // 判断绘画是否是有效绘画
            if (!this.painting.isValid()) {
                Logger.w(this.getClass(), "#makeEvaluationReport - Painting is NOT valid");
                List<EvaluationFeature> list = new ArrayList<>();
                EvaluationFeature feature = new EvaluationFeature();
                feature.addScore(Indicator.Unknown, 1, FloatUtils.random(0.8, 0.9));
                list.add(feature);

                this.reference = Reference.Abnormal;
                Logger.d(this.getClass(), "#makeEvaluationReport - reference: " + this.reference.name);
                report = new EvaluationReport(this.contactId, this.painting.getAttribute(), this.reference,
                        new PaintingConfidence(this.painting), list);
                return report;
            }

            // 设置参数
            if (this.painting.getAttribute().age < 16) {
                this.houseAreaRatioThreshold = 0.055;
            }

            // 绘画是否为空
            this.reference = this.painting.isEmpty() ? Reference.Abnormal : Reference.Normal;

            List<EvaluationFeature> results = new ArrayList<>();
            results.add(this.evalSpaceStructure());
            results.add(this.evalFrameStructure());
            results.add(this.evalHouse());
            results.add(this.evalTree());
            results.add(this.evalPerson());
            results.add(this.evalOthers());
            // 矫正
            results = this.correct(results);
            report = new EvaluationReport(this.contactId, this.painting.getAttribute(), this.reference,
                    new PaintingConfidence(this.painting), results);

            this.paintingFeatureSet = new PaintingFeatureSet(results, report.getRepresentationList());
        }
        else {
            Logger.w(this.getClass(), "#makeEvaluationReport - Only for test");
            // 仅用于测试
            EvaluationFeature result = new EvaluationFeature();
            int num = Utils.randomInt(3, 5);
            for (int i = 0; i < num; ++i) {
                int index = Utils.randomInt(0, Term.values().length - 1);
                result.addFeature("", Term.values()[index], Tendency.Positive);
            }
            report = new EvaluationReport(this.contactId, this.painting.getAttribute(), this.reference,
                    new PaintingConfidence(this.painting), result);
        }

        return report;
    }

    @Override
    public PaintingFeatureSet getPaintingFeatureSet() {
        return this.paintingFeatureSet;
    }

    private EvaluationFeature evalSpaceStructure() {
        EvaluationFeature result = new EvaluationFeature();

        // 画面大小比例
        double areaRatio = this.spaceLayout.getAreaRatio();

        Logger.d(this.getClass(), "#evalSpaceStructure - Area ratio: " + areaRatio +
                " - TRBL: " + spaceLayout.getTopMargin() + "," + spaceLayout.getRightMargin() +
                "," + spaceLayout.getBottomMargin() + "," + spaceLayout.getLeftMargin());

        if (areaRatio > 0) {
            if (areaRatio <= 0.09) {
                // 画幅小，偏模
                this.reference = Reference.Abnormal;
                Logger.d(this.getClass(), "#evalSpaceStructure - Abnormal: 画幅小");

                String desc = "画面的画幅相对画布面积非常小";
                result.addFeature(desc, Term.SelfConfidence, Tendency.Negative, PerceptronThing.createPictureSize());
                result.addFeature(desc, Term.SelfExistence, Tendency.Negative, PerceptronThing.createPictureSize());
                result.addFeature(desc, Term.SenseOfSecurity, Tendency.Negative, PerceptronThing.createPictureSize());

                result.addScore(Indicator.Psychosis, 1, FloatUtils.random(0.2, 0.3));
                result.addScore(Indicator.Confidence, -1, FloatUtils.random(0.5, 0.6));
                result.addScore(Indicator.Depression, 1, FloatUtils.random(0.6, 0.7));

                result.addFiveFactor(BigFiveFactor.Obligingness, FloatUtils.random(1.0, 2.0));
                result.addFiveFactor(BigFiveFactor.Conscientiousness, FloatUtils.random(3.0, 4.0));
                if (printBigFive) {
                    System.out.println("CP-001");
                }
            }
            else if (areaRatio < (1.0d / 6.0d)) {
                String desc = "画面的画幅相对画布面积较小";
                result.addFeature(desc, Term.SelfConfidence, Tendency.Negative, PerceptronThing.createPictureSize());
                result.addFeature(desc, Term.SelfExistence, Tendency.Negative, PerceptronThing.createPictureSize());
                result.addFeature(desc, Term.SocialAdaptability, Tendency.Negative, PerceptronThing.createPictureSize());

                result.addScore(Indicator.Confidence, -1, FloatUtils.random(0.4, 0.5));
                result.addScore(Indicator.SelfEsteem, -1, FloatUtils.random(0.4, 0.5));
                result.addScore(Indicator.SocialAdaptability, -1, FloatUtils.random(0.4, 0.5));

                result.addFiveFactor(BigFiveFactor.Obligingness, FloatUtils.random(1.5, 3.5));
                if (printBigFive) {
                    System.out.println("CP-002");
                }
            }
            else if (areaRatio >= 0.7d) {
                String desc = "画面的画幅占画布整体面积较大";
                result.addFeature(desc, Term.SelfExistence, Tendency.Positive, PerceptronThing.createPictureSize());
                result.addFeature(desc, Term.Extroversion, Tendency.Positive, PerceptronThing.createPictureSize());
                result.addFeature(desc, Term.Defensiveness, Tendency.Positive, PerceptronThing.createPictureSize());

                result.addScore(Indicator.Extroversion, 1, FloatUtils.random(0.4, 0.5));
                result.addScore(Indicator.Narcissism, 1, FloatUtils.random(0.2, 0.3));
                result.addScore(Indicator.SocialAdaptability, 1, FloatUtils.random(0.5, 0.6));
                result.addScore(Indicator.Optimism, 1, FloatUtils.random(0.3, 0.4));

                result.addFiveFactor(BigFiveFactor.Obligingness, FloatUtils.random(6.5, 7.0));
                result.addFiveFactor(BigFiveFactor.Extraversion, FloatUtils.random(5.0, 6.0));
                result.addFiveFactor(BigFiveFactor.Neuroticism, FloatUtils.random(8.0, 9.0));
                if (printBigFive) {
                    System.out.println("CP-098");
                }
            }
            else if (areaRatio >= 0.667d) {
                String desc = "画面的画幅占画布整体面积较大";
                result.addFeature(desc, Term.SelfExistence, Tendency.Positive, PerceptronThing.createPictureSize());
                result.addFeature(desc, Term.Extroversion, Tendency.Positive, PerceptronThing.createPictureSize());
                result.addFeature(desc, Term.Defensiveness, Tendency.Positive, PerceptronThing.createPictureSize());

                result.addScore(Indicator.Extroversion, 1, FloatUtils.random(0.4, 0.5));
                result.addScore(Indicator.Narcissism, 1, FloatUtils.random(0.2, 0.3));
                result.addScore(Indicator.SocialAdaptability, 1, FloatUtils.random(0.5, 0.6));
                result.addScore(Indicator.Optimism, 1, FloatUtils.random(0.3, 0.4));

                result.addFiveFactor(BigFiveFactor.Obligingness, FloatUtils.random(7.5, 8.0));
                result.addFiveFactor(BigFiveFactor.Extraversion, FloatUtils.random(7.0, 8.0));
                if (printBigFive) {
                    System.out.println("CP-003");
                }
            }
            else {
                String desc = "画面的画幅占画布整体面积适中";
                result.addFeature(desc, Term.SelfExistence, Tendency.Normal, PerceptronThing.createPictureSize());
                result.addFeature(desc, Term.Extroversion, Tendency.Normal, PerceptronThing.createPictureSize());
                result.addFeature(desc, Term.SelfConfidence, Tendency.Normal, PerceptronThing.createPictureSize());

                result.addScore(Indicator.Extroversion, 1, FloatUtils.random(0.3, 0.4));
                result.addScore(Indicator.SocialAdaptability, 1, FloatUtils.random(0.6, 0.7));

                result.addFiveFactor(BigFiveFactor.Obligingness, FloatUtils.random(6.0, 7.0));
                result.addFiveFactor(BigFiveFactor.Conscientiousness, FloatUtils.random(7.0, 8.0));
                result.addFiveFactor(BigFiveFactor.Extraversion, FloatUtils.random(4.0, 5.0));
                result.addFiveFactor(BigFiveFactor.Achievement, FloatUtils.random(6.0, 7.0));
                result.addFiveFactor(BigFiveFactor.Neuroticism, FloatUtils.random(3.0, 4.0));
                if (printBigFive) {
                    System.out.println("CP-004");
                }
            }
        }

        // 房、树、人之间的空间关系
        // 中线
        double banding = ((double) this.painting.getCanvasSize().height) * 0.167;
        double bl = this.painting.getCanvasSize().height * 0.5;
        double centerYOffset = this.painting.getCanvasSize().height * 0.033;   // 经验值
        int evalRange = (int) Math.round(banding * 0.6);

        House house = this.painting.getHouse();
        Tree tree = this.painting.getTree();
        Person person = this.painting.getPerson();

        if (null != house && null != tree && null != person) {
            // 位置关系，使用 box 计算位置
            Point hc = house.box.getCenterPoint();
            Point tc = tree.box.getCenterPoint();
            Point pc = person.box.getCenterPoint();

            // 判断上半将中线上移；判断下半将中心下移
            boolean houseTHalf = (hc.y + house.box.y0) < (bl - centerYOffset);
            boolean houseBHalf = (hc.y + house.box.y0) > (bl + centerYOffset);
            boolean treeTHalf = (tc.y + tree.box.y0) < (bl - centerYOffset);
            boolean treeBHalf = (tc.y + tree.box.y0) > (bl + centerYOffset);
            boolean personTHalf = (pc.y + person.box.y0) < (bl - centerYOffset);
            boolean personBHalf = (pc.y + person.box.y0) > (bl + centerYOffset);

            // 相对位置判断
            if (Math.abs(house.box.y1 - tree.box.y1) < evalRange && Math.abs(house.box.y1 - person.box.y1) < evalRange) {
                // 基本在一个水平线上
                String desc = "画面无远近感，空间布局单一";
                result.addFeature(desc, Term.Stereotype, Tendency.Positive, PerceptronThing.createThingPosition(
                        new Thing[] { house, tree, person }));
                result.addFeature(desc, Term.Childish, Tendency.Positive, PerceptronThing.createThingPosition(
                        new Thing[] { house, tree, person }));

                result.addScore(Indicator.SelfConsciousness, 1, FloatUtils.random(0.3, 0.4));
            }
            else {
                // 位置分散
                String desc = "画面中的主要元素位置较分散";
                result.addFeature(desc, Term.EmotionalStability, Tendency.Positive, PerceptronThing.createThingPosition(
                        new Thing[] { house, tree, person }));

                result.addScore(Indicator.Mood, -1, FloatUtils.random(0.3, 0.4));

                result.addFiveFactor(BigFiveFactor.Obligingness, FloatUtils.random(5.0, 5.5));
                result.addFiveFactor(BigFiveFactor.Conscientiousness, FloatUtils.random(7.0, 8.0));
                result.addFiveFactor(BigFiveFactor.Extraversion, FloatUtils.random(3.0, 3.5));
                result.addFiveFactor(BigFiveFactor.Neuroticism, FloatUtils.random(3.0, 4.0));
                if (printBigFive) {
                    System.out.println("CP-005");
                }
            }

            // 绝对位置判断
            if (houseTHalf && treeTHalf && personTHalf) {
                // 整体偏上
                String desc = "画面中的主要元素的构图偏向上半画幅";
                result.addFeature(desc, Term.Idealization, Tendency.Positive, PerceptronThing.createThingPosition(
                        new Thing[] { house, tree, person }));
                result.addFeature(desc, Term.Fantasy, Tendency.Positive, PerceptronThing.createThingPosition(
                        new Thing[] { house, tree, person }));
                result.addFeature(desc, Term.PositiveExpectation, Tendency.Positive, PerceptronThing.createThingPosition(
                        new Thing[] { house, tree, person }));

                result.addScore(Indicator.Idealism, 1, FloatUtils.random(0.3, 0.4));
                result.addScore(Indicator.Mood, 1, FloatUtils.random(0.3, 0.4));
            }
            else if (houseBHalf && treeBHalf && personBHalf) {
                // 整体偏下
                String desc = "画面中的主要元素的构图偏向下半画幅";
                result.addFeature(desc, Term.Instinct, Tendency.Positive, PerceptronThing.createThingPosition(
                        new Thing[] { house, tree, person }));
                result.addFeature(desc, Term.SenseOfSecurity, Tendency.Negative, PerceptronThing.createThingPosition(
                        new Thing[] { house, tree, person }));

                result.addScore(Indicator.Realism, 1, FloatUtils.random(0.3, 0.4));
                result.addScore(Indicator.Thought, 1, FloatUtils.random(0.3, 0.4));
                result.addScore(Indicator.SenseOfSecurity, -1, FloatUtils.random(0.2, 0.3));

                result.addFiveFactor(BigFiveFactor.Neuroticism, FloatUtils.random(4.5, 5.5));
                if (printBigFive) {
                    System.out.println("CP-006");
                }
            }
            else {
                String desc = "画面中主要元素在构图结构上整体结构分布分散";
                result.addFeature(desc, Term.EnvironmentalFriendliness, Tendency.Positive, PerceptronThing.createThingPosition(
                        new Thing[] { house, tree, person }));

                result.addScore(Indicator.SelfConsciousness, 1, FloatUtils.random(0.3, 0.4));
            }

            // 大小关系
            int ha = house.area;
            int ta = tree.area;
            int pa = person.area;
            if (ha >= ta && ha >= pa) {
                // 房大
                String desc = "画面中房元素整体较大";
                result.addFeature(desc, Term.PayAttentionToFamily, Tendency.Positive, PerceptronThing.createThingSize(
                        new Thing[] { house, tree, person }));

                result.addScore(Indicator.Family, 1, FloatUtils.random(0.3, 0.4));

                result.addFiveFactor(BigFiveFactor.Obligingness, FloatUtils.random(7.5, 8.5));
                result.addFiveFactor(BigFiveFactor.Conscientiousness, FloatUtils.random(5.0, 5.5));
                if (printBigFive) {
                    System.out.println("CP-007");
                }
            }
            if (ta >= ha && ta >= pa) {
                // 树大
                String desc = "画面中树元素整体较大";
                result.addFeature(desc, Term.SocialDemand, Tendency.Positive, PerceptronThing.createThingSize(
                        new Thing[] { house, tree, person }));

                result.addScore(Indicator.InterpersonalRelation, 1, FloatUtils.random(0.3, 0.4));

                result.addFiveFactor(BigFiveFactor.Obligingness, FloatUtils.random(5.0, 5.5));
                result.addFiveFactor(BigFiveFactor.Conscientiousness, FloatUtils.random(7.5, 8.5));
                result.addFiveFactor(BigFiveFactor.Extraversion, FloatUtils.random(8.0, 8.5));
                result.addFiveFactor(BigFiveFactor.Neuroticism, FloatUtils.random(7.0, 8.0));
                if (printBigFive) {
                    System.out.println("CP-008");
                }
            }
            if (pa >= ha && pa >= ta) {
                // 人大
                String desc = "画面中人元素整体较大";
                result.addFeature(desc, Term.SelfDemand, Tendency.Positive, PerceptronThing.createThingSize(
                        new Thing[] { house, tree, person }));
                result.addFeature(desc, Term.SelfInflated, Tendency.Positive, PerceptronThing.createThingSize(
                        new Thing[] { house, tree, person }));
                result.addFeature(desc, Term.SelfControl, Tendency.Negative, PerceptronThing.createThingSize(
                        new Thing[] { house, tree, person }));

                result.addScore(Indicator.SelfConsciousness, 1, FloatUtils.random(0.3, 0.4));

                result.addFiveFactor(BigFiveFactor.Obligingness, FloatUtils.random(4.5, 5.5));
                result.addFiveFactor(BigFiveFactor.Achievement, FloatUtils.random(5.5, 6.5));
                if (printBigFive) {
                    System.out.println("CP-009");
                }
            }

            // 距离
            int distHT = house.distance(tree);
            int distPH = person.distance(house);
            int distTP = tree.distance(person);
            Logger.d(this.getClass(), "#evalSpaceStructure - distance: " + distHT + "/" + distPH + "/" + distTP);
            int count = 0;
            if (distHT > 1) {
                ++count;
            }
            if (distPH > 1) {
                ++count;
            }
            if (distTP > 1) {
                ++count;
            }

            if (count > 1) {
                String desc = "画面中各主要元素有各自清晰的位置空间";
                result.addFeature(desc, Term.SenseOfReality, Tendency.Positive, PerceptronThing.createThingSize(
                        new Thing[] { house, tree, person }));
                result.addFeature(desc, Term.Depression, Tendency.Negative, PerceptronThing.createThingSize(
                        new Thing[] { house, tree, person }));

                result.addScore(Indicator.Realism, 1, FloatUtils.random(0.3, 0.4));
                result.addScore(Indicator.Depression, -1, FloatUtils.random(0.1 * count, 0.15 * count));
                // FIXME 1030
                result.addFiveFactor(BigFiveFactor.Obligingness, FloatUtils.random(6.0, 7.0));
                result.addFiveFactor(BigFiveFactor.Extraversion, FloatUtils.random(7.0, 8.0));
                result.addFiveFactor(BigFiveFactor.Neuroticism, FloatUtils.random(1.0, 2.0));
                if (printBigFive) {
                    System.out.println("CP-010");
                }
            }
            else if (count > 0) {
                String desc = "画面中各主要元素在空间上远近适度";
                result.addFeature(desc, Term.SenseOfReality, Tendency.Positive, PerceptronThing.createThingSize(
                        new Thing[] { house, tree, person }));
                result.addFeature(desc, Term.Depression, Tendency.Negative, PerceptronThing.createThingSize(
                        new Thing[] { house, tree, person }));

                result.addScore(Indicator.Depression, -1, FloatUtils.random(0.1 * count, 0.15 * count));

                result.addFiveFactor(BigFiveFactor.Neuroticism, FloatUtils.random(2.0, 3.0));
                if (printBigFive) {
                    System.out.println("CP-011");
                }
            }

            if (distTP < 0) {
                // 树和人重叠
                int area = tree.box.calculateCollisionArea(person.box);
                if (Math.abs((person.box.width * person.box.height) - area) < 100) {
                    // 人和树基本重叠
                    String desc = "画面中人与树在空间位置上重叠";
                    result.addFeature(desc, Term.Depression, Tendency.Positive, PerceptronThing.createThingSize(
                            new Thing[] { tree, person }));
                    result.addScore(Indicator.Depression, 1, FloatUtils.random(0.6, 0.7));
                    System.out.println("DEBUG: Depression - 画面中人与树在空间位置上重叠");
                }
            }

            if (distHT > 10 && distPH > 10 && distTP > 10) {
                // HTP 距离大，判断所占画幅大小
                double paintingArea = this.spaceLayout.getPaintingArea();
                double rH = house.area / paintingArea;
                double rT = tree.area / paintingArea;
                double rP = person.area / paintingArea;
                Logger.d(this.getClass(), "#evalSpaceStructure - Area ratio: " + rH + "," + rT + "," + rP);
                if ((rH < 0.1 && rT < 0.1) || (rH < 0.1 && rP < 0.1) || (rT < 0.1 && rP < 0.1)) {
                    String desc = "画面中各主要元素所占空间较小";
                    result.addFeature(desc, Term.SocialPowerlessness, Tendency.Positive, PerceptronThing.createThingSize(
                            new Thing[] { house, tree, person }));
                    result.addScore(Indicator.Anxiety, 1, FloatUtils.random(0.3, 0.4));
                }

                String desc = "画面中各主要元素之间距离适度，远近合适";
                result.addFeature(desc, Term.SenseOfReality, Tendency.Positive, PerceptronThing.createThingSize(
                        new Thing[] { house, tree, person }));
                result.addFeature(desc, Term.Extroversion, Tendency.Positive, PerceptronThing.createThingSize(
                        new Thing[] { house, tree, person }));
                result.addFeature(desc, Term.PursuitOfAchievement, Tendency.Positive, PerceptronThing.createThingSize(
                        new Thing[] { house, tree, person }));

                result.addFiveFactor(BigFiveFactor.Extraversion, FloatUtils.random(6.5, 7.5));
                result.addFiveFactor(BigFiveFactor.Achievement, FloatUtils.random(8.5, 9.0));
                if (printBigFive) {
                    System.out.println("CP-012");
                }
            }

            // 三者都有
            if (house.numComponents() > 2 && tree.numComponents() > 2 && person.numComponents() > 1) {
                String desc = "画面中的主要元素都绘制了若干细节元素";
                result.addFeature(desc, Term.AttentionToDetail, Tendency.Positive, new Thing[] {
                        house, tree, person
                });

                result.addScore(Indicator.Depression, -1, FloatUtils.random(0.75, 0.85));

                result.addFiveFactor(BigFiveFactor.Obligingness, FloatUtils.random(5.0, 5.5));
                result.addFiveFactor(BigFiveFactor.Conscientiousness, FloatUtils.random(7.0, 8.0));
                result.addFiveFactor(BigFiveFactor.Extraversion, FloatUtils.random(7.0, 8.0));
                if (printBigFive) {
                    System.out.println("CP-013");
                }
            }
        }
        else if (null != house && null != tree) {
            // 位置关系，使用 box 计算位置
            Point hc = house.box.getCenterPoint();
            Point tc = tree.box.getCenterPoint();

            // 判断上半将中线上移；判断下半将中心下移
            boolean houseTHalf = (hc.y + house.box.y0) < (bl - centerYOffset);
            boolean houseBHalf = (hc.y + house.box.y0) > (bl + centerYOffset);
            boolean treeTHalf = (tc.y + tree.box.y0) < (bl - centerYOffset);
            boolean treeBHalf = (tc.y + tree.box.y0) > (bl + centerYOffset);

            if (Math.abs(house.box.y1 - tree.box.y1) < evalRange) {
                // 基本在一个水平线上
                String desc = "画面中主要元素在构图结构上无远近感";
                result.addFeature(desc, Term.Stereotype, Tendency.Positive, PerceptronThing.createThingPosition(
                        new Thing[] { house, tree }));
                result.addFeature(desc, Term.Childish, Tendency.Positive, PerceptronThing.createThingPosition(
                        new Thing[] { house, tree }));

                result.addScore(Indicator.SelfConsciousness, 1, FloatUtils.random(0.3, 0.4));
            }

            // 绝对位置判断
            if (houseTHalf && treeTHalf) {
                // 整体偏上
                String desc = "画面中的主要元素的构图偏向上半画幅";
                result.addFeature(desc, Term.Idealization, Tendency.Positive, PerceptronThing.createThingPosition(
                        new Thing[] { house, tree }));
                result.addFeature(desc, Term.Fantasy, Tendency.Positive, PerceptronThing.createThingPosition(
                        new Thing[] { house, tree }));
                result.addFeature(desc, Term.PositiveExpectation, Tendency.Positive, PerceptronThing.createThingPosition(
                        new Thing[] { house, tree }));

                result.addScore(Indicator.Idealism, 1, FloatUtils.random(0.3, 0.4));
                result.addScore(Indicator.Mood, 1, FloatUtils.random(0.3, 0.4));
            }
            else if (houseBHalf && treeBHalf) {
                // 整体偏下
                String desc = "画面中的主要元素的构图偏向下半画幅";
                result.addFeature(desc, Term.SenseOfReality, Tendency.Positive, PerceptronThing.createThingPosition(
                        new Thing[] { house, tree }));
                result.addFeature(desc, Term.Instinct, Tendency.Positive, PerceptronThing.createThingPosition(
                        new Thing[] { house, tree }));
                result.addFeature(desc, Term.SenseOfSecurity, Tendency.Negative, PerceptronThing.createThingPosition(
                        new Thing[] { house, tree }));

                result.addScore(Indicator.Realism, 1, FloatUtils.random(0.3, 0.4));
                result.addScore(Indicator.Thought, 1, FloatUtils.random(0.3, 0.4));
                result.addScore(Indicator.SenseOfSecurity, -1, FloatUtils.random(0.2, 0.3));

                result.addFiveFactor(BigFiveFactor.Neuroticism, FloatUtils.random(4.5, 5.5));
                if (printBigFive) {
                    System.out.println("CP-014");
                }
            }
            else {
                String desc = "画面中主要元素在构图结构上整体结构分布分散";
                result.addFeature(desc, Term.SelfExistence, Tendency.Normal, PerceptronThing.createThingPosition(
                        new Thing[] { house, tree }));
                result.addScore(Indicator.SelfConsciousness, 1, FloatUtils.random(0.3, 0.4));
            }

            int ha = house.area;
            int ta = tree.area;
            if (ha > ta) {
                // 房大
                String desc = "画面中房元素整体结构明显";
                result.addFeature(desc, Term.PayAttentionToFamily, Tendency.Positive, PerceptronThing.createThingSize(
                        new Thing[] { house, tree }));

                result.addScore(Indicator.Family, 1, FloatUtils.random(0.3, 0.4));

                result.addFiveFactor(BigFiveFactor.Obligingness, FloatUtils.random(7.5, 8.5));
                result.addFiveFactor(BigFiveFactor.Extraversion, FloatUtils.random(8.0, 9.0));
                if (printBigFive) {
                    System.out.println("CP-015");
                }
            }
            else {
                // 树大
                String desc = "画面中树元素整体结构明显";
                result.addFeature(desc, Term.SocialDemand, Tendency.Positive, PerceptronThing.createThingSize(
                        new Thing[] { house, tree }));

                result.addScore(Indicator.InterpersonalRelation, 1, FloatUtils.random(0.3, 0.4));
            }

            // 间距
            Logger.d(this.getClass(), "#evalSpaceStructure - distance: HT - " + house.distance(tree));
            if (house.distance(tree) > 0) {
                result.addScore(Indicator.Depression, -1, FloatUtils.random(0.1, 0.2));
            }
        }
        else if (null != house && null != person) {
            // 位置关系，使用 box 计算位置
            Point hc = house.box.getCenterPoint();
            Point pc = person.box.getCenterPoint();

            boolean houseTHalf = (hc.y + house.box.y0) < (bl - centerYOffset);
            boolean houseBHalf = (hc.y + house.box.y0) > (bl + centerYOffset);
            boolean personTHalf = (pc.y + person.box.y0) < (bl - centerYOffset);
            boolean personBHalf = (pc.y + person.box.y0) > (bl + centerYOffset);

            if (Math.abs(house.box.y1 - person.box.y1) < evalRange) {
                // 基本在一个水平线上
                String desc = "画面中主要元素在构图结构上无远近感";
                result.addFeature(desc, Term.Stereotype, Tendency.Positive, PerceptronThing.createThingPosition(
                        new Thing[] { house, person }));
                result.addFeature(desc, Term.Childish, Tendency.Positive, PerceptronThing.createThingPosition(
                        new Thing[] { house, person }));

                result.addScore(Indicator.SelfConsciousness, 1, FloatUtils.random(0.3, 0.4));
            }

            // 绝对位置判断
            if (houseTHalf && personTHalf) {
                // 整体偏上
                String desc = "画面中的主要元素的构图偏向上半画幅";
                result.addFeature(desc, Term.Idealization, Tendency.Positive, PerceptronThing.createThingPosition(
                        new Thing[] { house, person }));
                result.addFeature(desc, Term.Fantasy, Tendency.Positive, PerceptronThing.createThingPosition(
                        new Thing[] { house, person }));
                result.addFeature(desc, Term.PositiveExpectation, Tendency.Positive, PerceptronThing.createThingPosition(
                        new Thing[] { house, person }));

                result.addScore(Indicator.Idealism, 1, FloatUtils.random(0.3, 0.4));
                result.addScore(Indicator.Mood, 1, FloatUtils.random(0.3, 0.4));
            }
            else if (houseBHalf && personBHalf) {
                // 整体偏下
                String desc = "画面中的主要元素的构图偏向下半画幅";
                result.addFeature(desc, Term.SenseOfReality, Tendency.Positive, PerceptronThing.createThingPosition(
                        new Thing[] { house, person }));
                result.addFeature(desc, Term.Instinct, Tendency.Positive, PerceptronThing.createThingPosition(
                        new Thing[] { house, person }));
                result.addFeature(desc, Term.SenseOfSecurity, Tendency.Negative, PerceptronThing.createThingPosition(
                        new Thing[] { house, person }));

                result.addScore(Indicator.Realism, 1, FloatUtils.random(0.3, 0.4));
                result.addScore(Indicator.Thought, 1, FloatUtils.random(0.3, 0.4));
                result.addScore(Indicator.SenseOfSecurity, -1, FloatUtils.random(0.2, 0.3));

                result.addFiveFactor(BigFiveFactor.Neuroticism, FloatUtils.random(4.5, 5.5));
                if (printBigFive) {
                    System.out.println("CP-016");
                }
            }
            else {
                String desc = "画面中主要元素在构图结构上整体结构分布分散";
                result.addFeature(desc, Term.SelfExistence, Tendency.Normal, PerceptronThing.createThingPosition(
                        new Thing[] { house, person }));
                result.addScore(Indicator.SelfConsciousness, 1, FloatUtils.random(0.3, 0.4));
            }

            int ha = house.area;
            int pa = person.area;
            if (ha > pa) {
                // 房大
                String desc = "画面中房元素整体结构明显";
                result.addFeature(desc, Term.PayAttentionToFamily, Tendency.Positive, PerceptronThing.createThingSize(
                        new Thing[] { house, person }));

                result.addScore(Indicator.Family, 1, FloatUtils.random(0.3, 0.4));

                result.addFiveFactor(BigFiveFactor.Obligingness, FloatUtils.random(7.0, 8.0));
                result.addFiveFactor(BigFiveFactor.Extraversion, FloatUtils.random(7.5, 8.0));
                result.addFiveFactor(BigFiveFactor.Achievement, FloatUtils.random(5.0, 5.5));
                if (printBigFive) {
                    System.out.println("CP-017");
                }
            }
            else {
                // 人大
                String desc = "画面中人元素整体结构明显";
                result.addFeature(desc, Term.SelfDemand, Tendency.Positive, PerceptronThing.createThingSize(
                        new Thing[] { house, person }));
                result.addFeature(desc, Term.SelfInflated, Tendency.Positive, PerceptronThing.createThingSize(
                        new Thing[] { house, person }));
                result.addFeature(desc, Term.SelfControl, Tendency.Negative, PerceptronThing.createThingSize(
                        new Thing[] { house, person }));

                result.addScore(Indicator.Family, 1, FloatUtils.random(0.1, 0.2));
                result.addScore(Indicator.SelfConsciousness, 1, FloatUtils.random(0.3, 0.4));
            }

            // 间距
            Logger.d(this.getClass(), "#evalSpaceStructure - distance: PH - " + house.distance(person));
            if (house.distance(person) > 0) {
                result.addScore(Indicator.Depression, -1, FloatUtils.random(0.1, 0.2));
            }
        }
        else if (null != tree && null != person) {
            // 位置关系，使用 box 计算位置
            Point tc = tree.box.getCenterPoint();
            Point pc = person.box.getCenterPoint();

            // 判断上半将中线上移；判断下半将中心下移
            boolean treeTHalf = (tc.y + tree.box.y0) < (bl - centerYOffset);
            boolean treeBHalf = (tc.y + tree.box.y0) > (bl + centerYOffset);
            boolean personTHalf = (pc.y + person.box.y0) < (bl - centerYOffset);
            boolean personBHalf = (pc.y + person.box.y0) > (bl + centerYOffset);

            if (Math.abs(tree.box.y1 - person.box.y1) < evalRange) {
                // 基本在一个水平线上
                String desc = "画面中主要元素在构图结构上无远近感";
                result.addFeature(desc, Term.Stereotype, Tendency.Positive, PerceptronThing.createThingPosition(
                        new Thing[] { tree, person }));
                result.addFeature(desc, Term.Childish, Tendency.Positive, PerceptronThing.createThingPosition(
                        new Thing[] { tree, person }));

                result.addScore(Indicator.SelfConsciousness, 1, FloatUtils.random(0.3, 0.4));
            }

            // 绝对位置判断
            if (treeTHalf && personTHalf) {
                // 整体偏上
                String desc = "画面中的主要元素的构图偏向上半画幅";
                result.addFeature(desc, Term.Idealization, Tendency.Positive, PerceptronThing.createThingPosition(
                        new Thing[] { tree, person }));
                result.addFeature(desc, Term.Fantasy, Tendency.Positive, PerceptronThing.createThingPosition(
                        new Thing[] { tree, person }));
                result.addFeature(desc, Term.PositiveExpectation, Tendency.Positive, PerceptronThing.createThingPosition(
                        new Thing[] { tree, person }));

                result.addScore(Indicator.Idealism, 1, FloatUtils.random(0.3, 0.4));
                result.addScore(Indicator.Mood, 1, FloatUtils.random(0.3, 0.4));
            }
            else if (treeBHalf && personBHalf) {
                // 整体偏下
                String desc = "画面中的主要元素的构图偏向下半画幅";
                result.addFeature(desc, Term.SenseOfReality, Tendency.Positive, PerceptronThing.createThingPosition(
                        new Thing[] { tree, person }));
                result.addFeature(desc, Term.Instinct, Tendency.Positive, PerceptronThing.createThingPosition(
                        new Thing[] { tree, person }));
                result.addFeature(desc, Term.SenseOfSecurity, Tendency.Negative, PerceptronThing.createThingPosition(
                        new Thing[] { tree, person }));

                result.addScore(Indicator.Realism, 1, FloatUtils.random(0.3, 0.4));
                result.addScore(Indicator.Thought, 1, FloatUtils.random(0.3, 0.4));
                result.addScore(Indicator.SenseOfSecurity, -1, FloatUtils.random(0.2, 0.3));

                result.addFiveFactor(BigFiveFactor.Neuroticism, FloatUtils.random(4.5, 5.5));
                if (printBigFive) {
                    System.out.println("CP-018");
                }
            }
            else {
                String desc = "画面中主要元素在构图结构上整体结构分布分散";
                result.addFeature(desc, Term.SelfExistence, Tendency.Normal, PerceptronThing.createThingPosition(
                        new Thing[] { tree, person }));
                result.addScore(Indicator.SelfConsciousness, 1, FloatUtils.random(0.3, 0.4));
            }

            int ta = tree.area;
            int pa = person.area;
            if (ta > pa) {
                // 树大
                String desc = "画面中树元素整体结构明显";
                result.addFeature(desc, Term.SocialDemand, Tendency.Positive, PerceptronThing.createThingSize(
                        new Thing[] { tree, person }));

                result.addScore(Indicator.InterpersonalRelation, 1, FloatUtils.random(0.3, 0.4));
            }
            else {
                // 人大
                String desc = "画面中人元素整体结构明显";
                result.addFeature(desc, Term.SelfDemand, Tendency.Positive, PerceptronThing.createThingSize(
                        new Thing[] { tree, person }));
                result.addFeature(desc, Term.SelfInflated, Tendency.Positive, PerceptronThing.createThingSize(
                        new Thing[] { tree, person }));
                result.addFeature(desc, Term.SelfControl, Tendency.Negative, PerceptronThing.createThingSize(
                        new Thing[] { tree, person }));

                result.addScore(Indicator.SelfConsciousness, 1, FloatUtils.random(0.3, 0.4));
            }

            // 间距
            Logger.d(this.getClass(), "#evalSpaceStructure - distance: TP - " + tree.distance(person));
            if (tree.distance(person) > 0) {
                result.addScore(Indicator.Depression, -1, FloatUtils.random(0.1, 0.2));
            }
        }
        else {
            // 房、树、人三元素中仅有一种元素

            // 偏模
            this.reference = Reference.Abnormal;
            Logger.d(this.getClass(), "#evalSpaceStructure - Abnormal: 房、树、人三元素中仅有一种元素");

            String desc = "画面中主要元素少于指导数量";
            result.addFeature(desc, Term.Escapism, Tendency.Positive);

            boolean onlyHouse = (null != house);
            boolean onlyTree = (null != tree);
            boolean onlyPerson = (null != person);

            Logger.d(this.getClass(), "#evalSpaceStructure - only one material: H/T/P - " +
                    onlyHouse + "/" + onlyTree + "/" + onlyPerson);

            double weight = FloatUtils.random(0.3, 0.4);
            Score score = result.getScore(Indicator.Confidence);
            if (null != score) {
                if (score.value < 0) {
                    weight += FloatUtils.random(0.2, 0.3);
                }
                else {
                    weight -= FloatUtils.random(0.2, 0.3);
                }
            }

            weight = Math.abs(weight);

            if (onlyHouse) {
                result.addScore(Indicator.Psychosis, 1, weight);
            }
            else if (onlyTree) {
                result.addScore(Indicator.Psychosis, 1, weight);
            }
            else if (onlyPerson) {
                result.addScore(Indicator.Psychosis, 1, weight);
            }
        }

        // 面积比例，建议不高于 0.010
        double tinyRatio = 0.008;
        long paintingArea = this.spaceLayout.getPaintingArea();
        if (null != person) {
            // 人整体大小
            int personArea = person.area;
            if (((double)personArea / (double)paintingArea) <= tinyRatio) {
                // 人很小
                String desc = "画面中人元素整体构成结构所占空间较小";
                result.addFeature(desc, Term.SenseOfSecurity, Tendency.Negative, PerceptronThing.createThingSize(
                        new Thing[] { person }));

                result.addScore(Indicator.SenseOfSecurity, -1, FloatUtils.random(0.3, 0.4));

                result.addFiveFactor(BigFiveFactor.Conscientiousness, FloatUtils.random(4.0, 5.0));
                result.addFiveFactor(BigFiveFactor.Neuroticism, FloatUtils.random(5.0, 6.0));
                if (printBigFive) {
                    System.out.println("CP-019");
                }
            }
        }

        // 画面涂鸦
        if (null != this.painting.getWhole()) {
            // 稀疏计数，大于等于3，说明画面过于简单
            int sparseness = 0;
            // 涂鸦计数，大于4，说明画面大面积涂鸦
            int doodles = 0;
            for (Texture texture : this.painting.getQuadrants()) {
                Logger.d(this.getClass(), "#evalSpaceStructure - Space texture:\n"
                        + texture.toJSON().toString(4));

                if (texture.max > 0 && texture.hierarchy > 0) {
                    // 判断画面涂鸦效果
                    /*
                     * 大面积涂鸦
                     * "avg": "6.334347345132742",
                     * "squareDeviation": "21.74551120817507",
                     * "density": "0.6666666666666666",
                     * "max": "15.851769911504423",
                     * "hierarchy": "0.7829784292035399",
                     * "standardDeviation": "4.663208252713476"
                     */
                    if (texture.avg >= 4.0 && texture.max >= 10) {
                        doodles += 2;
                    }
                    else if (texture.avg >= 2.0 && texture.density > 0.6 && texture.hierarchy > 0.02) {
                        doodles += 1;
                    }
                    else if (texture.density >= 0.5 && texture.max >= 5.0) {
                        doodles += 1;
                    }

                    // 判断画面稀疏感
                    if ((texture.max >= 2.0 || texture.max == 0.0) && texture.density < 0.8) {
                        // max 大于2或者等于0画面线条可能很粗，不判断画面疏密性
                        sparseness -= 1;
                    }
                    else if (texture.max < 0.1) {
                        sparseness += 1;
                    }
                    else if (texture.density < 0.1) {
                        sparseness += 2;
                    }
//                    if (texture.max >= 1.0 && texture.max < 2.0) {
//                        // 通过标准差和层密度，判断是否画面被反复涂鸦
//                        if (texture.standardDeviation >= 0.42 && texture.hierarchy <= 0.05) {
//                            doodles += 1;
//                        }
//                    }
                }
            }

            Logger.d(this.getClass(), "#evalSpaceStructure - Space whole texture:\n"
                    + this.painting.getWhole().toJSON().toString(4));
            // 整体观感
            if (this.painting.getWhole().max < 2.0 && this.painting.getWhole().max != 0) {
                if (this.painting.getWhole().density > 0.1 && this.painting.getWhole().density < 0.3) {
                    sparseness += 1;
                }
                else if (this.painting.getWhole().density <= 0.1) {
                    sparseness += 2;
                }
            }
            else if (this.painting.getWhole().max >= 2.0) {
                if (this.painting.getWhole().avg >= 4.0) {
                    doodles += 1;
                }
                else {
                    sparseness -= 1;
                }
            }

            if (doodles >= 5) {
                // 画面超过2/3画幅涂鸦
                String desc = "画面中出现大面积涂鸦，线条凌乱";
                result.addFeature(desc, Term.Anxiety, Tendency.Positive, PerceptronThing.createPictureSense());

                result.addScore(Indicator.Anxiety, 1, FloatUtils.random(0.8, 0.9));
                result.addScore(Indicator.Depression, 1, FloatUtils.random(0.8, 0.9));
                result.addScore(Indicator.Obsession, 1, FloatUtils.random(0.4, 0.5));

                System.out.println("DEBUG: Depression - 画面中出现大面积涂鸦，线条凌乱");

                result.addFiveFactor(BigFiveFactor.Obligingness, FloatUtils.random(2.0, 3.0));
                result.addFiveFactor(BigFiveFactor.Neuroticism, FloatUtils.random(8.0, 9.0));
                if (printBigFive) {
                    System.out.println("CP-100");
                }
            }
            else if (doodles >= 3) {
                // 画面超过1/2画幅涂鸦
                String desc = "画面中大部分画幅内容出现涂鸦";
                result.addFeature(desc, Term.Anxiety, Tendency.Positive, PerceptronThing.createPictureSense());

                result.addScore(Indicator.Anxiety, 1, FloatUtils.random(0.8, 0.9));

                result.addFiveFactor(BigFiveFactor.Obligingness, FloatUtils.random(4.0, 5.0));
                result.addFiveFactor(BigFiveFactor.Neuroticism, FloatUtils.random(8.0, 9.0));
                if (printBigFive) {
                    System.out.println("CP-099");
                }
            }
            else if (doodles >= 2) {
                // 画面有1/2画幅涂鸦
                Logger.d(this.getClass(), "#evalSpaceStructure - Space doodles: " + doodles);

                String desc = "画面中部分画幅内容出现涂鸦";
                result.addFeature(desc, Term.Anxiety, Tendency.Positive, PerceptronThing.createPictureSense());

                result.addScore(Indicator.Depression, 1, FloatUtils.random(0.2, 0.3));
                result.addScore(Indicator.Anxiety, 1, FloatUtils.random(0.6, 0.7));

                System.out.println("DEBUG: Depression - 画面中部分画幅内容出现涂鸦");

                result.addFiveFactor(BigFiveFactor.Obligingness, FloatUtils.random(4.0, 5.0));
                result.addFiveFactor(BigFiveFactor.Neuroticism, FloatUtils.random(7.0, 8.0));
                if (printBigFive) {
                    System.out.println("CP-020");
                }
            }
            else if (doodles >= 1) {
                // 画面有1/4画幅涂鸦
                Logger.d(this.getClass(), "#evalSpaceStructure - Space doodles: " + doodles);

                String desc = "画面中小部分画幅内容出现涂鸦";
                result.addFeature(desc, Term.Anxiety, Tendency.Positive, PerceptronThing.createPictureSense());

                result.addScore(Indicator.Anxiety, 1, FloatUtils.random(0.3, 0.4));

                result.addFiveFactor(BigFiveFactor.Obligingness, FloatUtils.random(5.0, 5.5));
                result.addFiveFactor(BigFiveFactor.Conscientiousness, FloatUtils.random(5.0, 5.5));
                result.addFiveFactor(BigFiveFactor.Neuroticism, FloatUtils.random(5.0, 5.5));
                if (printBigFive) {
                    System.out.println("CP-021");
                }
            }

            // 画面稀疏
            if (sparseness >= 4) {
                String desc = "画面整体绘画密度非常稀疏";
                result.addFeature(desc, Term.Creativity, Tendency.Negative, PerceptronThing.createPictureSense());

                result.addScore(Indicator.Depression, 1, FloatUtils.random(0.1, 0.2));
                result.addScore(Indicator.Anxiety, 1, FloatUtils.random(0.2, 0.3));
                result.addScore(Indicator.Creativity, -1, FloatUtils.random(0.1, 0.2));
                Logger.d(this.getClass(), "#evalSpaceStructure - Space sparseness: " + sparseness);

                result.addFiveFactor(BigFiveFactor.Neuroticism, FloatUtils.random(2.0, 3.0));
                if (printBigFive) {
                    System.out.println("CP-022");
                }
            }
            else if (sparseness >= 3) {
                String desc = "画面整体绘画密度较稀疏";
                result.addFeature(desc, Term.Creativity, Tendency.Negative, PerceptronThing.createPictureSense());

                result.addScore(Indicator.Creativity, -1, FloatUtils.random(0.1, 0.2));
                Logger.d(this.getClass(), "#evalSpaceStructure - Space sparseness: " + sparseness);

                result.addFiveFactor(BigFiveFactor.Extraversion, FloatUtils.random(1.0, 1.5));
                result.addFiveFactor(BigFiveFactor.Neuroticism, FloatUtils.random(3.0, 4.0));
                if (printBigFive) {
                    System.out.println("CP-023");
                }
            }
            else if (sparseness >= 2) {
                String desc = "画面整体绘画密度稀疏";
                result.addFeature(desc, Term.Anxiety, Tendency.Positive, PerceptronThing.createPictureSense());

                result.addScore(Indicator.Anxiety, 1, FloatUtils.random(0.5, 0.6));
                Logger.d(this.getClass(), "#evalSpaceStructure - Space sparseness: " + sparseness);

                result.addFiveFactor(BigFiveFactor.Obligingness, FloatUtils.random(3.5, 4.0));
                result.addFiveFactor(BigFiveFactor.Conscientiousness, FloatUtils.random(3.0, 4.0));
                result.addFiveFactor(BigFiveFactor.Neuroticism, FloatUtils.random(3.5, 4.0));
                if (printBigFive) {
                    System.out.println("CP-024");
                }
            }
            else if (sparseness >= 1) {
                String desc = "画面整体绘画密度有些许稀疏";
                result.addFeature(desc, Term.Anxiety, Tendency.Positive, PerceptronThing.createPictureSense());

                result.addScore(Indicator.Anxiety, 1, FloatUtils.random(0.4, 0.5));
                Logger.d(this.getClass(), "#evalSpaceStructure - Space sparseness: " + sparseness);

                result.addFiveFactor(BigFiveFactor.Neuroticism, FloatUtils.random(5.0, 6.0));
                if (printBigFive) {
                    System.out.println("CP-025");
                }
            }

            if (doodles >= 2 && sparseness >= 2) {
                this.reference = Reference.Abnormal;
                Logger.d(this.getClass(), "#evalSpaceStructure - Abnormal: 画幅大面积涂鸦且图形稀疏");

                String desc = "画面的画幅有涂鸦且画面稀疏";
                result.addFeature(desc, Term.MentalStress, Tendency.Positive, PerceptronThing.createPictureSense());
            }
        }

        return result;
    }

    private EvaluationFeature evalFrameStructure() {
        EvaluationFeature result = new EvaluationFeature();

        FrameStructureDescription description = this.calcFrameStructure(this.spaceLayout.getPaintingBox());
        if (description.isWholeTop()) {
            // 整体顶部
            String desc = "整个画幅结构偏向画布顶部";
            result.addFeature(desc, Term.Idealization, Tendency.Positive, PerceptronThing.createPictureLayout());
            result.addFeature(desc, Term.Fantasy, Tendency.Positive, PerceptronThing.createPictureLayout());
            result.addFeature(desc, Term.PositiveExpectation, Tendency.Positive, PerceptronThing.createPictureLayout());

            result.addFiveFactor(BigFiveFactor.Obligingness, FloatUtils.random(7.0, 7.5));
            result.addFiveFactor(BigFiveFactor.Conscientiousness, FloatUtils.random(7.0, 8.0));
            result.addFiveFactor(BigFiveFactor.Achievement, FloatUtils.random(7.0, 8.0));
            result.addFiveFactor(BigFiveFactor.Extraversion, FloatUtils.random(4.0, 5.0));
            result.addFiveFactor(BigFiveFactor.Neuroticism, FloatUtils.random(3.0, 3.5));
            if (printBigFive) {
                System.out.println("CP-026");
            }
        }
        else if (description.isWholeBottom()) {
            // 整体底部
            String desc = "整个画幅结构偏向画布底部";
            result.addFeature(desc, Term.Instinct, Tendency.Positive, PerceptronThing.createPictureLayout());
            result.addFeature(desc, Term.SenseOfSecurity, Tendency.Negative, PerceptronThing.createPictureLayout());

            result.addFiveFactor(BigFiveFactor.Conscientiousness, FloatUtils.random(2.0, 2.5));
            result.addFiveFactor(BigFiveFactor.Extraversion, FloatUtils.random(3.0, 4.0));
            result.addFiveFactor(BigFiveFactor.Achievement, FloatUtils.random(7.0, 8.0));
            result.addFiveFactor(BigFiveFactor.Neuroticism, FloatUtils.random(7.0, 8.0));
            if (printBigFive) {
                System.out.println("CP-027");
            }
        }
        else if (description.isWholeLeft()) {
            // 整体左边
            String desc = "整个画幅结构偏向画布左边";
            result.addFeature(desc, Term.Nostalgia, Tendency.Positive, PerceptronThing.createPictureLayout());

            result.addFiveFactor(BigFiveFactor.Achievement, FloatUtils.random(2.0, 3.0));
            if (printBigFive) {
                System.out.println("CP-028");
            }
        }
        else if (description.isWholeRight()) {
            // 整体右边
            String desc = "整个画幅结构偏向画布右边";
            result.addFeature(desc, Term.Future, Tendency.Positive, PerceptronThing.createPictureLayout());

            result.addFiveFactor(BigFiveFactor.Achievement, FloatUtils.random(7.0, 8.0));
            if (printBigFive) {
                System.out.println("CP-029");
            }
        }

//        spaceLayout.getTopMargin() + "," + spaceLayout.getRightMargin() +
//                "," + spaceLayout.getBottomMargin() + "," + spaceLayout.getLeftMargin());

        // 空间构图
        double minThreshold = this.canvasSize.width * 0.025f;
        double maxThreshold = this.canvasSize.width * 0.15f;

        int minMarginCount = 0;
        int maxMarginCount = 0;

        if (this.spaceLayout.getTopMargin() < minThreshold) {
            ++minMarginCount;
        }
        if (this.spaceLayout.getRightMargin() < minThreshold) {
            ++minMarginCount;
        }
        if (this.spaceLayout.getBottomMargin() < minThreshold) {
            ++minMarginCount;
        }
        if (this.spaceLayout.getLeftMargin() < minThreshold) {
            ++minMarginCount;
        }

        if (this.spaceLayout.getTopMargin() > maxThreshold) {
            ++maxMarginCount;
        }
        if (this.spaceLayout.getRightMargin() > maxThreshold) {
            ++maxMarginCount;
        }
        if (this.spaceLayout.getBottomMargin() > maxThreshold) {
            ++maxMarginCount;
        }
        if (this.spaceLayout.getLeftMargin() > maxThreshold) {
            ++maxMarginCount;
        }

        if (minMarginCount > 1) {
            // 达到边缘
            String desc = "整个画幅结构达到画布边缘";
            result.addFeature(desc, Term.EnvironmentalDependence, Tendency.Positive, PerceptronThing.createPictureLayout());

            result.addScore(Indicator.Independence, 1, FloatUtils.random(0.2, 0.3));

            result.addFiveFactor(BigFiveFactor.Obligingness, FloatUtils.random(5.5, 6.0));
            result.addFiveFactor(BigFiveFactor.Conscientiousness, FloatUtils.random(7.0, 7.5));
            result.addFiveFactor(BigFiveFactor.Neuroticism, FloatUtils.random(2.0, 2.5));
            if (printBigFive) {
                System.out.println("CP-030");
            }
        }

        if (maxMarginCount > 1) {
            // 未达边缘
            String desc = "整个画幅结构未达到画布边缘";
            result.addFeature(desc, Term.EnvironmentalAlienation, Tendency.Positive, PerceptronThing.createPictureLayout());

            result.addScore(Indicator.Independence, -1, FloatUtils.random(0.2, 0.3));

            result.addFiveFactor(BigFiveFactor.Obligingness, FloatUtils.random(5.0, 6.0));
            result.addFiveFactor(BigFiveFactor.Conscientiousness, FloatUtils.random(8.5, 9.5));
            result.addFiveFactor(BigFiveFactor.Achievement, FloatUtils.random(5.0, 5.5));
            result.addFiveFactor(BigFiveFactor.Neuroticism, FloatUtils.random(2.0, 2.5));
            if (printBigFive) {
                System.out.println("CP-031");
            }
        }

        if (minMarginCount >= 3) {
            String desc = "整个画幅结构达到画布边缘";
            result.addFeature(desc, Term.PursuitOfAchievement, Tendency.Positive, PerceptronThing.createPictureLayout());

            result.addFiveFactor(BigFiveFactor.Obligingness, FloatUtils.random(7.5, 8.0));
            result.addFiveFactor(BigFiveFactor.Conscientiousness, FloatUtils.random(5.0, 5.5));
            result.addFiveFactor(BigFiveFactor.Achievement, FloatUtils.random(8.0, 8.5));
            if (printBigFive) {
                System.out.println("CP-097");
            }
        }
        else if (minMarginCount >= 2) {
            String desc = "整个画幅结构达到画布边缘";
            result.addFeature(desc, Term.PursuitOfAchievement, Tendency.Positive, PerceptronThing.createPictureLayout());

            result.addFiveFactor(BigFiveFactor.Obligingness, FloatUtils.random(7.5, 8.0));
            result.addFiveFactor(BigFiveFactor.Conscientiousness, FloatUtils.random(5.0, 5.5));
            result.addFiveFactor(BigFiveFactor.Extraversion, FloatUtils.random(8.0, 8.5));
            result.addFiveFactor(BigFiveFactor.Achievement, FloatUtils.random(8.0, 8.5));
            result.addFiveFactor(BigFiveFactor.Neuroticism, FloatUtils.random(3.0, 3.5));
            if (printBigFive) {
                System.out.println("CP-032");
            }
        }

        // 房、树、人各自的大小
        House house = this.painting.getHouse();
        Tree tree = this.painting.getTree();
        Person person = this.painting.getPerson();

        double hAreaRatio = (null != house) ? (double)house.area / (double)this.spaceLayout.getPaintingArea() : -1;
        double tAreaRatio = (null != tree) ? (double)tree.area / (double)this.spaceLayout.getPaintingArea() : -1;
        double pAreaRatio = (null != person) ? (double)person.area / (double)this.spaceLayout.getPaintingArea() : -1;

        Logger.d(this.getClass(), "#evalFrameStructure - Area ratio: " + hAreaRatio +
                "/" + tAreaRatio + "/" + pAreaRatio);

        // 左半边中线位置
        int halfLeftCenterX = (int) Math.round(this.canvasSize.width * 0.5 * 0.5);
        // 右半边中线位置
        int halfRightCenterX = this.canvasSize.width - halfLeftCenterX;

        if (null != house) {
            int cX = (int) Math.round(house.boundingBox.x + house.boundingBox.width * 0.5);
            if (cX < halfLeftCenterX) {
                // house 中线越过左半边中线
                String desc = "画面中房元素穿越左半边中线";
                result.addFeature(desc, Term.PayAttentionToFamily, Tendency.Positive,
                        PerceptronThing.createPictureLayout(new Thing[] { house }));
                result.addScore(Indicator.Family, 1, FloatUtils.random(0.1, 0.2));
            }
            else if (cX > halfRightCenterX) {
                // house 中线越过右半边中线
                String desc = "画面中房元素穿越右半边中线";
                result.addFeature(desc, Term.PayAttentionToFamily, Tendency.Negative,
                        PerceptronThing.createPictureLayout(new Thing[] { house }));
                result.addScore(Indicator.Family, -1, FloatUtils.random(0.1, 0.2));
            }

            if (hAreaRatio < this.houseAreaRatioThreshold) {
                // 房的面积非常小
                String desc = "画面中房元素整体占比非常小";
                result.addFeature(desc, Term.PayAttentionToFamily, Tendency.Negative,
                        PerceptronThing.createThingSize(new Thing[] { house }));

                result.addScore(Indicator.Family, -1, FloatUtils.random(0.3, 0.4));
                result.addScore(Indicator.Depression, 1, FloatUtils.random(0.5, 0.6));

                System.out.println("DEBUG: Depression - 画面中房元素整体占比非常小");

                result.addFiveFactor(BigFiveFactor.Obligingness, FloatUtils.random(1.0, 2.0));
                result.addFiveFactor(BigFiveFactor.Conscientiousness, FloatUtils.random(6.5, 7.0));
                result.addFiveFactor(BigFiveFactor.Neuroticism, FloatUtils.random(2.0, 3.0));
                if (printBigFive) {
                    System.out.println("CP-033");
                }

                if (tAreaRatio > 0 && tAreaRatio < this.treeAreaRatioThreshold) {
                    this.reference = Reference.Abnormal;
                    Logger.d(this.getClass(), "#evalFrameStructure - Abnormal: 房面积非常小，树面积非常小");
                    desc = "画面中房元素和树元素整体占比非常小";
                    result.addFeature(desc, Term.MentalStress, Tendency.Positive,
                            PerceptronThing.createThingSize(new Thing[] { house }));
                }
                else if (pAreaRatio > 0 && pAreaRatio <= this.personAreaRatioThreshold) {
                    this.reference = Reference.Abnormal;
                    Logger.d(this.getClass(), "#evalFrameStructure - Abnormal: 房面积非常小，人面积非常小");
                    desc = "画面中房元素和人元素整体占比非常小";
                    result.addFeature(desc, Term.MentalStress, Tendency.Positive,
                            PerceptronThing.createThingSize(new Thing[] { house }));
                }
            }
            else if (hAreaRatio < this.houseAreaRatioThreshold * 2) {
                result.addFiveFactor(BigFiveFactor.Achievement, FloatUtils.random(4.0, 4.5));
                if (printBigFive) {
                    System.out.println("CP-034");
                }
            }
            else if (hAreaRatio > 0.3d) {
                String desc = "画面中房元素占比较大";
                result.addFeature(desc, Term.SelfExistence, Tendency.Positive,
                        PerceptronThing.createThingSize(new Thing[] { house }));

                result.addFiveFactor(BigFiveFactor.Achievement, FloatUtils.random(8.5, 9.0));
                if (printBigFive) {
                    System.out.println("CP-095");
                }
            }
            else if (hAreaRatio > 0.2d) {
                result.addFiveFactor(BigFiveFactor.Obligingness, FloatUtils.random(5.0, 5.5));
                result.addFiveFactor(BigFiveFactor.Extraversion, FloatUtils.random(2.0, 2.5));
                result.addFiveFactor(BigFiveFactor.Achievement, FloatUtils.random(8.0, 8.5));
                result.addFiveFactor(BigFiveFactor.Neuroticism, FloatUtils.random(7.0, 7.5));
                if (printBigFive) {
                    System.out.println("CP-096");
                }
            }
            else {
                result.addFiveFactor(BigFiveFactor.Obligingness, FloatUtils.random(5.0, 5.5));
                result.addFiveFactor(BigFiveFactor.Conscientiousness, FloatUtils.random(6.5, 7.5));
                result.addFiveFactor(BigFiveFactor.Achievement, FloatUtils.random(8.0, 9.0));
                result.addFiveFactor(BigFiveFactor.Extraversion, FloatUtils.random(8.0, 9.0));
                result.addFiveFactor(BigFiveFactor.Neuroticism, FloatUtils.random(4.5, 5.5));
                if (printBigFive) {
                    System.out.println("CP-035");
                }
            }
        }

        if (null != tree) {
            if (tAreaRatio < this.treeAreaRatioThreshold) {
                // 树的面积非常小
                String desc = "画面中树的整理比例非常小";
                result.addFeature(desc, Term.Introversion, Tendency.Positive,
                        PerceptronThing.createThingSize(new Thing[] { tree }));
                result.addFeature(desc, Term.Depression, Tendency.Positive,
                        PerceptronThing.createThingSize(new Thing[] { tree }));

                result.addFiveFactor(BigFiveFactor.Obligingness, FloatUtils.random(5.0, 5.5));
                result.addFiveFactor(BigFiveFactor.Conscientiousness, FloatUtils.random(2.5, 3.5));
                result.addFiveFactor(BigFiveFactor.Neuroticism, FloatUtils.random(2.5, 3.0));
                if (printBigFive) {
                    System.out.println("CP-036");
                }
            }
        }

        if (null != person) {
            if (pAreaRatio < this.personAreaRatioThreshold) {
                // 人的面积非常小
                String desc = "画面中人的整理比例非常小";
                result.addFeature(desc, Term.Depression, Tendency.Positive,
                        PerceptronThing.createThingSize(new Thing[] { person }));
                result.addFeature(desc, Term.SenseOfSecurity, Tendency.Negative,
                        PerceptronThing.createThingSize(new Thing[] { person }));

                result.addScore(Indicator.Depression, 1, FloatUtils.random(0.4, 0.5));
                result.addScore(Indicator.SenseOfSecurity, -1, FloatUtils.random(0.2, 0.3));
                result.addScore(Indicator.Introversion, 1, FloatUtils.random(0.3, 0.4));

                System.out.println("DEBUG: Depression - " + desc);

                result.addFiveFactor(BigFiveFactor.Conscientiousness, FloatUtils.random(4.0, 5.0));
                result.addFiveFactor(BigFiveFactor.Extraversion, FloatUtils.random(2.0, 3.0));
                result.addFiveFactor(BigFiveFactor.Achievement, FloatUtils.random(4.0, 4.5));
                if (printBigFive) {
                    System.out.println("CP-037");
                }
            }
            else if (pAreaRatio > 0.09) {
                // 人的面积非常大
                String desc = "画面中人的整理比例非常大";
                result.addFeature(desc, Term.SelfConfidence, Tendency.Positive,
                        PerceptronThing.createThingSize(new Thing[] { person }));
                result.addFeature(desc, Term.SelfInflated, Tendency.Positive,
                        PerceptronThing.createThingSize(new Thing[] { person }));
                result.addFeature(desc, Term.Aggression, Tendency.Positive,
                        PerceptronThing.createThingSize(new Thing[] { person }));

                result.addScore(Indicator.Aggression, 1, FloatUtils.random(0.1, 0.2));
                result.addScore(Indicator.Extroversion, 1, FloatUtils.random(0.5, 0.6));

                result.addFiveFactor(BigFiveFactor.Extraversion, FloatUtils.random(8.5, 9.5));
                if (printBigFive) {
                    System.out.println("CP-038");
                }
            }
        }

        if (pAreaRatio > 0 && pAreaRatio <= this.personAreaRatioThreshold
                && tAreaRatio > 0 && tAreaRatio < this.treeAreaRatioThreshold) {
            // 偏模
            this.reference = Reference.Abnormal;
            Logger.d(this.getClass(), "#evalFrameStructure - Abnormal: 人面积非常小，树面积非常小");
        }
        else if (hAreaRatio > 0 && hAreaRatio < this.houseAreaRatioThreshold
                && tAreaRatio > 0 && tAreaRatio < this.treeAreaRatioThreshold
                && pAreaRatio > 0 && pAreaRatio < this.personAreaRatioThreshold) {
            // 偏模
            this.reference = Reference.Abnormal;
            Logger.d(this.getClass(), "#evalFrameStructure - Abnormal: 房面积非常小，人面积非常小，树面积非常小");
        }

        // 判断画面对称性

        boolean symmetry = this.calcSymmetry(this.painting.getTrees());
        if (symmetry) {
            result.addScore(Indicator.Obsession, 1, FloatUtils.random(0.6, 0.7));
        }

        symmetry = this.calcSymmetry(this.painting.getHouses());
        if (symmetry) {
            result.addScore(Indicator.Obsession, 1, FloatUtils.random(0.6, 0.7));
        }

        symmetry = this.calcSymmetry(this.painting.getPersons());
        if (symmetry) {
            result.addScore(Indicator.Obsession, 1, FloatUtils.random(0.6, 0.7));
        }

        return result;
    }

    private EvaluationFeature evalHouse() {
        EvaluationFeature result = new EvaluationFeature();
        if (null == this.painting.getHouses()) {
            return result;
        }

        List<House> houseList = this.painting.getHouses();
        for (House house : houseList) {
            // 立体感
            if (house.hasSidewall()) {
                String desc = "房有立体感";
                result.addFeature(desc, Term.Creativity, Tendency.Positive, new Thing[] { house });

                result.addScore(Indicator.Creativity, 1, FloatUtils.random(0.7, 0.8));
            }

            // 房屋类型
            if (Label.Bungalow == house.getLabel()) {
                // 平房
                String desc = "房元素是平房造型";
                result.addFeature(desc, Term.Simple, Tendency.Positive, new Thing[] { house });

                result.addScore(Indicator.Simple, 1, FloatUtils.random(0.5, 0.6));
            }
            else if (Label.Villa == house.getLabel()) {
                // 别墅
                String desc = "房元素是别墅造型";
                result.addFeature(desc, Term.Luxurious, Tendency.Positive, new Thing[] { house });

                result.addScore(Indicator.EvaluationFromOutside, 1, FloatUtils.random(0.5, 0.6));
            }
            else if (Label.Building == house.getLabel()) {
                // 楼房
                String desc = "房元素是楼房造型";
                result.addFeature(desc, Term.PursuitOfAchievement, Tendency.Positive, new Thing[] { house });
                result.addFeature(desc, Term.Defensiveness, Tendency.Positive, new Thing[] { house });

                result.addScore(Indicator.Realism, 1, FloatUtils.random(0.5, 0.6));
            }
            else if (Label.Fairyland == house.getLabel()) {
                // 童话房
                String desc = "房元素是童话房造型";
                result.addFeature(desc, Term.Fantasy, Tendency.Positive, new Thing[] { house });
                result.addFeature(desc, Term.Childish, Tendency.Normal, new Thing[] { house });

                result.addScore(Indicator.Idealism, 1, FloatUtils.random(0.5, 0.6));
            }
            else if (Label.Temple == house.getLabel()) {
                // 庙宇
                String desc = "房元素是庙宇造型";
                result.addFeature(desc, Term.Extreme, Tendency.Positive, new Thing[] { house });

                result.addScore(Indicator.Paranoid, 1, FloatUtils.random(0.5, 0.6));
            }
            else if (Label.Grave == house.getLabel()) {
                // 坟墓
                String desc = "房元素是坟墓造型";
                result.addFeature(desc, Term.WorldWeariness, Tendency.Positive, new Thing[] { house });

                result.addScore(Indicator.Depression, 1, FloatUtils.random(0.5, 0.6));
                System.out.println("DEBUG: Depression - " + desc);
            }

            // 房顶
            if (house.hasRoof()) {
                if (house.getRoof().isTextured()) {
                    String desc = "房屋的房顶绘制了纹理或装饰";
                    result.addFeature(desc, Term.Perfectionism, Tendency.Normal, new Thing[] { house.getRoof() });

                    result.addScore(Indicator.Obsession, 1, FloatUtils.random(0.6, 0.7));
                }

                if (house.getRoofHeightRatio() > 0.5f) {
                    // 房顶高
                    String desc = "房屋的房顶较高";
                    result.addFeature(desc, Term.Complacent, Tendency.Positive, new Thing[] { house.getRoof() });
                    result.addFeature(desc, Term.Future, Tendency.Positive, new Thing[] { house.getRoof() });

                    result.addScore(Indicator.AchievementMotivation, 1, FloatUtils.random(0.6, 0.7));

                    result.addFiveFactor(BigFiveFactor.Conscientiousness, FloatUtils.random(8.0, 8.5));
                    result.addFiveFactor(BigFiveFactor.Extraversion, FloatUtils.random(8.0, 8.5));
                    result.addFiveFactor(BigFiveFactor.Achievement, FloatUtils.random(8.0, 8.5));
                    if (printBigFive) {
                        System.out.println("CP-039");
                    }
                }

                if (house.getRoofAreaRatio() > 0.3f) {
                    // 房顶面积大
                    String desc = "房屋的房顶面积较大";
                    result.addFeature(desc, Term.MentalStress, Tendency.Positive, new Thing[] { house.getRoof() });
                    result.addFeature(desc, Term.Escapism, Tendency.Positive, new Thing[] { house.getRoof() });

                    result.addScore(Indicator.Stress, 1, FloatUtils.random(0.4, 0.5));
                }
            }

            // 天窗
            if (house.hasRoofSkylight()) {
                String desc = "房屋有天窗";
                result.addFeature(desc, Term.Maverick, Tendency.Positive, new Thing[] { house.getRoofSkylights().get(0) });

                result.addScore(Indicator.Independence, 1, FloatUtils.random(0.6, 0.7));

                result.addFiveFactor(BigFiveFactor.Extraversion, FloatUtils.random(8.0, 8.5));
                result.addFiveFactor(BigFiveFactor.Achievement, FloatUtils.random(8.0, 8.5));
                result.addFiveFactor(BigFiveFactor.Neuroticism, FloatUtils.random(8.0, 8.5));
                if (printBigFive) {
                    System.out.println("CP-040");
                }
            }

            // 烟囱
            if (house.hasChimney()) {
                String desc = "房屋有烟囱";
                result.addFeature(desc, Term.PursueInterpersonalRelationships, Tendency.Positive, new Thing[] {
                        house.getChimneys().get(0)
                });

                result.addScore(Indicator.InterpersonalRelation, 1, FloatUtils.random(0.6, 0.7));

                result.addFiveFactor(BigFiveFactor.Obligingness, FloatUtils.random(4.0, 5.0));
                result.addFiveFactor(BigFiveFactor.Conscientiousness, FloatUtils.random(4.0, 5.0));
                result.addFiveFactor(BigFiveFactor.Extraversion, FloatUtils.random(5.0, 5.5));
                result.addFiveFactor(BigFiveFactor.Neuroticism, FloatUtils.random(5.5, 6.0));
                if (printBigFive) {
                    System.out.println("CP-041");
                }
            }

            // 门和窗
            if (!house.hasDoor() && !house.hasWindow()) {
                String desc = "房屋没有门和窗";
                result.addFeature(desc, Term.WillingnessToCommunicate, Tendency.Negative, new Thing[] { house });
                result.addScore(Indicator.Family, -1, FloatUtils.random(0.3, 0.4));
            }
            else {
                result.addScore(Indicator.Family, 1, FloatUtils.random(0.4, 0.5));

                if (house.hasDoor()) {
                    result.addScore(Indicator.InterpersonalRelation, 1, FloatUtils.random(0.6, 0.7));

                    double areaRatio = house.getMaxDoorAreaRatio();
                    if (areaRatio < 0.029f) {
                        String desc = "房屋的门很小";
                        result.addFeature(desc, Term.SocialPowerlessness, Tendency.Positive, new Thing[] {
                                house.getDoors().get(0)
                        });
                    }
                    else if (areaRatio >= 0.15f) {
                        String desc = "房屋的门很大";
                        result.addFeature(desc, Term.Dependence, Tendency.Positive, new Thing[] {
                                house.getDoors().get(0)
                        });
                    }
                    else if (areaRatio > 0.12f) {
                        String desc = "房屋的门较大";
                        result.addFeature(desc, Term.PursueInterpersonalRelationships, Tendency.Positive, new Thing[] {
                                house.getDoors().get(0)
                        });
                    }
                    else {
                        String desc = "房屋有门";
                        result.addFeature(desc, Term.PursueInterpersonalRelationships, Tendency.Positive, new Thing[] {
                                house.getDoors().get(0)
                        });
                    }

                    // 开启的门
                    if (house.hasOpenDoor()) {
                        String desc = "房屋的门是打开的";
                        result.addFeature(desc, Term.Extroversion, Tendency.Positive, new Thing[] {
                                house.getDoors().get(0)
                        });

                        result.addScore(Indicator.Extroversion, 1, FloatUtils.random(0.6, 0.7));

                        result.addFiveFactor(BigFiveFactor.Extraversion, FloatUtils.random(6.5, 7.5));
                        if (printBigFive) {
                            System.out.println("CP-042");
                        }
                    }
                }

                if (house.hasWindow()) {
                    result.addScore(Indicator.InterpersonalRelation, 1, FloatUtils.random(0.6, 0.7));

                    double areaRatio = house.getMaxWindowAreaRatio();
                    if (areaRatio < 0.03f) {
                        String desc = "房屋的窗较小";
                        result.addFeature(desc, Term.SocialPowerlessness, Tendency.Positive, new Thing[] {
                                house.getWindows().get(0)
                        });
                    }
                    else if (areaRatio > 0.11f) {
                        String desc = "房屋的窗较大";
                        result.addFeature(desc, Term.PursueInterpersonalRelationships, Tendency.Positive, new Thing[] {
                                house.getWindows().get(0)
                        });
                    }

                    // 涂鸦计数
                    int countDoodle = 0;
                    for (Window window : house.getWindows()) {
                        Logger.d(this.getClass(), "#evalHouse - Window doodle:\n" + window.texture.toJSON().toString(4));
                        if (window.isDoodle()) {
                            ++countDoodle;
                        }
                    }

                    if (countDoodle >= 3) {
                        result.addScore(Indicator.Anxiety, 1, FloatUtils.random(0.3, 0.4));
                        result.addScore(Indicator.Family, -1, FloatUtils.random(0.3, 0.4));
                        System.out.println("DEBUG: Anxiety - House window - " + countDoodle);
                    }
                    else if (countDoodle >= 1) {
                        result.addScore(Indicator.Anxiety, 1, FloatUtils.random(0.2, 0.3));
                        result.addScore(Indicator.Family, -1, FloatUtils.random(0.2, 0.3));
                        System.out.println("DEBUG: Anxiety - House window - " + countDoodle);
                    }
                }

                // 计算总面积比例
                double areaRatio = house.getAllDoorsAndWindowsAreaRatio();
                if (areaRatio < 0.2) {
                    result.addScore(Indicator.InterpersonalRelation, -1, FloatUtils.random(0.5, 0.6));
                }
            }

            // 窗帘
            if (house.hasCurtain()) {
                String desc = "房屋有窗帘";
                result.addFeature(desc, Term.Sensitiveness, Tendency.Positive, new Thing[] {
                        house.getCurtains().get(0)
                });
                result.addFeature(desc, Term.Suspiciousness, Tendency.Positive);

                result.addScore(Indicator.InterpersonalRelation, 1, FloatUtils.random(0.6, 0.7));
            }

            // 小径
            if (house.hasPath()) {
                String desc = "房前有小径";
                result.addFeature(desc, Term.Straightforwardness, Tendency.Positive, new Thing[] {
                        house.getPaths().get(0)
                });

                if (house.hasCurvePath()) {
                    // 弯曲小径
                    desc = "房屋前是弯曲小径";
                    result.addFeature(desc, Term.Vigilance, Tendency.Positive, new Thing[] {
                            house.getPaths().get(0)
                    });
                }

                if (house.hasCobbledPath()) {
                    // 石头小径
                    desc = "房屋前是石头小径";
                    result.addFeature(desc, Term.Perfectionism, Tendency.Positive, new Thing[] {
                            house.getPaths().get(0)
                    });
                }
            }

            // 栅栏
            if (house.hasFence()) {
                String desc = "房屋周围有栅栏";
                result.addFeature(desc, Term.Defensiveness, Tendency.Positive, new Thing[] {
                        house.getFences().get(0)
                });
            }


            // 判断房屋是否涂鸦
            if (house.isDoodle()) {
                // 涂鸦的房子
                Logger.d(this.getClass(), "#evalHouse - House is doodle - " + house.texture.toJSON().toString(4));

                String desc = "房子有涂鸦痕迹";
                result.addFeature(desc, Term.Defensiveness, Tendency.Positive, new Thing[] { house });
                result.addFeature(desc, Term.Depression, Tendency.Positive, new Thing[] { house });
                result.addFeature(desc, Term.Anxiety, Tendency.Positive, new Thing[] { house });

                if (house.isDeepnessDoodle()) {
                    result.addScore(Indicator.Family, -1, FloatUtils.random(0.6, 0.7));
                    result.addScore(Indicator.Anxiety, 1, FloatUtils.random(0.7, 0.8));
                    // 重置抑郁
                    result.removeScores(Indicator.Depression);
                    result.addScore(Indicator.Depression, 1, FloatUtils.random(0.7, 0.8));
                    System.out.println("DEBUG: Depression - " + desc);
                }
                else {
                    result.addScore(Indicator.Family, -1, FloatUtils.random(0.3, 0.4));
                    result.addScore(Indicator.Anxiety, 1, FloatUtils.random(0.7, 0.8));
                }

                result.addFiveFactor(BigFiveFactor.Neuroticism, FloatUtils.random(7.5, 8.0));
                if (printBigFive) {
                    System.out.println("CP-043");
                }
            }
        }

        Logger.d(this.getClass(), "#evalHouse - Number of houses : " + houseList.size());
        if (houseList.size() > 1) {
            result.addScore(Indicator.Anxiety, -1, FloatUtils.random(0.3, 0.4));
            result.addScore(Indicator.Creativity, 1, FloatUtils.random(0.6, 0.7));
        }

        return result;
    }

    private EvaluationFeature evalTree() {
        EvaluationFeature result = new EvaluationFeature();
        if (null == this.painting.getTrees()) {
            return result;
        }

        // 整个画面里没有树干
        boolean hasTrunk = false;
        int countTreeDoodle = 0;
        int countCanopyDoodle = 0;

        List<Tree> treeList = this.painting.getTrees();
        for (Tree tree : treeList) {
            // 树类型
            if (Label.DeciduousTree == tree.getLabel()) {
                // 落叶树
                hasTrunk = true;

                String desc = "树类型疑似落叶树";
                result.addFeature(desc, Term.MentalStress, Tendency.Positive, new Thing[] { tree });
                result.addFeature(desc, Term.ExternalPressure, Tendency.Positive, new Thing[] { tree });

                result.addScore(Indicator.Stress, 1, FloatUtils.random(0.6, 0.7));
            }
            else if (Label.DeadTree == tree.getLabel()) {
                // 枯树
                hasTrunk = true;

                String desc = "树类型疑似枯树";
                result.addFeature(desc, Term.EmotionalDisturbance, Tendency.Positive, new Thing[] { tree });

                result.addScore(Indicator.Depression, 1, FloatUtils.random(0.6, 0.7));
                result.addScore(Indicator.Anxiety, 1, FloatUtils.random(0.6, 0.7));

                System.out.println("DEBUG: Depression - " + desc);

                result.addFiveFactor(BigFiveFactor.Neuroticism, FloatUtils.random(6.5, 7.5));
                if (printBigFive) {
                    System.out.println("CP-044");
                }

                Logger.d(this.getClass(), "#evalTree [Depression] : dead tree");
            }
            else if (Label.PineTree == tree.getLabel()) {
                // 松树
                hasTrunk = true;

                String desc = "树类型疑似松树";
                result.addFeature(desc, Term.SelfControl, Tendency.Positive, new Thing[] { tree });

                result.addScore(Indicator.AchievementMotivation, 1, FloatUtils.random(0.6, 0.7));
                result.addScore(Indicator.SelfControl, 1, FloatUtils.random(0.6, 0.7));

                result.addFiveFactor(BigFiveFactor.Conscientiousness, FloatUtils.random(7.0, 8.0));
                if (printBigFive) {
                    System.out.println("CP-045");
                }
            }
            else if (Label.WillowTree == tree.getLabel()) {
                // 柳树
                hasTrunk = true;

                String desc = "树类型疑似柳树";
                result.addFeature(desc, Term.Sensitiveness, Tendency.Positive, new Thing[] { tree });
                result.addFeature(desc, Term.Emotionality, Tendency.Positive, new Thing[] { tree });

                result.addScore(Indicator.Mood, 1, FloatUtils.random(0.6, 0.7));
            }
            else if (Label.CoconutTree == tree.getLabel()) {
                // 椰子树
                hasTrunk = true;

                String desc = "树类型疑似椰子树";
                result.addFeature(desc, Term.Emotionality, Tendency.Positive, new Thing[] { tree });
                result.addFeature(desc, Term.Creativity, Tendency.Positive, new Thing[] { tree });

                result.addScore(Indicator.Mood, 1, FloatUtils.random(0.6, 0.7));
                result.addScore(Indicator.Creativity, 1, FloatUtils.random(0.6, 0.7));
            }
            else if (Label.Bamboo == tree.getLabel()) {
                // 竹子
                hasTrunk = true;

                String desc = "树类型疑似竹子";
                result.addFeature(desc, Term.Independence, Tendency.Positive, new Thing[] { tree });

                result.addScore(Indicator.Thought, 1, FloatUtils.random(0.6, 0.7));
                result.addScore(Indicator.Independence, 1, FloatUtils.random(0.6, 0.7));

                result.addFiveFactor(BigFiveFactor.Achievement, FloatUtils.random(8.0, 9.0));
                if (printBigFive) {
                    System.out.println("CP-046");
                }
            }
            else {
                // 一般树
                String desc = "树类型疑似简洁树型";
                result.addFeature(desc, Term.Straightforwardness, Tendency.Positive, new Thing[] { tree });
            }

            // 树干
            if (tree.hasTrunk()) {
                hasTrunk = true;
                double ratio = tree.getTrunkWidthRatio();
                Logger.d(this.getClass(), "#evalTree - Tree trunk width ratio: " + ratio);
                if (ratio < 0.18d) {
                    // 细
                    String desc = "树的树干较细";
                    result.addFeature(desc, Term.Powerlessness, Tendency.Positive, new Thing[] {
                            tree.getTrunks().get(0)
                    });

                    result.addScore(Indicator.Depression, 1, FloatUtils.random(0.3, 0.4));
                    result.addScore(Indicator.SelfEsteem, -1, FloatUtils.random(0.3, 0.4));
                    result.addScore(Indicator.SocialAdaptability, -1, FloatUtils.random(0.3, 0.4));

                    System.out.println("DEBUG: Depression - " + desc);

                    result.addFiveFactor(BigFiveFactor.Obligingness, FloatUtils.random(1.5, 2.5));
                    if (printBigFive) {
                        System.out.println("CP-047");
                    }
                }
                else if (ratio >= 0.18d && ratio < 0.3d) {
                    // 粗细适度
                    String desc = "树的树干粗细适度";
                    result.addFeature(desc, Term.SelfEsteem, Tendency.Normal, new Thing[] {
                            tree.getTrunks().get(0)
                    });

                    result.addScore(Indicator.SelfEsteem, 1, FloatUtils.random(0.2, 0.3));
                    result.addScore(Indicator.Pessimism, -1, FloatUtils.random(0.3, 0.4));

                    result.addFiveFactor(BigFiveFactor.Conscientiousness, FloatUtils.random(5.0, 5.5));
                    result.addFiveFactor(BigFiveFactor.Extraversion, FloatUtils.random(7.0, 8.0));
                    result.addFiveFactor(BigFiveFactor.Achievement, FloatUtils.random(8.0, 9.0));
                    result.addFiveFactor(BigFiveFactor.Neuroticism, FloatUtils.random(4.0, 5.0));
                    if (printBigFive) {
                        System.out.println("CP-048");
                    }
                }
                else if (ratio >= 0.3d && ratio < 0.5d) {
                    // 粗
                    String desc = "树的树干略粗";
                    result.addFeature(desc, Term.EmotionalStability, Tendency.Positive, new Thing[] {
                            tree.getTrunks().get(0)
                    });

                    result.addScore(Indicator.Confidence, 1, FloatUtils.random(0.2, 0.3));
                    result.addScore(Indicator.Depression, -1, FloatUtils.random(0.2, 0.3));
                    Logger.d(this.getClass(), "#evalTree [Depression] : -1");

                    result.addFiveFactor(BigFiveFactor.Obligingness, FloatUtils.random(6.5, 7.5));
                    result.addFiveFactor(BigFiveFactor.Conscientiousness, FloatUtils.random(7.5, 8.0));
                    result.addFiveFactor(BigFiveFactor.Extraversion, FloatUtils.random(5.5, 6.0));
                    result.addFiveFactor(BigFiveFactor.Achievement, FloatUtils.random(8.5, 9.0));
                    result.addFiveFactor(BigFiveFactor.Neuroticism, FloatUtils.random(3.5, 4.0));
                    if (printBigFive) {
                        System.out.println("CP-049");
                    }
                }
                else if (ratio >= 0.5d && ratio < 0.7d) {
                    // 粗
                    String desc = "树的树干较粗";
                    result.addFeature(desc, Term.EmotionalStability, Tendency.Positive, new Thing[] {
                            tree.getTrunks().get(0)
                    });

                    result.addScore(Indicator.Confidence, 1, FloatUtils.random(0.2, 0.3));
                    result.addScore(Indicator.Depression, -1, FloatUtils.random(0.3, 0.4));
                    Logger.d(this.getClass(), "#evalTree [Depression] : -1");

                    result.addFiveFactor(BigFiveFactor.Obligingness, FloatUtils.random(6.5, 7.5));
                    result.addFiveFactor(BigFiveFactor.Conscientiousness, FloatUtils.random(7.5, 8.0));
                    result.addFiveFactor(BigFiveFactor.Neuroticism, FloatUtils.random(2.0, 3.0));
                    if (printBigFive) {
                        System.out.println("CP-093");
                    }
                }
                else {
                    // 很粗
                    String desc = "树的树干很粗";
                    result.addFeature(desc, Term.HighEnergy, Tendency.Positive, new Thing[] {
                            tree.getTrunks().get(0)
                    });
                    result.addFeature(desc, Term.EmotionalStability, Tendency.Positive, new Thing[] {
                            tree.getTrunks().get(0)
                    });

                    Score score = result.addScore(Indicator.Depression, -1, FloatUtils.random(0.6, 0.7));
                    Logger.d(this.getClass(), "#evalTree [Depression] : " + score.value);

                    result.addFiveFactor(BigFiveFactor.Neuroticism, FloatUtils.random(1.5, 2.5));
                    if (printBigFive) {
                        System.out.println("CP-050");
                    }
                }
            }

            // 树根
            if (tree.hasRoot()) {
                String desc = "树根可见";
                result.addFeature(desc, Term.Instinct, Tendency.Positive);

                result.addScore(Indicator.SelfControl, 1, FloatUtils.random(0.6, 0.7));
                result.addScore(Indicator.Paranoid, 1, FloatUtils.random(0.6, 0.7));
            }

            // 树洞
            if (tree.hasHole()) {
                String desc = "树洞可见";
                result.addFeature(desc, Term.Trauma, Tendency.Positive, new Thing[] {
                        tree.getHoles().get(0)
                });
                result.addFeature(desc, Term.EmotionalDisturbance, Tendency.Positive, new Thing[] {
                        tree.getHoles().get(0)
                });

                result.addScore(Indicator.Stress, 1, FloatUtils.random(0.6, 0.7));
                result.addScore(Indicator.SenseOfSecurity, -1, FloatUtils.random(0.1, 0.2));
            }

            // 树冠大小
            if (tree.hasCanopy()) {
                String desc = "树有树冠";
                result.addFeature(desc, Term.HighEnergy, Tendency.Positive, new Thing[] {
                        tree.getCanopies().get(0)
                });

                // 通过评估面积和高度确定树冠大小
                if (tree.getCanopyAreaRatio() >= 0.45) {
                    desc = "树的树冠较大";
                    result.addFeature(desc, Term.SocialDemand, Tendency.Positive, new Thing[] {
                            tree.getCanopies().get(0)
                    });

                    result.addScore(Indicator.Confidence, 1, FloatUtils.random(0.6, 0.7));
                    result.addScore(Indicator.InterpersonalRelation, 1, FloatUtils.random(0.6, 0.7));
                }
                else if (tree.getCanopyAreaRatio() < 0.2) {
                    desc = "树的树冠较小";
                    result.addFeature(desc, Term.SelfConfidence, Tendency.Negative, new Thing[] {
                            tree.getCanopies().get(0)
                    });

                    result.addScore(Indicator.Confidence, -1, FloatUtils.random(0.6, 0.7));
                    result.addScore(Indicator.InterpersonalRelation, -1, FloatUtils.random(0.6, 0.7));
                }

                if (tree.getCanopyHeightRatio() >= 0.33) {
                    desc = "树的树冠较高";
                    result.addFeature(desc, Term.SelfEsteem, Tendency.Positive, new Thing[] {
                            tree.getCanopies().get(0)
                    });

                    result.addScore(Indicator.Confidence, 1, FloatUtils.random(0.6, 0.7));
                    result.addScore(Indicator.InterpersonalRelation, 1, FloatUtils.random(0.6, 0.7));
                }
                else if (tree.getCanopyHeightRatio() < 0.2) {
                    desc = "树的树冠较矮";
                    result.addFeature(desc, Term.SelfEsteem, Tendency.Negative, new Thing[] {
                            tree.getCanopies().get(0)
                    });

                    result.addScore(Indicator.Confidence, -1, FloatUtils.random(0.6, 0.7));
                    result.addScore(Indicator.InterpersonalRelation, -1, FloatUtils.random(0.6, 0.7));
                }

                if (tree.getCanopyAreaRatio() < 0.2 && tree.getCanopyHeightRatio() < 0.3) {
                    desc = "树的树冠很小";
                    result.addFeature(desc, Term.Childish, Tendency.Positive, new Thing[] {
                            tree.getCanopies().get(0)
                    });
                }

                // 判断树冠涂鸦
                if (tree.isDoodleCanopy()) {
                    ++countCanopyDoodle;
                }
            }
            /* 树冠存在识别失败的可能性
            else {
                // 安全感缺失
                result.addFeature(Comment.SenseOfSecurity, Tendency.Negative);
                result.addScore(Indicator.SenseOfSecurity, -1, FloatUtils.random(0.6, 0.7));
            }*/

            // 果实
            if (tree.hasFruit()) {
                String desc = "树上有果实";
                result.addFeature(desc, Term.PursuitOfAchievement, Tendency.Positive, new Thing[] {
                        tree.getFruits().get(0)
                });

                result.addScore(Indicator.AchievementMotivation, 1, FloatUtils.random(0.6, 0.7));

                result.addFiveFactor(BigFiveFactor.Conscientiousness, FloatUtils.random(7.0, 8.0));
                result.addFiveFactor(BigFiveFactor.Achievement, FloatUtils.random(7.5, 8.0));
                result.addFiveFactor(BigFiveFactor.Neuroticism, FloatUtils.random(6.0, 7.0));
                if (printBigFive) {
                    System.out.println("CP-051");
                }

                double[] areaRatios = tree.getFruitAreaRatios();
                if (null != areaRatios) {
                    boolean many = areaRatios.length >= 3;
                    boolean big = false;
                    // 判断大小
                    for (double ratio : areaRatios) {
                        if (ratio >= 0.02) {
                            big = true;
                        }
                    }

                    if (big && many) {
                        // 大而多
                        desc = "树上的果实多而且大";
                        result.addFeature(desc, Term.ManyGoals, Tendency.Positive, new Thing[] {
                                tree.getFruits().get(0)
                        });
                        result.addFeature(desc, Term.ManyDesires, Tendency.Positive, new Thing[] {
                                tree.getFruits().get(0)
                        });
                        result.addFeature(desc, Term.SelfConfidence, Tendency.Positive, new Thing[] {
                                tree.getFruits().get(0)
                        });

                        result.addScore(Indicator.Struggle, 1, FloatUtils.random(0.5, 0.6));
                    }
                    else if (big) {
                        desc = "树上的果实大";
                        result.addFeature(desc, Term.ManyGoals, Tendency.Positive, new Thing[] {
                                tree.getFruits().get(0)
                        });
                    }
                    else if (many) {
                        desc = "树上的果实多";
                        result.addFeature(desc, Term.ManyGoals, Tendency.Positive, new Thing[] {
                                tree.getFruits().get(0)
                        });
                        result.addFeature(desc, Term.SelfConfidence, Tendency.Negative, new Thing[] {
                                tree.getFruits().get(0)
                        });

                        result.addScore(Indicator.AchievementMotivation, 1, FloatUtils.random(0.3, 0.4));
                    }
                    else {
                        desc = "树上的果实小";
                        result.addFeature(desc, Term.SelfConfidence, Tendency.Negative, new Thing[] {
                                tree.getFruits().get(0)
                        });

                        result.addScore(Indicator.Confidence, -1, FloatUtils.random(0.6, 0.7));
                    }
                }
            }

            // 水果数量
            if (tree.numFruits() >= 4 && tree.numFruits() < 8) {
                result.addScore(Indicator.Obsession, 1, FloatUtils.random(0.4, 0.5));
            }
            else if (tree.numFruits() >= 8) {
                result.addScore(Indicator.Obsession, 1, FloatUtils.random(0.6, 0.7));
            }

            // 判断树是否涂鸦
            if (tree.isDoodle()) {
                // 涂鸦的树
                Logger.d(this.getClass(), "#evalTree - Tree is doodle - \n" + tree.texture.toJSON().toString(4));

                ++countTreeDoodle;

                // 判断涂鸦树面积
                if ((double)tree.area / (double)this.spaceLayout.getPaintingArea() > 0.28d) {
                    String desc = "画面中的树有涂鸦效果且面积较大";
                    result.addFeature(desc, Term.Anxiety, Tendency.Positive, new Thing[] { tree });

                    result.addScore(Indicator.Anxiety, 1, FloatUtils.random(0.5, 0.6));
                }

                result.addScore(Indicator.Anxiety, 1, FloatUtils.random(0.55, 0.65));
                result.addScore(Indicator.Depression, 1, FloatUtils.random(0.1, 0.2));
                System.out.println("DEBUG: Depression - 树涂鸦");

                result.addFiveFactor(BigFiveFactor.Conscientiousness, FloatUtils.random(6.5, 7.0));
                result.addFiveFactor(BigFiveFactor.Achievement, FloatUtils.random(5.0, 5.5));
                result.addFiveFactor(BigFiveFactor.Neuroticism, FloatUtils.random(6.5, 7.0));
                if (printBigFive) {
                    System.out.println("CP-052");
                }
            }
        }

        if (countTreeDoodle >= 3) {
            result.addFiveFactor(BigFiveFactor.Obligingness, FloatUtils.random(9.0, 9.5));
            result.addFiveFactor(BigFiveFactor.Conscientiousness, FloatUtils.random(1.0, 1.5));
            result.addFiveFactor(BigFiveFactor.Neuroticism, FloatUtils.random(1.0, 1.5));
            if (printBigFive) {
                System.out.println("CP-091");
            }
        }
        else if (countTreeDoodle >= 2) {
            result.addFiveFactor(BigFiveFactor.Obligingness, FloatUtils.random(7.0, 7.5));
            result.addFiveFactor(BigFiveFactor.Conscientiousness, FloatUtils.random(2.0, 2.5));
            result.addFiveFactor(BigFiveFactor.Neuroticism, FloatUtils.random(2.0, 2.5));
            if (printBigFive) {
                System.out.println("CP-094");
            }
        }

        if (countCanopyDoodle >= 2 && countCanopyDoodle <= 4) {
            result.addScore(Indicator.Depression, 1, FloatUtils.random(0.1, 0.2));
            result.addScore(Indicator.Anxiety, 1, FloatUtils.random(0.5, 0.6));
            System.out.println("DEBUG: Depression - " + countCanopyDoodle + " 树冠涂鸦");
        }
        else if (countCanopyDoodle > 4) {
            result.addScore(Indicator.Depression, 1, FloatUtils.random(0.3, 0.4));
            result.addScore(Indicator.Anxiety, 1, FloatUtils.random(0.5, 0.6));
            System.out.println("DEBUG: Depression - " + countCanopyDoodle + " 树冠涂鸦");
        }


        if (!hasTrunk) {
            // 无树干
            String desc = "树疑似没有清晰树干";
            result.addFeature(desc, Term.SelfExistence, Tendency.Negative);

            result.addScore(Indicator.Confidence, 1, FloatUtils.random(0.3, 0.4));

            result.addFiveFactor(BigFiveFactor.Obligingness, FloatUtils.random(5.0, 5.5));
            result.addFiveFactor(BigFiveFactor.Conscientiousness, FloatUtils.random(4.0, 5.0));
            result.addFiveFactor(BigFiveFactor.Neuroticism, FloatUtils.random(6.0, 6.5));
            if (printBigFive) {
                System.out.println("CP-053");
            }
        }

        return result;
    }

    private EvaluationFeature evalPerson() {
        EvaluationFeature result = new EvaluationFeature();
        if (null == this.painting.getPersons()) {
            return result;
        }

        int numStickMan = 0;
        for (Person person : this.painting.getPersons()) {
            if (person instanceof StickMan) {
                // 火柴人
                ++numStickMan;

                if (numStickMan == 1) {
                    String desc = "人的形态疑似火柴人形态";
                    result.addFeature(desc, Term.Defensiveness, Tendency.Positive, new Thing[] { person });
                    result.addFeature(desc, Term.Creativity, Tendency.Negative, new Thing[] { person });

                    result.addScore(Indicator.Creativity, -1, FloatUtils.random(0.3, 0.4));
                }
            }
        }

        if (numStickMan >= 3) {
            result.addScore(Indicator.Obsession, 1, FloatUtils.random(0.5, 0.6));
        }

        if (numStickMan > 0) {
            // 人物没细节
            result.addFiveFactor(BigFiveFactor.Conscientiousness, FloatUtils.random(2.0, 3.0));

            if (printBigFive) {
                System.out.println("CP-054");
            }
        }

        // 人是否有足够细节
        boolean detailed = false;
        boolean faceDetailed = false;
        Person hitPerson = null;
        for (Person person : this.painting.getPersons()) {
            // 判断细节
            if (person.numComponents() >= 4) {
                hitPerson = person;
                detailed = true;
            }

            if (person.numFaceComponents() >= 3) {
                hitPerson = person;
                faceDetailed = true;
            }
        }

        if (detailed && faceDetailed) {
            String desc = "人的肢体有绘制细节";
            result.addFeature(desc, Term.SelfExistence, Tendency.Positive, new Thing[] { hitPerson });
            result.addFeature(desc, Term.Creativity, Tendency.Positive, new Thing[] { hitPerson });

            result.addFiveFactor(BigFiveFactor.Conscientiousness, FloatUtils.random(7.0, 8.0));
            if (printBigFive) {
                System.out.println("CP-055");
            }
        }
        else if (detailed || faceDetailed) {
            result.addFiveFactor(BigFiveFactor.Conscientiousness, FloatUtils.random(4.5, 6.5));
            result.addFiveFactor(BigFiveFactor.Achievement, FloatUtils.random(5.0, 5.5));
            if (printBigFive) {
                System.out.println("CP-056");
            }
        }
        else {
            String desc = "人物没有细节";
            result.addFeature(desc, Term.Creativity, Tendency.Normal);

            result.addFiveFactor(BigFiveFactor.Obligingness, FloatUtils.random(4.5, 5.0));
            result.addFiveFactor(BigFiveFactor.Conscientiousness, FloatUtils.random(2.0, 3.0));
            result.addFiveFactor(BigFiveFactor.Extraversion, FloatUtils.random(6.0, 6.5));
            result.addFiveFactor(BigFiveFactor.Achievement, FloatUtils.random(4.0, 4.5));
            result.addFiveFactor(BigFiveFactor.Neuroticism, FloatUtils.random(5.0, 6.0));
            if (printBigFive) {
                System.out.println("CP-057");
            }
        }

        for (Person person : this.painting.getPersons()) {
            // 性别
            if (person.getGender() == Person.Gender.Female) {
                if (this.painting.getAttribute().isMale()) {
                    // 男画女
                    String desc = "男性作者绘制女性人物";
                    result.addFeature(desc, Term.SocialPowerlessness, Tendency.Positive, new Thing[] { person });
                    result.addScore(Indicator.Mood, -1, FloatUtils.random(0.3, 0.4));
                    break;
                }
            }
            else if (person.getGender() == Person.Gender.Male) {
                if (this.painting.getAttribute().isFemale()) {
                    // 女画男
                    String desc = "女性作者绘制男性人物";
                    result.addFeature(desc, Term.SelfDemand, Tendency.Positive, new Thing[] { person });
                    result.addScore(Indicator.SelfConsciousness, 1, FloatUtils.random(0.3, 0.4));
                    break;
                }
            }
        }

        Person person = this.painting.getPerson();
        if (null != person) {
            // 头
            if (person.hasHead()) {
                // 头身比例
                Logger.d(this.getClass(), "#evalPerson - Head height ratio: " + person.getHeadHeightRatio());
                if (this.painting.getAttribute().age > 12) {
                    double hR = this.painting.getAttribute().age >= 18 ? 0.47d : 0.5d;
                    if (person.getHeadHeightRatio() > hR) {
                        // 头大
                        String desc = "";
                        result.addFeature(desc, Term.SocialAdaptability, Tendency.Negative, new Thing[] {
                                person.getHead()
                        });

                        result.addScore(Indicator.Impulsion, 1, FloatUtils.random(0.6, 0.7));
                        result.addScore(Indicator.SocialAdaptability, -1, FloatUtils.random(0.6, 0.7));

                        result.addFiveFactor(BigFiveFactor.Conscientiousness, FloatUtils.random(4.5, 5.0));
                        result.addFiveFactor(BigFiveFactor.Neuroticism, FloatUtils.random(8.0, 8.5));
                        if (printBigFive) {
                            System.out.println("CP-058");
                        }
                    }
                }
            }

            // 人物动态、静态判断方式：比较手臂和腿的边界盒形状，边界盒形状越相似则越接近静态，反之为动态。
            // TODO XJW

            // 五官是否完整
            if (!(person.hasEye() && person.hasNose() && person.hasMouth())) {
                // 不完整
                String desc = "人疑似没有细致的五官";
                result.addFeature(desc, Term.SelfConfidence, Tendency.Negative, new Thing[] { person });

                result.addScore(Indicator.Confidence, -1, FloatUtils.random(0.6, 0.7));

                /* FIXME XJW 不完整不能作为尽责性判断
                result.addFiveFactor(BigFivePersonality.Conscientiousness, FloatUtils.random(1.0, 2.0));
                */
            }
            else {
                result.addScore(Indicator.Introversion, 1, FloatUtils.random(0.6, 0.7));

                result.addFiveFactor(BigFiveFactor.Obligingness, FloatUtils.random(5.0, 5.5));
                result.addFiveFactor(BigFiveFactor.Extraversion, FloatUtils.random(7.0, 8.0));
                result.addFiveFactor(BigFiveFactor.Neuroticism, FloatUtils.random(7.0, 8.0));
                if (printBigFive) {
                    System.out.println("CP-059");
                }
            }

            // 眼
            if (person.hasEye()) {
                // 是否睁开眼
                if (!person.hasOpenEye()) {
                    String desc = "";
                    result.addFeature(desc, Term.Hostility, Tendency.Positive, new Thing[] {
                            person.getEyes().get(0)
                    });
                }

                double ratio = person.getMaxEyeAreaRatio();
                if (ratio > 0.018) {
                    // 眼睛大
                    String desc = "人物眼睛较大";
                    result.addFeature(desc, Term.Sensitiveness, Tendency.Positive, new Thing[] {
                            person.getEyes().get(0)
                    });
                    result.addFeature(desc, Term.Alertness, Tendency.Positive, new Thing[] {
                            person.getEyes().get(0)
                    });
                }
            }
            else {
                String desc = "人物疑似没有绘制眼睛";
                result.addFeature(desc, Term.IntrapsychicConflict, Tendency.Positive);
            }

            // 眉毛
            if (person.hasEyebrow()) {
                String desc = "人物有眉毛";
                result.addFeature(desc, Term.AttentionToDetail, Tendency.Positive, new Thing[] {
                        person.getEyebrows().get(0)
                });

                result.addScore(Indicator.Paranoid, 1, FloatUtils.random(0.6, 0.7));
            }

            // 嘴
            if (person.hasMouth()) {
                if (person.getMouth().isOpen()) {
                    String desc = "人物的最是张开的";
                    result.addFeature(desc, Term.LongingForMaternalLove, Tendency.Positive, new Thing[] {
                            person.getMouth()
                    });
                }
                else if (person.getMouth().isStraight()) {
                    String desc = "人物有嘴";
                    result.addFeature(desc, Term.Strong, Tendency.Positive, new Thing[] {
                            person.getMouth()
                    });

                    result.addScore(Indicator.Repression, 1, FloatUtils.random(0.6, 0.7));
                }
            }

            // 耳朵
            if (!person.hasEar()) {
                // 没有耳朵
                String desc = "人物没有耳朵";
                result.addFeature(desc, Term.Stubborn, Tendency.Positive);
            }

            // 头发
            if (person.hasHair()) {
                if (person.hasStraightHair()) {
                    // 直发
                    String desc = "人物是直发";
                    result.addFeature(desc, Term.Simple, Tendency.Positive, new Thing[] {
                            person.getHairs().get(0)
                    });

                    result.addScore(Indicator.Impulsion, 1, FloatUtils.random(0.6, 0.7));

                    result.addFiveFactor(BigFiveFactor.Conscientiousness, FloatUtils.random(1.0, 2.0));
                    if (printBigFive) {
                        System.out.println("CP-060");
                    }
                }
                else if (person.hasShortHair()) {
                    // 短发
                    String desc = "人物是短发";
                    result.addFeature(desc, Term.DesireForControl, Tendency.Positive, new Thing[] {
                            person.getHairs().get(0)
                    });

                    result.addScore(Indicator.Obsession, 1, FloatUtils.random(0.3, 0.4));

                    result.addFiveFactor(BigFiveFactor.Achievement, FloatUtils.random(8.5, 9.5));
                    if (printBigFive) {
                        System.out.println("CP-061");
                    }
                }
                else if (person.hasCurlyHair()) {
                    // 卷发
                    String desc = "人物是卷发";
                    result.addFeature(desc, Term.Sentimentality, Tendency.Positive, new Thing[] {
                            person.getHairs().get(0)
                    });

                    result.addScore(Indicator.Independence, 1, FloatUtils.random(0.6, 0.7));
                }
                else if (person.hasStandingHair()) {
                    // 竖直头发
                    String desc = "人物头发是疑似直线条";
                    result.addFeature(desc, Term.Aggression, Tendency.Positive, new Thing[] {
                            person.getHairs().get(0)
                    });

                    result.addScore(Indicator.Hostile, 1, FloatUtils.random(0.6, 0.7));

                    result.addFiveFactor(BigFiveFactor.Obligingness, FloatUtils.random(0.5, 1.5));
                    if (printBigFive) {
                        System.out.println("CP-062");
                    }
                }
                else {
                    // 有头发
                    result.addFiveFactor(BigFiveFactor.Conscientiousness, FloatUtils.random(5.5, 6.5));
                    result.addFiveFactor(BigFiveFactor.Extraversion, FloatUtils.random(5.0, 5.5));
                    result.addFiveFactor(BigFiveFactor.Neuroticism, FloatUtils.random(7.5, 8.5));

                    if (printBigFive) {
                        System.out.println("CP-063");
                    }
                }
            }

            // 发饰
            if (person.hasHairAccessory()) {
                String desc = "人物有发饰";
                result.addFeature(desc, Term.Narcissism, Tendency.Positive, new Thing[] {
                        person.getHairAccessories().get(0)
                });

                result.addScore(Indicator.Narcissism, 1, FloatUtils.random(0.6, 0.7));
            }

            // 帽子
            if (person.hasCap()) {
                String desc = "人物有帽子";
                result.addFeature(desc, Term.Powerlessness, Tendency.Positive, new Thing[] {
                        person.getCap()
                });

                result.addScore(Indicator.Repression, 1, FloatUtils.random(0.6, 0.7));
            }

            // 手臂
            if (person.hasTwoArms() && person.hasBody()) {
                // 计算手臂间距离相对于身体的宽度
                double d = person.calcArmsDistance();
                if (d > person.getBody().getWidth() * 0.5) {
                    // 手臂分开
                    String desc = "人物手臂分开";
                    result.addFeature(desc, Term.Extroversion, Tendency.Positive, new Thing[] {
                            person.getBody()
                    });
                }

                result.addScore(Indicator.EvaluationFromOutside, 1, FloatUtils.random(0.6, 0.7));
                result.addScore(Indicator.Paranoid, 1, FloatUtils.random(0.6, 0.7));

                result.addFiveFactor(BigFiveFactor.Conscientiousness, FloatUtils.random(8.0, 9.0));
                result.addFiveFactor(BigFiveFactor.Neuroticism, FloatUtils.random(8.0, 8.5));
                if (printBigFive) {
                    System.out.println("CP-064");
                }
            }

            // 腿
            if (person.hasTwoLegs()) {
                double d = person.calcLegsDistance();
                Leg thinLeg = person.getThinnestLeg();
                if (null != thinLeg) {
                    if (d > 5.0 && d < thinLeg.getWidth() * 0.5) {
                        // 腿的距离较近
                        String desc = "人物双腿分开";
                        result.addFeature(desc, Term.Cautious, Tendency.Positive, new Thing[] { thinLeg });
                        result.addFeature(desc, Term.Introversion, Tendency.Positive, new Thing[] { thinLeg });

                        result.addFiveFactor(BigFiveFactor.Extraversion, FloatUtils.random(4.0, 5.0));
                        if (printBigFive) {
                            System.out.println("CP-065");
                        }
                    }
                }

                result.addFiveFactor(BigFiveFactor.Conscientiousness, FloatUtils.random(7.5, 8.5));
                result.addFiveFactor(BigFiveFactor.Extraversion, FloatUtils.random(8.0, 8.5));
                if (printBigFive) {
                    System.out.println("CP-066");
                }
            }

            // 判断人是否涂鸦
            if (person.texture.max > 0 && person.texture.hierarchy > 0) {
                if (person.texture.max >= 1.0 && person.texture.max < 2.0) {
                    // 判断标准差和层密度
                    if (person.texture.standardDeviation >= 0.42 && person.texture.hierarchy <= 0.05) {
                        // 涂鸦的人
                        result.addScore(Indicator.Anxiety, 1, FloatUtils.random(0.6, 0.7));
                        Logger.d(this.getClass(), "#evalPerson - Person is doodle - \n" +
                                person.texture.toJSON().toString(4));

                        result.addFiveFactor(BigFiveFactor.Neuroticism, FloatUtils.random(5.5, 6.5));
                        if (printBigFive) {
                            System.out.println("CP-067");
                        }
                    }
                }
            }
//            if (person.isDoodle()) {
//                // 涂鸦的人
//                result.addScore(Indicator.Anxiety, 1, FloatUtils.random(0.6, 0.7));
//                Logger.d(this.getClass(), "#evalPerson - Person is doodle - \n" + person.doodle.toJSON().toString(4));
//            }
        }

        return result;
    }

    private EvaluationFeature evalOthers() {
        EvaluationFeature result = new EvaluationFeature();

        OtherSet other = this.painting.getOther();

        int counter = 0;

        if (other.has(Label.Table)) {
            // 桌子
            String desc = "画面中有桌子";
            result.addFeature(desc, Term.PayAttentionToFamily, Tendency.Positive, new Thing[] { other.get(Label.Table) });
            result.addScore(Indicator.Family, 1, FloatUtils.random(0.7, 0.8));
        }

        if (other.has(Label.Bed)) {
            // 床
            String desc = "画面中有床";
            result.addFeature(desc, Term.PayAttentionToFamily, Tendency.Positive, new Thing[] { other.get(Label.Bed) });
            result.addScore(Indicator.Family, 1, FloatUtils.random(0.7, 0.8));

            result.addFiveFactor(BigFiveFactor.Obligingness, FloatUtils.random(1.0, 2.5));
            if (printBigFive) {
                System.out.println("CP-068");
            }
        }

        if (other.has(Label.Sun)) {
            // 太阳
            String desc = "画面中有太阳";
            result.addFeature(desc, Term.PositiveExpectation, Tendency.Positive, new Thing[] { other.get(Label.Sun) });

            result.addScore(Indicator.Optimism, 1, FloatUtils.random(0.7, 0.8));
            result.addScore(Indicator.Depression, -1, FloatUtils.random(0.4, 0.5));

            if (other.get(Label.Sun).isDoodle()) {
                // 涂鸦的太阳
                Logger.d(this.getClass(), "#evalOthers - Sun is doodle - \n"
                        + other.get(Label.Sun).texture.toJSON().toString(4));
                result.addScore(Indicator.Depression, 1, FloatUtils.random(0.7, 0.8));

                result.addFiveFactor(BigFiveFactor.Obligingness, FloatUtils.random(4.0, 4.5));
                result.addFiveFactor(BigFiveFactor.Conscientiousness, FloatUtils.random(2.0, 3.0));
                result.addFiveFactor(BigFiveFactor.Extraversion, FloatUtils.random(2.0, 3.0));
                result.addFiveFactor(BigFiveFactor.Achievement, FloatUtils.random(3.0, 4.0));
                result.addFiveFactor(BigFiveFactor.Neuroticism, FloatUtils.random(8.0, 8.5));
                if (printBigFive) {
                    System.out.println("CP-092");
                }
            }
        }

        if (other.has(Label.Moon)) {
            // 月亮
            String desc = "画面中有月亮";
            result.addFeature(desc, Term.Sentimentality, Tendency.Positive, new Thing[] { other.get(Label.Moon) });
        }

        if (other.has(Label.Star)) {
            // 星星
            String desc = "画面中有星星";
            result.addFeature(desc, Term.Fantasy, Tendency.Positive, new Thing[] { other.get(Label.Star) });
        }

        if (other.has(Label.Mountain)) {
            // 山
            String desc = "画面中有山";
            result.addFeature(desc, Term.NeedProtection, Tendency.Positive, new Thing[] { other.get(Label.Mountain) });
            result.addScore(Indicator.SenseOfSecurity, -1, FloatUtils.random(0.3, 0.4));
        }

        if (other.has(Label.Flower)) {
            // 花
            String desc = "画面中有花";
            result.addFeature(desc, Term.Vanity, Tendency.Positive, new Thing[] { other.get(Label.Flower) });
            result.addScore(Indicator.EvaluationFromOutside, 1, FloatUtils.random(0.7, 0.8));
            counter += 1;
        }

        if (other.has(Label.Grass)) {
            // 草
            String desc = "画面中有草";
            result.addFeature(desc, Term.Stubborn, Tendency.Positive, new Thing[] { other.get(Label.Grass) });
        }

        if (other.has(Label.Sea)) {
            // 海
            String desc = "画面中有海";
            result.addFeature(desc, Term.DesireForFreedom, Tendency.Normal, new Thing[] { other.get(Label.Sea) });
        }

        if (other.has(Label.Pool)) {
            // 池塘
            String desc = "画面中有池塘";
            result.addFeature(desc, Term.Stubborn, Tendency.Normal, new Thing[] { other.get(Label.Pool) });
            result.addScore(Indicator.InterpersonalRelation, -1, FloatUtils.random(0.2, 0.3));
        }

        if (other.has(Label.Sunflower)) {
            // 向日葵
            String desc = "画面中有向日葵";
            result.addFeature(desc, Term.Extroversion, Tendency.Positive, new Thing[] { other.get(Label.Sunflower) });
            result.addFeature(desc, Term.PursuitOfAchievement, Tendency.Normal);
            result.addScore(Indicator.Extroversion, 1, FloatUtils.random(0.7, 0.8));
            result.addFiveFactor(BigFiveFactor.Extraversion, FloatUtils.random(8.5, 9.5));
            if (printBigFive) {
                System.out.println("CP-069");
            }
            counter += 1;
        }

        if (other.has(Label.Mushroom)) {
            // 蘑菇
            String desc = "画面中有蘑菇";
            result.addFeature(desc, Term.MentalStress, Tendency.Positive, new Thing[] { other.get(Label.Mushroom) });
            result.addScore(Indicator.Stress, 1, FloatUtils.random(0.7, 0.8));
            counter += 1;
        }

        if (other.has(Label.Lotus)) {
            // 莲花
            String desc = "画面中有莲花";
            result.addFeature(desc, Term.SelfInflated, Tendency.Positive, new Thing[] { other.get(Label.Lotus) });
            result.addFeature(desc, Term.Creativity, Tendency.Normal, new Thing[] { other.get(Label.Lotus) });
            result.addScore(Indicator.Confidence, 1, FloatUtils.random(0.2, 0.3));
            result.addScore(Indicator.MoralSense, 1, FloatUtils.random(0.7, 0.8));
            counter += 1;
        }

        if (other.has(Label.PlumFlower)) {
            // 梅花
            String desc = "画面中有梅花";
            result.addFeature(desc, Term.SelfEsteem, Tendency.Positive, new Thing[] { other.get(Label.PlumFlower) });
            result.addScore(Indicator.SelfEsteem, 1, FloatUtils.random(0.7, 0.8));
            counter += 1;
        }

        if (other.has(Label.Rose)) {
            // 玫瑰
            String desc = "画面中有玫瑰";
            result.addFeature(desc, Term.Creativity, Tendency.Positive, new Thing[] { other.get(Label.Rose) });
            result.addScore(Indicator.Creativity, 1, FloatUtils.random(0.7, 0.8));
            counter += 1;
        }

        if (other.has(Label.Cloud)) {
            // 云
            String desc = "画面中有云";
            result.addFeature(desc, Term.Imagination, Tendency.Positive, new Thing[] { other.get(Label.Cloud) });
            result.addScore(Indicator.Optimism, 1, FloatUtils.random(0.7, 0.8));
            result.addScore(Indicator.Idealism, 1, FloatUtils.random(0.7, 0.8));
        }

        if (other.has(Label.Rain)) {
            // 雨
            String desc = "画面中有雨";
            result.addFeature(desc, Term.MentalStress, Tendency.Positive, new Thing[] { other.get(Label.Rain) });
            result.addScore(Indicator.Stress, 1, FloatUtils.random(0.7, 0.8));
        }

        if (other.has(Label.Rainbow)) {
            // 彩虹
            String desc = "画面中有彩虹";
            result.addFeature(desc, Term.Future, Tendency.Positive, new Thing[] { other.get(Label.Rainbow) });
            result.addScore(Indicator.Simple, 1, FloatUtils.random(0.7, 0.8));
        }

        if (other.has(Label.Torch)) {
            // 火炬
            String desc = "画面中有火炬";
            result.addFeature(desc, Term.Hostility, Tendency.Positive, new Thing[] { other.get(Label.Torch) });

            result.addScore(Indicator.Hostile, 1, FloatUtils.random(0.3, 0.4));

            result.addFiveFactor(BigFiveFactor.Obligingness, FloatUtils.random(2.5, 3.5));
            if (printBigFive) {
                System.out.println("CP-070");
            }
        }

        if (other.has(Label.Bonfire)) {
            // 火堆
            String desc = "画面中有火堆";
            result.addFeature(desc, Term.Hostility, Tendency.Positive, new Thing[] { other.get(Label.Bonfire) });

            result.addScore(Indicator.Hostile, 1, FloatUtils.random(0.3, 0.4));

            result.addFiveFactor(BigFiveFactor.Obligingness, FloatUtils.random(2.5, 3.5));
            if (printBigFive) {
                System.out.println("CP-071");
            }
        }

        if (other.has(Label.Bird)) {
            // 鸟
            String desc = "画面中有鸟";
            result.addFeature(desc, Term.DesireForFreedom, Tendency.Positive, new Thing[] { other.get(Label.Bird) });
            result.addScore(Indicator.InterpersonalRelation, 1, FloatUtils.random(0.7, 0.8));
            result.addScore(Indicator.DesireForFreedom, 1, FloatUtils.random(0.7, 0.8));
            counter += 1;
        }

        if (other.has(Label.Cat)) {
            // 猫
            String desc = "画面中有猫";
            result.addFeature(desc, Term.SocialDemand, Tendency.Positive, new Thing[] { other.get(Label.Cat) });
            result.addScore(Indicator.Mood, 1, FloatUtils.random(0.7, 0.8));
            result.addScore(Indicator.InterpersonalRelation, 1, FloatUtils.random(0.3, 0.4));
            result.addScore(Indicator.Meekness, 1, FloatUtils.random(0.5, 0.6));
            counter += 1;
        }

        if (other.has(Label.Dog)) {
            // 狗
            String desc = "画面中有狗";
            result.addFeature(desc, Term.NeedProtection, Tendency.Positive, new Thing[] { other.get(Label.Dog) });
            result.addFeature(desc, Term.SenseOfSecurity, Tendency.Negative, new Thing[] { other.get(Label.Dog) });
            result.addScore(Indicator.SenseOfSecurity, -1, FloatUtils.random(0.3, 0.4));
            counter += 1;
        }

        if (other.has(Label.Cow)) {
            // 牛
            String desc = "画面中有牛";
            result.addFeature(desc, Term.Simple, Tendency.Positive, new Thing[] { other.get(Label.Cow) });

            result.addScore(Indicator.Struggle, 1, FloatUtils.random(0.7, 0.8));
            counter += 1;

            result.addFiveFactor(BigFiveFactor.Conscientiousness, FloatUtils.random(8.0, 9.5));
            if (printBigFive) {
                System.out.println("CP-072");
            }
        }

        if (other.has(Label.Sheep)) {
            // 羊
            String desc = "画面中有羊";
            result.addFeature(desc, Term.Creativity, Tendency.Positive, new Thing[] { other.get(Label.Sheep) });

            result.addScore(Indicator.Meekness, 1, FloatUtils.random(0.7, 0.8));
            counter += 1;
        }

        if (other.has(Label.Pig)) {
            // 猪
            String desc = "画面中有猪";
            result.addFeature(desc, Term.Creativity, Tendency.Positive, new Thing[] { other.get(Label.Pig) });

            result.addScore(Indicator.Meekness, 1, FloatUtils.random(0.5, 0.6));
            counter += 1;
        }

        if (other.has(Label.Fish)) {
            // 鱼
            String desc = "画面中有鱼";
            result.addFeature(desc, Term.DesireForFreedom, Tendency.Positive, new Thing[] { other.get(Label.Fish) });
            result.addScore(Indicator.InterpersonalRelation, 1, FloatUtils.random(0.5, 0.6));
            result.addScore(Indicator.DesireForFreedom, 1, FloatUtils.random(0.7, 0.8));
            counter += 1;
        }

        if (other.has(Label.Rabbit)) {
            // 兔
            String desc = "画面中有兔子";
            result.addFeature(desc, Term.Introversion, Tendency.Positive, new Thing[] { other.get(Label.Rabbit) });
            result.addScore(Indicator.Meekness, 1, FloatUtils.random(0.7, 0.8));
            counter += 1;
        }

        if (other.has(Label.Horse)) {
            // 马
            String desc = "画面中有马";
            result.addFeature(desc, Term.DesireForFreedom, Tendency.Positive, new Thing[] { other.get(Label.Horse) });
            result.addScore(Indicator.DesireForFreedom, 1, FloatUtils.random(0.7, 0.8));
            counter += 1;
        }

        if (other.has(Label.Hawk)) {
            // 鹰
            String desc = "画面中有鹰";
            result.addFeature(desc, Term.PursuitOfAchievement, Tendency.Positive, new Thing[] { other.get(Label.Hawk) });
            result.addScore(Indicator.Struggle, 1, FloatUtils.random(0.7, 0.8));
            result.addScore(Indicator.Creativity, 1, FloatUtils.random(0.7, 0.8));
            counter += 1;
        }

        if (other.has(Label.Rat)) {
            // 鼠
            String desc = "画面中有鼠";
            result.addFeature(desc, Term.Sensitiveness, Tendency.Positive, new Thing[] { other.get(Label.Rat) });
            result.addScore(Indicator.Confidence, -1, FloatUtils.random(0.7, 0.8));
            counter += 1;
        }

        if (other.has(Label.Butterfly)) {
            // 蝴蝶
            String desc = "画面中有蝴蝶";
            result.addFeature(desc, Term.PursuitOfAchievement, Tendency.Positive, new Thing[] { other.get(Label.Butterfly) });
        }

        if (other.has(Label.Tiger)) {
            // 虎
            String desc = "画面中有虎";
            result.addFeature(desc, Term.Extroversion, Tendency.Positive, new Thing[] { other.get(Label.Tiger) });
            result.addFeature(desc, Term.SelfConfidence, Tendency.Positive, new Thing[] { other.get(Label.Tiger) });
            result.addScore(Indicator.Extroversion, 1, FloatUtils.random(0.7, 0.8));
            result.addScore(Indicator.Confidence, 1, FloatUtils.random(0.5, 0.6));
            result.addFiveFactor(BigFiveFactor.Extraversion, FloatUtils.random(7.0, 8.0));
            if (printBigFive) {
                System.out.println("CP-073");
            }
            counter += 1;
        }

        if (other.has(Label.Hedgehog)) {
            // 刺猬
            String desc = "画面中有刺猬";
            result.addFeature(desc, Term.SenseOfSecurity, Tendency.Negative, new Thing[] { other.get(Label.Hedgehog) });
            result.addScore(Indicator.SenseOfSecurity, -1, FloatUtils.random(0.7, 0.8));
            counter += 1;
        }

        if (other.has(Label.Snake)) {
            // 蛇
            String desc = "画面中有蛇";
            result.addFeature(desc, Term.Trauma, Tendency.Positive, new Thing[] { other.get(Label.Snake) });
            result.addScore(Indicator.Mood, -1, FloatUtils.random(0.3, 0.4));
        }

        if (other.has(Label.Dragon)) {
            // 龙
            String desc = "画面中有龙";
            result.addFeature(desc, Term.PursuitOfAchievement, Tendency.Positive, new Thing[] { other.get(Label.Dragon) });
            result.addScore(Indicator.AchievementMotivation, 1, FloatUtils.random(0.7, 0.8));
            counter += 1;

            result.addFiveFactor(BigFiveFactor.Conscientiousness, FloatUtils.random(7.5, 8.5));
            if (printBigFive) {
                System.out.println("CP-074");
            }
        }

        if (other.has(Label.Watch)) {
            // 表
            String desc = "画面中有表";
            result.addFeature(desc, Term.Anxiety, Tendency.Positive, new Thing[] { other.get(Label.Watch) });
            result.addScore(Indicator.Anxiety, 1, FloatUtils.random(0.7, 0.8));

            result.addFiveFactor(BigFiveFactor.Neuroticism, FloatUtils.random(4.0, 5.0));
            if (printBigFive) {
                System.out.println("CP-075");
            }
        }

        if (other.has(Label.Clock)) {
            // 钟
            String desc = "画面中有时钟";
            result.addFeature(desc, Term.Anxiety, Tendency.Positive, new Thing[] { other.get(Label.Clock) });
            result.addScore(Indicator.Anxiety, 1, FloatUtils.random(0.7, 0.8));

            result.addFiveFactor(BigFiveFactor.Neuroticism, FloatUtils.random(4.0, 5.0));
            if (printBigFive) {
                System.out.println("CP-076");
            }
        }

        if (other.has(Label.MusicalNotation)) {
            // 音乐符号
            String desc = "画面中有音乐符号";
            result.addFeature(desc, Term.WorldWeariness, Tendency.Normal, new Thing[] { other.get(Label.MusicalNotation) });
            result.addScore(Indicator.Psychosis, 1, FloatUtils.random(0.1, 0.2));
        }

        if (other.has(Label.TV)) {
            // 电视
            String desc = "画面中有电视";
            result.addFeature(desc, Term.PayAttentionToFamily, Tendency.Negative, new Thing[] { other.get(Label.TV) });
            result.addScore(Indicator.Family, 1, FloatUtils.random(0.4, 0.5));
        }

        if (other.has(Label.Pole)) {
            // 电线杆
            String desc = "画面中有电线杆";
            result.addFeature(desc, Term.Stubborn, Tendency.Positive, new Thing[] { other.get(Label.Pole) });
        }

        if (other.has(Label.Tower)) {
            // 铁塔
            String desc = "画面中有铁塔";
            result.addFeature(desc, Term.Stereotype, Tendency.Positive, new Thing[] { other.get(Label.Tower) });
            result.addScore(Indicator.MoralSense, 1, FloatUtils.random(0.7, 0.8));
        }

        if (other.has(Label.Lighthouse)) {
            // 灯塔
            String desc = "画面中有灯塔";
            result.addFeature(desc, Term.Idealization, Tendency.Positive, new Thing[] { other.get(Label.Lighthouse) });
        }

        if (other.has(Label.Gun)) {
            // 枪
            String desc = "画面中有枪";
            result.addFeature(desc, Term.Aggression, Tendency.Positive, new Thing[] { other.get(Label.Gun) });
            result.addScore(Indicator.Aggression, 1, FloatUtils.random(0.7, 0.8));

            result.addFiveFactor(BigFiveFactor.Obligingness, FloatUtils.random(0.5, 1.5));
            if (printBigFive) {
                System.out.println("CP-077");
            }
        }

        if (other.has(Label.Sword)) {
            // 剑
            String desc = "画面中有剑";
            result.addFeature(desc, Term.Aggression, Tendency.Positive, new Thing[] { other.get(Label.Sword) });
            result.addFeature(desc, Term.Hostility, Tendency.Positive, new Thing[] { other.get(Label.Sword) });
            result.addScore(Indicator.Aggression, 1, FloatUtils.random(0.7, 0.8));
        }

        if (other.has(Label.Knife)) {
            // 刀
            String desc = "画面中有刀";
            result.addFeature(desc, Term.Aggression, Tendency.Positive, new Thing[] { other.get(Label.Knife) });
            result.addFeature(desc, Term.Hostility, Tendency.Positive, new Thing[] { other.get(Label.Knife) });
            result.addScore(Indicator.Aggression, 1, FloatUtils.random(0.7, 0.8));
        }

        if (other.has(Label.Shield)) {
            // 盾
            String desc = "画面中有盾";
            result.addFeature(desc, Term.Defensiveness, Tendency.Positive, new Thing[] { other.get(Label.Shield) });
            result.addFeature(desc, Term.NeedProtection, Tendency.Positive, new Thing[] { other.get(Label.Shield) });
            result.addScore(Indicator.Pessimism, 1, FloatUtils.random(0.7, 0.8));
        }

        if (other.has(Label.Sandglass)) {
            // 沙漏
            String desc = "画面中有沙漏";
            result.addFeature(desc, Term.Anxiety, Tendency.Positive, new Thing[] { other.get(Label.Sandglass) });
            result.addScore(Indicator.Anxiety, 1, FloatUtils.random(0.7, 0.8));
        }

        if (other.has(Label.Kite)) {
            // 风筝
            String desc = "画面中有风筝";
            result.addFeature(desc, Term.DesireForFreedom, Tendency.Positive, new Thing[] { other.get(Label.Kite) });
            result.addScore(Indicator.DesireForFreedom, 1, FloatUtils.random(0.7, 0.8));
            counter += 1;
        }

        if (other.has(Label.Umbrella)) {
            // 伞
            String desc = "画面中有伞";
            result.addFeature(desc, Term.SenseOfSecurity, Tendency.Negative, new Thing[] { other.get(Label.Umbrella) });
            result.addScore(Indicator.SenseOfSecurity, -1, FloatUtils.random(0.7, 0.8));
        }

        if (other.has(Label.Windmill)) {
            // 风车
            String desc = "画面中有风车";
            result.addFeature(desc, Term.Fantasy, Tendency.Positive, new Thing[] { other.get(Label.Windmill) });
            result.addScore(Indicator.Simple, 1, FloatUtils.random(0.7, 0.8));
            counter += 1;
        }

        if (other.has(Label.Flag)) {
            // 旗帜
            String desc = "画面中有旗帜";
            result.addFeature(desc, Term.PursuitOfAchievement, Tendency.Positive, new Thing[] { other.get(Label.Flag) });
            result.addScore(Indicator.MoralSense, 1, FloatUtils.random(0.7, 0.8));
        }

        if (other.has(Label.Bridge)) {
            // 桥
            String desc = "画面中有桥";
            result.addFeature(desc, Term.PursueInterpersonalRelationships, Tendency.Positive, new Thing[] { other.get(Label.Bridge) });
            result.addScore(Indicator.InterpersonalRelation, -1, FloatUtils.random(0.7, 0.8));
        }

        if (other.has(Label.Crossroads)) {
            // 十字路口
            String desc = "画面中有十字路口";
            result.addFeature(desc, Term.Anxiety, Tendency.Positive, new Thing[] { other.get(Label.Crossroads) });
            result.addScore(Indicator.Anxiety, 1, FloatUtils.random(0.5, 0.6));

            result.addFiveFactor(BigFiveFactor.Neuroticism, FloatUtils.random(7.0, 7.5));
            if (printBigFive) {
                System.out.println("CP-078");
            }
        }

        if (other.has(Label.Ladder)) {
            // 梯子
            String desc = "画面中有梯子";
            result.addFeature(desc, Term.PursuitOfAchievement, Tendency.Positive, new Thing[] { other.get(Label.Ladder) });
            result.addScore(Indicator.AchievementMotivation, 1, FloatUtils.random(0.7, 0.8));
            result.addScore(Indicator.Anxiety, 1, FloatUtils.random(0.5, 0.6));

            result.addFiveFactor(BigFiveFactor.Conscientiousness, FloatUtils.random(7.0, 9.0));
            if (printBigFive) {
                System.out.println("CP-079");
            }
        }

        if (other.has(Label.Stairs)) {
            // 楼梯
            String desc = "画面中有楼梯";
            result.addFeature(desc, Term.EnvironmentalAlienation, Tendency.Positive, new Thing[] { other.get(Label.Stairs) });
            result.addScore(Indicator.AchievementMotivation, 1, FloatUtils.random(0.7, 0.8));
            result.addScore(Indicator.Anxiety, 1, FloatUtils.random(0.5, 0.6));

            result.addFiveFactor(BigFiveFactor.Conscientiousness, FloatUtils.random(7.0, 9.0));
            if (printBigFive) {
                System.out.println("CP-080");
            }
        }

        if (other.has(Label.Birdcage)) {
            // 鸟笼
            String desc = "画面中有鸟笼";
            result.addFeature(desc, Term.DesireForFreedom, Tendency.Positive, new Thing[] { other.get(Label.Birdcage) });
            result.addScore(Indicator.DesireForFreedom, 1, FloatUtils.random(0.7, 0.8));
            counter += 1;
        }

        if (other.has(Label.Car)) {
            // 汽车
            String desc = "画面中有汽车";
            result.addFeature(desc, Term.Luxurious, Tendency.Positive, new Thing[] { other.get(Label.Car) });
        }

        if (other.has(Label.Boat)) {
            // 船
            String desc = "画面中有船";
            result.addFeature(desc, Term.DesireForFreedom, Tendency.Positive, new Thing[] { other.get(Label.Boat) });
            result.addScore(Indicator.DesireForFreedom, 1, FloatUtils.random(0.7, 0.8));
        }

        if (other.has(Label.Airplane)) {
            // 飞机
            String desc = "画面中有飞机";
            result.addFeature(desc, Term.Escapism, Tendency.Positive, new Thing[] { other.get(Label.Airplane) });
            result.addScore(Indicator.Independence, 1, FloatUtils.random(0.6, 0.7));
            result.addFiveFactor(BigFiveFactor.Achievement, FloatUtils.random(6.5, 7.5));
            if (printBigFive) {
                System.out.println("CP-081");
            }
        }

        if (other.has(Label.Bike)) {
            // 自行车
            String desc = "画面中有自行车";
            result.addFeature(desc, Term.EmotionalDisturbance, Tendency.Positive, new Thing[] { other.get(Label.Bike) });
            counter += 1;
        }

        if (other.has(Label.Skull)) {
            // 骷髅
            String desc = "画面中有骷髅";
            result.addFeature(desc, Term.WorldWeariness, Tendency.Positive, new Thing[] { other.get(Label.Skull) });
            result.addScore(Indicator.Psychosis, 1, FloatUtils.random(0.6, 0.7));
        }

        if (other.has(Label.Glasses)) {
            // 眼镜
            String desc = "画面中有眼镜";
            result.addFeature(desc, Term.Escapism, Tendency.Positive, new Thing[] { other.get(Label.Glasses) });
        }

        if (other.has(Label.Swing)) {
            // 秋千
            String desc = "画面中有秋千";
            result.addFeature(desc, Term.Childish, Tendency.Positive, new Thing[] { other.get(Label.Swing) });
        }

        if (counter >= 2) {
            String desc = "画面中有其他物品";
            result.addFeature(desc, Term.Creativity, Tendency.Positive);
            result.addScore(Indicator.Creativity, counter, FloatUtils.random(0.7, 0.8));
        }

        return result;
    }

    private List<EvaluationFeature> correct(List<EvaluationFeature> list) {
        // 如果有乐观和社会适应性，则社会适应性负分降分
        double optimism = 0;

//        int depressionValue = 0;
        int depressionCount = 0;
        double depression = 0;

        double anxiety = 0;
        double obsession = 0;

        if (this.painting.getAttribute().age < 18) {
            // 针对年龄的修订
            double w = 1d - Math.sin(this.painting.getAttribute().age * 0.041);
            for (EvaluationFeature ef : list) {
                List<Score> scores = ef.getScores(Indicator.Depression);
                for (Score score : scores) {
                    if (w <= 0) {
                        break;
                    }

                    if (score.value > 2) {
                        w = w - score.weight;
//                        ef.removeScore(score);
                        Logger.w(this.getClass(), "#correct - Depression -> Indicator.Stress: " + score.weight);
                        ef.addScore(Indicator.Stress, 1, score.weight);
                        break;
                    }
                }

                if (w <= 0) {
                    break;
                }
            }
        }

        for (EvaluationFeature ef : list) {
            // 计算"乐观"总分
            Score score = ef.getScore(Indicator.Optimism);
            if (null != score && score.value > 0) {
                optimism += score.weight;
            }

            // 统计"抑郁"
            List<Score> scores = ef.getScores(Indicator.Depression);
            for (Score s : scores) {
                depressionCount += 1;
//                depressionValue += s.value;
                if (s.value > 0) {
                    depression += s.weight;
                } else {
                    depression -= s.weight;
                }
            }

            scores = ef.getScores(Indicator.Anxiety);
            for (Score s : scores) {
                anxiety += s.value > 0 ? s.weight : -s.weight;
            }

            scores = ef.getScores(Indicator.Obsession);
            for (Score s : scores) {
                obsession += s.value > 0 ? s.weight : -s.weight;
            }
        }

        if (optimism > 0) {
            for (EvaluationFeature ef : list) {
                List<Score> scores = ef.getScores(Indicator.SocialAdaptability);
                for (Score score : scores) {
                    score.weight -= optimism * 0.677;
                    score.weight = Math.abs(score.weight);
                }
            }
        }

//        if (depressionValue < -2.0 && depressionCount == Math.abs(depressionValue)) {
//            for (EvaluationFeature ef : list) {
//                ef.update
//            }
//        }
//        else if ((depressionValue < 0 && depression > 0.5) ||
//                (depressionValue < 0 && depression > 0.3 && depressionCount >= 5)) {
//            // 增加一个权重负值
//            Logger.w(this.getClass(), "#correct - depression: " + depressionValue + " - " + depression);
//            list.get(list.size() - 1).addScore(Indicator.Depression, -1, FloatUtils.random(0.79, 0.88));
//        }
        if (depressionCount > 0) {
            if (this.painting.getAttribute().age >= 35) {
                list.get(list.size() - 1).addScore(Indicator.Depression, -1,
                        this.painting.getAttribute().age * FloatUtils.random(0.009, 0.011));
            }
        }

        // 按照年龄修订
        if (this.painting.getAttribute().age >= 35) {
            Score psychosis = null;
            for (EvaluationFeature ef : list) {
                for (Score s : ef.getScores()) {
                    if (s.indicator == Indicator.Psychosis) {
                        if (null != psychosis) {
                            if (s.weight > psychosis.weight) {
                                psychosis = s;
                            }
                        }
                        else {
                            psychosis = s;
                        }
                    }
                }
            }
            if (null != psychosis) {
                for (EvaluationFeature ef : list) {
                    ef.removeScore(psychosis);
                }
            }
        }

        // 三个核心指标
        if (depression == 0) {
            list.get(list.size() - 1).addScore(Indicator.Depression, 1, 0);
        }
        if (anxiety == 0) {
            list.get(list.size() - 1).addScore(Indicator.Anxiety, 1, 0);
        }
        if (obsession == 0) {
            list.get(list.size() - 1).addScore(Indicator.Obsession, 1, 0);
        }

        if (!this.painting.getAttribute().strict) {
            // 删除精神病性指标描述
            for (EvaluationFeature ef : list) {
                ef.removeScores(Indicator.Psychosis);
            }
        }

        return list;
    }

    private FrameStructureDescription calcFrameStructure(BoundingBox bbox) {
        int halfHeight = (int) (this.canvasSize.height * 0.5);
        int halfWidth = (int) (this.canvasSize.width * 0.5);

        BoundingBox topSpaceBox = new BoundingBox(0, 0,
                this.canvasSize.width, halfHeight);
        BoundingBox bottomSpaceBox = new BoundingBox(0, halfHeight,
                this.canvasSize.width, halfHeight);
        BoundingBox leftSpaceBox = new BoundingBox(0, 0,
                halfWidth, this.canvasSize.height);
        BoundingBox rightSpaceBox = new BoundingBox(halfWidth, 0,
                halfWidth, this.canvasSize.height);

        FrameStructureDescription fsd = new FrameStructureDescription();

        // 判读上下空间
        int topArea = topSpaceBox.calculateCollisionArea(bbox);
        int bottomArea = bottomSpaceBox.calculateCollisionArea(bbox);
        if (topArea > bottomArea) {
            fsd.addFrameStructure(FrameStructure.WholeTopSpace);
        }
        else {
            fsd.addFrameStructure(FrameStructure.WholeBottomSpace);
        }

        // 判断左右空间
        int leftArea = leftSpaceBox.calculateCollisionArea(bbox);
        int rightArea = rightSpaceBox.calculateCollisionArea(bbox);
        if (leftArea > rightArea) {
            fsd.addFrameStructure(FrameStructure.WholeLeftSpace);
        }
        else {
            fsd.addFrameStructure(FrameStructure.WholeRightSpace);
        }

        // 中间区域
        int paddingWidth = Math.round(((float) this.canvasSize.width) / 6.0f);
        int paddingHeight = Math.round(((float) this.canvasSize.height) / 6.0f);
        BoundingBox centerBox = new BoundingBox(paddingWidth, paddingHeight,
                this.canvasSize.width - paddingWidth * 2,
                this.canvasSize.height - paddingHeight * 2);
        halfHeight = (int) (centerBox.height * 0.5);
        halfWidth = (int) (centerBox.width * 0.5);
        BoundingBox topLeftBox = new BoundingBox(centerBox.x, centerBox.y, halfWidth, halfHeight);
        BoundingBox topRightBox = new BoundingBox(centerBox.x + halfWidth, centerBox.y,
                halfWidth, halfHeight);
        BoundingBox bottomLeftBox = new BoundingBox(centerBox.x, centerBox.y + halfHeight,
                halfWidth, halfHeight);
        BoundingBox bottomRightBox = new BoundingBox(centerBox.x + halfWidth, centerBox.y + halfHeight,
                halfWidth, halfHeight);
        int topLeftArea = topLeftBox.calculateCollisionArea(bbox);
        int topRightArea = topRightBox.calculateCollisionArea(bbox);
        int bottomLeftArea = bottomLeftBox.calculateCollisionArea(bbox);
        int bottomRightArea = bottomRightBox.calculateCollisionArea(bbox);

        List<AreaDesc> list = new ArrayList<>(4);
        list.add(new AreaDesc(topLeftArea, FrameStructure.CenterTopLeftSpace));
        list.add(new AreaDesc(topRightArea, FrameStructure.CenterTopRightSpace));
        list.add(new AreaDesc(bottomLeftArea, FrameStructure.CenterBottomLeftSpace));
        list.add(new AreaDesc(bottomRightArea, FrameStructure.CenterBottomRightSpace));

        // 面积从小到达排列
        Collections.sort(list, new Comparator<AreaDesc>() {
            @Override
            public int compare(AreaDesc ad1, AreaDesc ad2) {
                return ad1.area - ad2.area;
            }
        });

        fsd.addFrameStructure(list.get(list.size() - 1).structure);
        return fsd;
    }

    private boolean calcSymmetry(List<? extends Thing> thingList) {
        if (null == thingList || thingList.isEmpty()) {
            return false;
        }

        int centerX = (int) Math.round(this.canvasSize.width * 0.5);
        List<Thing> leftList = new ArrayList<>();
        List<Thing> rightList = new ArrayList<>();

        for (Thing thing : thingList) {
            if (thing.box.x1 < centerX) {
                leftList.add(thing);
            }
            else if (thing.box.x0 > centerX) {
                rightList.add(thing);
            }
        }

        if (!leftList.isEmpty() && !rightList.isEmpty()) {
            return true;
        }

        return false;
    }

    public class FrameStructureDescription {

        private List<FrameStructure> frameStructures;

        private FrameStructureDescription() {
            this.frameStructures = new ArrayList<>();
        }

        protected void addFrameStructure(FrameStructure structure) {
            if (this.frameStructures.contains(structure)) {
                return;
            }
            this.frameStructures.add(structure);
        }

        public boolean isWholeTop() {
            return this.frameStructures.contains(FrameStructure.WholeTopSpace);
        }

        public boolean isWholeBottom() {
            return this.frameStructures.contains(FrameStructure.WholeBottomSpace);
        }

        public boolean isWholeLeft() {
            return this.frameStructures.contains(FrameStructure.WholeLeftSpace);
        }

        public boolean isWholeRight() {
            return this.frameStructures.contains(FrameStructure.WholeRightSpace);
        }

        public boolean isCenterTopLeft() {
            return this.frameStructures.contains(FrameStructure.CenterTopLeftSpace);
        }

        public boolean isCenterTopRight() {
            return this.frameStructures.contains(FrameStructure.CenterTopRightSpace);
        }

        public boolean isCenterBottomLeft() {
            return this.frameStructures.contains(FrameStructure.CenterBottomLeftSpace);
        }

        public boolean isCenterBottomRight() {
            return this.frameStructures.contains(FrameStructure.CenterBottomRightSpace);
        }
    }

    private class AreaDesc {
        protected int area;
        protected FrameStructure structure;

        protected AreaDesc(int area, FrameStructure structure) {
            this.area = area;
            this.structure = structure;
        }
    }
}
