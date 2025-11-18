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
import cube.aigc.psychology.composition.Texture;
import cube.aigc.psychology.material.*;
import cube.aigc.psychology.material.other.OtherSet;
import cube.util.FloatUtils;
import cube.util.Functions;
import cube.util.calc.FrameStructureCalculator;
import cube.util.calc.FrameStructureDescription;

import java.util.ArrayList;
import java.util.Arrays;
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
        results.add(this.evalHouse(spaceLayout));
        results.add(this.evalPerson(spaceLayout));
        results.add(this.evalTree(spaceLayout));
        results.add(this.evalOthers(spaceLayout));

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

                result.addScore(Indicator.AnxiousPreoccupiedAttachment, 1, FloatUtils.random(0.2, 0.3));
            }
        }

        FrameStructureCalculator calculator = new FrameStructureCalculator();
        FrameStructureDescription description = calculator.calcFrameStructure(this.painting.getCanvasSize(),
                spaceLayout.getPaintingBox());

        if (description.isNotInCorner()) {
            result.addScore(Indicator.SecureAttachment, 1, FloatUtils.random(0.2, 0.3));
        }
        else {
            result.addScore(Indicator.DismissiveAvoidantAttachment, 1, FloatUtils.random(0.3, 0.4));
        }

        if (this.painting.hasHouse() && this.painting.hasPerson()) {
            House house = this.painting.getHouse();
            Person person = this.painting.getPerson();
            int dist = house.distance(person);
            if (dist > 10) {
                // 房和人有明显
                String desc = "画面里的房和人不重合";
                result.addFeature(desc, Term.PayAttentionToFamily, Tendency.Positive, PerceptronThing.createPictureLayout());
                result.addScore(Indicator.SecureAttachment, 1, FloatUtils.random(0.3, 0.4));
            }
            else if (dist > 0) {
                // 房和人有明显
                String desc = "画面里的房和人不重合";
                result.addFeature(desc, Term.PayAttentionToFamily, Tendency.Positive, PerceptronThing.createPictureLayout());
                result.addScore(Indicator.SecureAttachment, 1, FloatUtils.random(0.2, 0.3));
            }
            else {
                result.addScore(Indicator.DismissiveAvoidantAttachment, 1, FloatUtils.random(0.3, 0.4));
            }

            // 判断面积
            if ((double)(person.area / house.area) > 0.3) {
                // 人的面积较房大
                result.addScore(Indicator.AnxiousPreoccupiedAttachment, 1, FloatUtils.random(0.3, 0.4));
            }
            else {
                // 人的面积较房小
                result.addScore(Indicator.DisorganizedAttachment, 1, FloatUtils.random(0.3, 0.4));
            }
        }

        // 画面涂鸦
        if (null != this.painting.getWhole()) {
            int doodles = 0;
            for (Texture texture : this.painting.getQuadrants()) {
                if (texture.max > 0 && texture.hierarchy > 0 && texture.density >= 0.5 && texture.max >= 5.0) {
                    ++doodles;
                }
            }

            if (doodles >= 2) {
                result.addScore(Indicator.AnxiousPreoccupiedAttachment, 1, FloatUtils.random(0.2, 0.3));
            }
        }

        return result;
    }

    private EvaluationFeature evalHouse(SpaceLayout spaceLayout) {
        EvaluationFeature result = new EvaluationFeature();

        if (this.painting.hasHouse()) {
            boolean housePath = false;

            List<House> houses = this.painting.getHouses();
            if (houses.size() >= 2) {
                result.addScore(Indicator.AnxiousPreoccupiedAttachment, 1, FloatUtils.random(0.2, 0.3));

                // 判断房之间的距离
                House house1 = houses.get(0);
                House house2 = houses.get(1);
                int dist = house1.distance(house2);
                if (dist > 0) {
                    // 房与房之间有间隔
                    result.addScore(Indicator.SecureAttachment, 1, FloatUtils.random(0.3, 0.4));
                }
                else {
                    // 房与房之间无间隔
                    result.addScore(Indicator.DisorganizedAttachment, 1, FloatUtils.random(0.3, 0.4));
                }
            }
            else {
                result.addScore(Indicator.SecureAttachment, 1, FloatUtils.random(0.2, 0.3));
            }

            int countWindows = 0;
            int countDoors = 0;
            int countChimneys = 0;
            for (House house : houses) {
                if (house.hasWindow()) {
                    countWindows += house.getWindows().size();
                }
                else if (house.hasDoor()) {
                    countDoors += house.getDoors().size();
                }
                else if (house.hasChimney()) {
                    countChimneys += 1;
                }

                if (house.hasCobbledPath() || house.hasCurvePath() || house.hasPath()) {
                    housePath = true;
                }
            }

            if (countWindows >= 3) {
                result.addScore(Indicator.AnxiousPreoccupiedAttachment, 1, FloatUtils.random(0.2, 0.3));
            }
            else {
                result.addScore(Indicator.DismissiveAvoidantAttachment, 1, FloatUtils.random(0.2, 0.3));
            }

            if (countDoors >= 2) {
                result.addScore(Indicator.SecureAttachment, 1, FloatUtils.random(0.4, 0.5));
            }
            else {
                result.addScore(Indicator.SecureAttachment, 1, FloatUtils.random(0.2, 0.3));
            }

            if (countChimneys > 0) {
                result.addScore(Indicator.AnxiousPreoccupiedAttachment, 1, FloatUtils.random(0.2, 0.3));
            }
            else {
                result.addScore(Indicator.DismissiveAvoidantAttachment, 1, FloatUtils.random(0.2, 0.3));
            }

            // 有小径
            if (housePath) {
                result.addScore(Indicator.AnxiousPreoccupiedAttachment, 1, FloatUtils.random(0.2, 0.3));
            }
        }
        else {
            result.addScore(Indicator.DisorganizedAttachment, 1, FloatUtils.random(0.2, 0.3));
        }

        return result;
    }

    private EvaluationFeature evalPerson(SpaceLayout spaceLayout) {
        EvaluationFeature result = new EvaluationFeature();

        if (this.painting.hasPerson()) {
            List<Person> persons = this.painting.getPersons();

            FrameStructureCalculator calculator = new FrameStructureCalculator();

            double[] areas = new double[persons.size()];
            List<Double> distanceList = new ArrayList<>();
            for (int i = 0; i < persons.size(); ++i) {
                Person base = persons.get(i);
                areas[i] = base.area;

                FrameStructureDescription desc = calculator.calcFrameStructure(this.painting.getCanvasSize(), base.boundingBox);
                if (desc.isInCorner()) {
                    result.addScore(Indicator.DismissiveAvoidantAttachment, 1, FloatUtils.random(0.2, 0.3));
                }

                for (Person person : persons) {
                    if (base == person) {
                        continue;
                    }

                    int dist = base.distance(person);
                    distanceList.add((double)dist);
                }
            }

            // 计算面积变异系数
            if (Functions.sampleStandardDeviation(areas) / Functions.mean(areas) <= 0.6) {
                // 面积相当
                result.addScore(Indicator.SecureAttachment, 1, FloatUtils.random(0.2, 0.3));
            }
            else {
                result.addScore(Indicator.DismissiveAvoidantAttachment, 1, FloatUtils.random(0.2, 0.3));
            }

            // 计算距离变异系数
            double[] distances = FloatUtils.toArray(distanceList.toArray(new Double[0]));
            if (Functions.sampleStandardDeviation(distances) / Functions.mean(distances) > 0.7) {
                result.addScore(Indicator.DisorganizedAttachment, 1, FloatUtils.random(0.2, 0.3));
            }

            if (persons.size() >= 2) {
                // 人物较多
                result.addScore(Indicator.SecureAttachment, 1, FloatUtils.random(0.1, 0.2));

                Person person1 = persons.get(0);
                Person person2 = persons.get(1);
                int dist = person1.distance(person2);
                if (dist > 10) {
                    result.addScore(Indicator.SecureAttachment, 1, FloatUtils.random(0.3, 0.4));
                }
                else {
                    result.addScore(Indicator.AnxiousPreoccupiedAttachment, 1, FloatUtils.random(0.3, 0.4));
                }
            }
            else if (persons.size() == 1) {
                result.addScore(Indicator.DismissiveAvoidantAttachment, 1, FloatUtils.random(0.3, 0.4));
            }

            Person person = this.painting.getPerson();
            if (!person.hasEye() && !person.hasEar() && !person.hasNose() && !person.hasMouth()) {
                result.addScore(Indicator.DismissiveAvoidantAttachment, 1, FloatUtils.random(0.3, 0.4));
            }
            else {
                result.addScore(Indicator.SecureAttachment, 1, FloatUtils.random(0.3, 0.4));
            }

            if (!person.hasLeg() && !person.hasArm()) {
                result.addScore(Indicator.DisorganizedAttachment, 1, FloatUtils.random(0.2, 0.3));
            }
            else {
                result.addScore(Indicator.AnxiousPreoccupiedAttachment, 1, FloatUtils.random(0.2, 0.3));
            }
        }
        else {
            result.addScore(Indicator.DisorganizedAttachment, 1, FloatUtils.random(0.2, 0.3));
        }

        return result;
    }

    private EvaluationFeature evalTree(SpaceLayout spaceLayout) {
        EvaluationFeature result = new EvaluationFeature();

        if (this.painting.hasTree()) {
            boolean canopy = false;
            for (Tree tree : this.painting.getTrees()) {
                if (tree.hasCanopy()) {
                    canopy = true;
                }

                if (Label.DeciduousTree == tree.getLabel()) {
                    // 落叶树
                    result.addScore(Indicator.DismissiveAvoidantAttachment, 1, FloatUtils.random(0.1, 0.2));
                }
            }

            if (canopy) {
                result.addScore(Indicator.SecureAttachment, 1, FloatUtils.random(0.2, 0.3));
            }
        }

        return result;
    }

    private EvaluationFeature evalOthers(SpaceLayout spaceLayout) {
        EvaluationFeature result = new EvaluationFeature();

        OtherSet other = this.painting.getOther();

        if (other.has(Label.Table)) {
            result.addScore(Indicator.SecureAttachment, 1, FloatUtils.random(0.4, 0.5));
        }

        if (other.has(Label.Sun)) {
            result.addScore(Indicator.SecureAttachment, 1, FloatUtils.random(0.4, 0.5));

            if (other.get(Label.Sun).isDoodle()) {
                result.addScore(Indicator.DismissiveAvoidantAttachment, 1, FloatUtils.random(0.4, 0.5));
            }
        }

        if (other.has(Label.Moon)) {
            result.addScore(Indicator.DismissiveAvoidantAttachment, 1, FloatUtils.random(0.2, 0.3));
        }

        if (other.has(Label.Mountain)) {
            result.addScore(Indicator.SecureAttachment, 1, FloatUtils.random(0.2, 0.3));
        }

        if (other.has(Label.Flower)) {
            result.addScore(Indicator.SecureAttachment, 1, FloatUtils.random(0.2, 0.3));
        }
        else {
            result.addScore(Indicator.DismissiveAvoidantAttachment, 1, FloatUtils.random(0.1, 0.2));
        }

        if (other.has(Label.Bird)) {
            List<Thing> list = other.getList(Label.Bird);
            if (list.size() > 2) {
                int max = 0;
                int min = Integer.MAX_VALUE;
                int sum = 0;
                int count = 0;
                for (int i = 0; i < list.size(); ++i) {
                    Thing base = list.get(i);
                    for (Thing bird : list) {
                        if (base == bird) {
                            continue;
                        }

                        int d = base.distance(bird);
                        if (d > max) {
                            max = d;
                        }
                        if (d < min) {
                            min = d;
                        }
                        sum += d;
                        count += 1;
                    }
                }

                // 判断鸟是否分散
                if (count > 0 && max > 0 && min > list.get(0).getWidth()) {
                    double avg = (double) sum / (double) count;
                    if (avg > list.get(0).getWidth()) {
                        result.addScore(Indicator.AnxiousPreoccupiedAttachment, 1, FloatUtils.random(0.2, 0.3));
                    }
                }
            }
        }

        if (other.has(Label.Dog) || other.has(Label.Cat)) {
            result.addScore(Indicator.SecureAttachment, 1, FloatUtils.random(0.1, 0.2));
        }
        else if (other.has(Label.Dog) && other.has(Label.Cat)) {
            result.addScore(Indicator.SecureAttachment, 1, FloatUtils.random(0.2, 0.3));
        }

        if (other.has(Label.Butterfly)) {
            result.addScore(Indicator.SecureAttachment, 1, FloatUtils.random(0.1, 0.2));
        }

        return result;
    }
}
