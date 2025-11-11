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
import cube.aigc.psychology.material.House;
import cube.aigc.psychology.material.Person;
import cube.util.FloatUtils;
import cube.util.calc.FrameStructureCalculator;
import cube.util.calc.FrameStructureDescription;

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

        SpaceLayout spaceLayout = new SpaceLayout(this.painting);
        results.add(this.evalSpaceStructure(spaceLayout));
        results.add(this.evalHouse(spaceLayout));
        results.add(this.evalPerson(spaceLayout));

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
                result.addScore(Indicator.AvoidantAttachment, 1, FloatUtils.random(0.3, 0.4));
            }

            // 判断面积
            if ((double)(person.area / house.area) > 0.3) {
                // 人的面积较房大
                result.addScore(Indicator.AnxiousAttachment, 1, FloatUtils.random(0.3, 0.4));
            }
            else {
                // 人的面积较房小
                result.addScore(Indicator.FearfulAttachment, 1, FloatUtils.random(0.3, 0.4));
            }
        }

        return result;
    }

    private EvaluationFeature evalHouse(SpaceLayout spaceLayout) {
        EvaluationFeature result = new EvaluationFeature();

        if (this.painting.hasHouse()) {
            List<House> houses = this.painting.getHouses();
            if (houses.size() >= 2) {
                result.addScore(Indicator.AnxiousAttachment, 1, FloatUtils.random(0.2, 0.3));

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
                    result.addScore(Indicator.FearfulAttachment, 1, FloatUtils.random(0.3, 0.4));
                }
            }
            else {
                result.addScore(Indicator.SecureAttachment, 1, FloatUtils.random(0.2, 0.3));
            }

            int countWindows = 0;
            int countDoors = 0;
            for (House house : houses) {
                if (house.hasWindow()) {
                    countWindows += house.getWindows().size();
                }
                else if (house.hasDoor()) {
                    countDoors += house.getDoors().size();
                }
            }

            if (countWindows >= 3) {
                result.addScore(Indicator.AnxiousAttachment, 1, FloatUtils.random(0.2, 0.3));
            }
            else {
                result.addScore(Indicator.AvoidantAttachment, 1, FloatUtils.random(0.2, 0.3));
            }

            if (countDoors >= 2) {
                result.addScore(Indicator.SecureAttachment, 1, FloatUtils.random(0.4, 0.5));
            }
            else {
                result.addScore(Indicator.SecureAttachment, 1, FloatUtils.random(0.2, 0.3));
            }
        }
        else {
            result.addScore(Indicator.FearfulAttachment, 1, FloatUtils.random(0.2, 0.3));
        }

        return result;
    }

    private EvaluationFeature evalPerson(SpaceLayout spaceLayout) {
        EvaluationFeature result = new EvaluationFeature();

        if (this.painting.hasPerson()) {
            List<Person> persons = this.painting.getPersons();
            if (persons.size() >= 2) {
                Person person1 = persons.get(0);
                Person person2 = persons.get(1);
                int dist = person1.distance(person2);
                if (dist > 10) {
                    result.addScore(Indicator.SecureAttachment, 1, FloatUtils.random(0.3, 0.4));
                }
                else {
                    result.addScore(Indicator.AnxiousAttachment, 1, FloatUtils.random(0.3, 0.4));
                }
            }
            else if (persons.size() == 1) {
                result.addScore(Indicator.AvoidantAttachment, 1, FloatUtils.random(0.3, 0.4));
            }

            Person person = this.painting.getPerson();
            if (!person.hasEye() && !person.hasEar() && !person.hasNose() && !person.hasMouth()) {
                result.addScore(Indicator.AvoidantAttachment, 1, FloatUtils.random(0.3, 0.4));
            }
            else {
                result.addScore(Indicator.SecureAttachment, 1, FloatUtils.random(0.3, 0.4));
            }

            if (!person.hasLeg() && !person.hasArm()) {
                result.addScore(Indicator.FearfulAttachment, 1, FloatUtils.random(0.2, 0.3));
            }
            else {
                result.addScore(Indicator.AnxiousAttachment, 1, FloatUtils.random(0.2, 0.3));
            }
        }
        else {
            result.addScore(Indicator.FearfulAttachment, 1, FloatUtils.random(0.2, 0.3));
        }

        return result;
    }
}
