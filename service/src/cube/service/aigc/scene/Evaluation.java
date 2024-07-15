/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 Ambrose Xu.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package cube.service.aigc.scene;

import cell.util.Utils;
import cell.util.log.Logger;
import cube.aigc.psychology.*;
import cube.aigc.psychology.algorithm.Score;
import cube.aigc.psychology.algorithm.Tendency;
import cube.aigc.psychology.composition.FrameStructure;
import cube.aigc.psychology.composition.SpaceLayout;
import cube.aigc.psychology.composition.Texture;
import cube.aigc.psychology.composition.TheBigFive;
import cube.aigc.psychology.material.*;
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
 * 评估器。
 */
public class Evaluation {

    private Painting painting;

    private Size canvasSize;

    private SpaceLayout spaceLayout;

    private Reference reference;

    public Evaluation(Attribute attribute) {
        this.painting = new Painting(attribute);
    }

    public Evaluation(Painting painting) {
        this.painting = painting;
        this.canvasSize = painting.getCanvasSize();
        this.spaceLayout = new SpaceLayout(painting);
        this.reference = Reference.Normal;
    }

    public EvaluationFeature evalSpaceStructure() {
        EvaluationFeature result = new EvaluationFeature();

        // 画面大小比例
        double areaRatio = this.spaceLayout.getAreaRatio();

        Logger.d(this.getClass(), "#evalSpaceStructure - Area ratio: " + areaRatio +
                " - TRBL: " + spaceLayout.getTopMargin() + "," + spaceLayout.getRightMargin() +
                "," + spaceLayout.getBottomMargin() + "," + spaceLayout.getLeftMargin());

        if (areaRatio > 0) {
            if (areaRatio <= 0.09) {
                result.addScore(Indicator.Psychosis, 1, FloatUtils.random(0.2, 0.3));
                result.addScore(Indicator.Confidence, -1, FloatUtils.random(0.5, 0.6));

                // 画幅小，偏模
                this.reference = Reference.Abnormal;
                Logger.d(this.getClass(), "#evalSpaceStructure - Abnormal: 画幅小");

                result.addFiveFactor(TheBigFive.Obligingness, FloatUtils.random(0.5, 1.5));
            }
            else if (areaRatio >= (2.0d / 3.0d)) {
                result.addFeature(Term.SelfExistence, Tendency.Positive);

                result.addScore(Indicator.Extroversion, 1, FloatUtils.random(0.4, 0.5));
                result.addScore(Indicator.Narcissism, 1, FloatUtils.random(0.2, 0.3));
                result.addScore(Indicator.SocialAdaptability, 1, FloatUtils.random(0.5, 0.6));

                result.addFiveFactor(TheBigFive.Obligingness, FloatUtils.random(7.5, 8.0));
                result.addFiveFactor(TheBigFive.Extraversion, FloatUtils.random(7.0, 7.5));
            }
            else if (areaRatio < (1.0d / 6.0d)) {
                result.addFeature(Term.SelfEsteem, Tendency.Negative);
                result.addFeature(Term.SelfConfidence, Tendency.Negative);
                result.addFeature(Term.SocialAdaptability, Tendency.Negative);

                result.addScore(Indicator.Confidence, -1, FloatUtils.random(0.4, 0.5));
                result.addScore(Indicator.SelfEsteem, -1, FloatUtils.random(0.4, 0.5));
                result.addScore(Indicator.SocialAdaptability, -1, FloatUtils.random(0.4, 0.5));

                result.addFiveFactor(TheBigFive.Obligingness, FloatUtils.random(1.5, 3.5));
            }
            else if (areaRatio < (1.0d / 7.0d)) {
                result.addScore(Indicator.Introversion, 1, FloatUtils.random(0.2, 0.3));

                result.addFiveFactor(TheBigFive.Extraversion, FloatUtils.random(1.0, 2.0));
            }
            else {
                result.addFiveFactor(TheBigFive.Obligingness, FloatUtils.random(4.5, 6.5));
                result.addFiveFactor(TheBigFive.Extraversion, FloatUtils.random(3.0, 4.0));
            }
        }

        // 房、树、人之间的空间关系
        // 中线
        double banding = ((double) this.painting.getCanvasSize().height) * 0.167;
        double bl = (this.painting.getCanvasSize().height - (int) banding) * 0.5;
        double centerYOffset = this.painting.getCanvasSize().height * 0.033;   // 经验值
        int evalRange = (int) Math.round(banding * 0.5);

        House house = this.painting.getHouse();
        Tree tree = this.painting.getTree();
        Person person = this.painting.getPerson();

        if (null != house && null != tree && null != person) {
            // 位置关系，使用 box 计算位置
            Point hc = house.box.getCenterPoint();
            Point tc = tree.box.getCenterPoint();
            Point pc = person.box.getCenterPoint();

            // 判断上半将中线上移；判断下半将中心下移
            boolean houseTHalf = hc.y < (bl - centerYOffset);
            boolean houseBHalf = hc.y > (bl + centerYOffset);
            boolean treeTHalf = tc.y < (bl - centerYOffset);
            boolean treeBHalf = tc.y > (bl + centerYOffset);
            boolean personTHalf = pc.y < (bl - centerYOffset);
            boolean personBHalf = pc.y > (bl + centerYOffset);

            // 相对位置判断
            if (Math.abs(hc.y - tc.y) < evalRange && Math.abs(hc.y - pc.y) < evalRange) {
                // 基本在一个水平线上
                result.addFeature(Term.Stereotype, Tendency.Positive);

                result.addScore(Indicator.SelfConsciousness, 1, FloatUtils.random(0.3, 0.4));
            }
            else {
                // 位置分散
                result.addFeature(Term.EmotionalStability, Tendency.Negative);
                result.addScore(Indicator.Emotion, -1, FloatUtils.random(0.3, 0.4));
            }

            // 绝对位置判断
            if (houseTHalf && treeTHalf && personTHalf) {
                // 整体偏上
                result.addFeature(Term.Idealization, Tendency.Positive);

                result.addScore(Indicator.Idealism, 1, FloatUtils.random(0.3, 0.4));
                result.addScore(Indicator.Emotion, 1, FloatUtils.random(0.3, 0.4));
            }
            else if (houseBHalf && treeBHalf && personBHalf) {
                // 整体偏下
                result.addFeature(Term.Idealization, Tendency.Negative);

                result.addScore(Indicator.Realism, 1, FloatUtils.random(0.3, 0.4));
                result.addScore(Indicator.Thought, 1, FloatUtils.random(0.3, 0.4));
                result.addScore(Indicator.SenseOfSecurity, -1, FloatUtils.random(0.2, 0.3));

                result.addFiveFactor(TheBigFive.Neuroticism, FloatUtils.random(4.5, 5.5));
            }
            else {
                result.addScore(Indicator.SelfConsciousness, 1, FloatUtils.random(0.3, 0.4));
            }

            // 大小关系
            int ha = house.area;
            int ta = tree.area;
            int pa = person.area;
            if (ha >= ta && ha >= pa) {
                // 房大
                result.addFeature(Term.PayAttentionToFamily, Tendency.Positive);

                result.addScore(Indicator.Family, 1, FloatUtils.random(0.3, 0.4));

                result.addFiveFactor(TheBigFive.Obligingness, FloatUtils.random(7.5, 8.5));
            }
            if (ta >= ha && ta >= pa) {
                // 树大
                result.addFeature(Term.SocialDemand, Tendency.Positive);

                result.addScore(Indicator.InterpersonalRelation, 1, FloatUtils.random(0.3, 0.4));

                result.addFiveFactor(TheBigFive.Conscientiousness, FloatUtils.random(7.5, 8.5));
            }
            if (pa >= ha && pa >= ta) {
                // 人大
                result.addFeature(Term.SelfDemand, Tendency.Positive);
                result.addFeature(Term.SelfInflated, Tendency.Positive);
                result.addFeature(Term.SelfControl, Tendency.Negative);

                result.addScore(Indicator.SelfConsciousness, 1, FloatUtils.random(0.3, 0.4));

                result.addFiveFactor(TheBigFive.Obligingness, FloatUtils.random(4.5, 5.5));
                result.addFiveFactor(TheBigFive.Achievement, FloatUtils.random(5.5, 6.5));
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
                result.addScore(Indicator.Depression, -1, FloatUtils.random(0.2 * count, 0.25 * count));
                result.addFiveFactor(TheBigFive.Neuroticism, FloatUtils.random(1.0, 2.0));
            }
            else if (count > 0) {
                result.addScore(Indicator.Depression, -1, FloatUtils.random(0.1 * count, 0.15 * count));
                result.addFiveFactor(TheBigFive.Neuroticism, FloatUtils.random(2.0, 3.0));
            }

            if (distTP < 0) {
                // 树和人重叠
                int area = tree.box.calculateCollisionArea(person.box);
                if (Math.abs((person.box.width * person.box.height) - area) < 100) {
                    // 人和树基本重叠
                    result.addScore(Indicator.Depression, 1, FloatUtils.random(0.6, 0.7));
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
                    result.addFeature(Term.SocialPowerlessness, Tendency.Positive);
                    result.addScore(Indicator.Depression, 1, FloatUtils.random(0.3, 0.4));
                }

                result.addFiveFactor(TheBigFive.Extraversion, FloatUtils.random(6.5, 7.5));
            }

            // 三者都有
            if (house.numComponents() > 2 && tree.numComponents() > 2 && person.numComponents() > 1) {
                result.addScore(Indicator.Depression, -1, FloatUtils.random(0.75, 0.85));

                result.addFiveFactor(TheBigFive.Neuroticism, FloatUtils.random(3.5, 4.5));
            }
        }
        else if (null != house && null != tree) {
            // 位置关系，使用 box 计算位置
            Point hc = house.box.getCenterPoint();
            Point tc = tree.box.getCenterPoint();

            // 判断上半将中线上移；判断下半将中心下移
            boolean houseTHalf = hc.y < (bl - centerYOffset);
            boolean houseBHalf = hc.y > (bl + centerYOffset);
            boolean treeTHalf = tc.y < (bl - centerYOffset);
            boolean treeBHalf = tc.y > (bl + centerYOffset);

            if (Math.abs(hc.y - tc.y) < evalRange) {
                // 基本在一个水平线上
                result.addFeature(Term.Stereotype, Tendency.Positive);

                result.addScore(Indicator.SelfConsciousness, 1, FloatUtils.random(0.3, 0.4));
            }

            // 绝对位置判断
            if (houseTHalf && treeTHalf) {
                // 整体偏上
                result.addFeature(Term.Idealization, Tendency.Positive);

                result.addScore(Indicator.Idealism, 1, FloatUtils.random(0.3, 0.4));
                result.addScore(Indicator.Emotion, 1, FloatUtils.random(0.3, 0.4));
            }
            else if (houseBHalf && treeBHalf) {
                // 整体偏下
                result.addFeature(Term.Idealization, Tendency.Negative);

                result.addScore(Indicator.Realism, 1, FloatUtils.random(0.3, 0.4));
                result.addScore(Indicator.Thought, 1, FloatUtils.random(0.3, 0.4));
                result.addScore(Indicator.SenseOfSecurity, -1, FloatUtils.random(0.2, 0.3));

                result.addFiveFactor(TheBigFive.Neuroticism, FloatUtils.random(4.5, 5.5));
            }
            else {
                result.addScore(Indicator.SelfConsciousness, 1, FloatUtils.random(0.3, 0.4));
            }

            int ha = house.area;
            int ta = tree.area;
            if (ha > ta) {
                // 房大
                result.addFeature(Term.PayAttentionToFamily, Tendency.Positive);

                result.addScore(Indicator.Family, 1, FloatUtils.random(0.3, 0.4));

                result.addFiveFactor(TheBigFive.Obligingness, FloatUtils.random(6.5, 8.5));
            }
            else {
                // 树大
                result.addFeature(Term.SocialDemand, Tendency.Positive);

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

            boolean houseTHalf = hc.y < (bl - centerYOffset);
            boolean houseBHalf = hc.y > (bl + centerYOffset);
            boolean personTHalf = pc.y < (bl - centerYOffset);
            boolean personBHalf = pc.y > (bl + centerYOffset);

            if (Math.abs(hc.y - pc.y) < evalRange) {
                // 基本在一个水平线上
                result.addFeature(Term.Stereotype, Tendency.Positive);

                result.addScore(Indicator.SelfConsciousness, 1, FloatUtils.random(0.3, 0.4));
            }

            // 绝对位置判断
            if (houseTHalf && personTHalf) {
                // 整体偏上
                result.addFeature(Term.Idealization, Tendency.Positive);

                result.addScore(Indicator.Idealism, 1, FloatUtils.random(0.3, 0.4));
                result.addScore(Indicator.Emotion, 1, FloatUtils.random(0.3, 0.4));
            }
            else if (houseBHalf && personBHalf) {
                // 整体偏下
                result.addFeature(Term.Idealization, Tendency.Negative);

                result.addScore(Indicator.Realism, 1, FloatUtils.random(0.3, 0.4));
                result.addScore(Indicator.Thought, 1, FloatUtils.random(0.3, 0.4));
                result.addScore(Indicator.SenseOfSecurity, -1, FloatUtils.random(0.2, 0.3));

                result.addFiveFactor(TheBigFive.Neuroticism, FloatUtils.random(4.5, 5.5));
            }
            else {
                result.addScore(Indicator.SelfConsciousness, 1, FloatUtils.random(0.3, 0.4));
            }

            int ha = house.area;
            int pa = person.area;
            if (ha > pa) {
                // 房大
                result.addFeature(Term.PayAttentionToFamily, Tendency.Positive);

                result.addScore(Indicator.Family, 1, FloatUtils.random(0.3, 0.4));

                result.addFiveFactor(TheBigFive.Obligingness, FloatUtils.random(6.5, 8.5));
            }
            else {
                // 人大
                result.addFeature(Term.SelfDemand, Tendency.Positive);
                result.addFeature(Term.SelfInflated, Tendency.Positive);
                result.addFeature(Term.SelfControl, Tendency.Negative);

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
            boolean treeTHalf = tc.y < (bl - centerYOffset);
            boolean treeBHalf = tc.y > (bl + centerYOffset);
            boolean personTHalf = pc.y < (bl - centerYOffset);
            boolean personBHalf = pc.y > (bl + centerYOffset);

            if (Math.abs(tc.y - pc.y) < evalRange) {
                // 基本在一个水平线上
                result.addFeature(Term.Stereotype, Tendency.Positive);

                result.addScore(Indicator.SelfConsciousness, 1, FloatUtils.random(0.3, 0.4));
            }

            // 绝对位置判断
            if (treeTHalf && personTHalf) {
                // 整体偏上
                result.addFeature(Term.Idealization, Tendency.Positive);

                result.addScore(Indicator.Idealism, 1, FloatUtils.random(0.3, 0.4));
                result.addScore(Indicator.Emotion, 1, FloatUtils.random(0.3, 0.4));
            }
            else if (treeBHalf && personBHalf) {
                // 整体偏下
                result.addFeature(Term.Idealization, Tendency.Negative);

                result.addScore(Indicator.Realism, 1, FloatUtils.random(0.3, 0.4));
                result.addScore(Indicator.Thought, 1, FloatUtils.random(0.3, 0.4));
                result.addScore(Indicator.SenseOfSecurity, -1, FloatUtils.random(0.2, 0.3));

                result.addFiveFactor(TheBigFive.Neuroticism, FloatUtils.random(4.5, 5.5));
            }
            else {
                result.addScore(Indicator.SelfConsciousness, 1, FloatUtils.random(0.3, 0.4));
            }

            int ta = tree.area;
            int pa = person.area;
            if (ta > pa) {
                // 树大
                result.addFeature(Term.SocialDemand, Tendency.Positive);

                result.addScore(Indicator.InterpersonalRelation, 1, FloatUtils.random(0.3, 0.4));
            }
            else {
                // 人大
                result.addFeature(Term.SelfDemand, Tendency.Positive);
                result.addFeature(Term.SelfInflated, Tendency.Positive);
                result.addFeature(Term.SelfControl, Tendency.Negative);

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
                result.addFeature(Term.SenseOfSecurity, Tendency.Negative);

                result.addScore(Indicator.SenseOfSecurity, -1, FloatUtils.random(0.3, 0.4));

                result.addFiveFactor(TheBigFive.Neuroticism, FloatUtils.random(6.5, 7.5));
            }
        }

        // 画面涂鸦
        if (null != this.painting.getWhole()) {
            // 稀疏计数，如果稀疏大于等于3，说明画面过于简单
            int sparseness = 0;
            int doodles = 0;
            for (Texture texture : this.painting.getQuadrants()) {
                Logger.d(this.getClass(), "#evalSpaceStructure - Space texture:\n"
                        + texture.toJSON().toString(4));

                if (texture.isValid()) {
                    // 判断画面涂鸦效果
                    if (texture.density > 0.8 && texture.max < 2.0) {
                        doodles += 1;
                    }

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
            if (this.painting.getWhole().max < 2.0 && this.painting.getWhole().max != 0) {
                if (this.painting.getWhole().density > 0.1 && this.painting.getWhole().density < 0.3) {
                    sparseness += 1;
                }
                else if (this.painting.getWhole().density <= 0.1) {
                    sparseness += 2;
                }
            }
            else if (this.painting.getWhole().max >= 2.0) {
                sparseness -= 1;
            }

            if (doodles >= 2) {
                // 画面有1/2画幅涂鸦
                result.addScore(Indicator.Depression, 1, FloatUtils.random(0.5, 0.6));
                result.addScore(Indicator.Anxiety, 1, FloatUtils.random(0.9, 1.0));
                Logger.d(this.getClass(), "#evalSpaceStructure - Space doodles: " + doodles);

                result.addFiveFactor(TheBigFive.Neuroticism, FloatUtils.random(7.0, 9.0));
            }
            else if (doodles >= 1) {
                // 画面有1/4画幅涂鸦
                result.addScore(Indicator.Anxiety, 1, FloatUtils.random(0.4, 0.5));
                Logger.d(this.getClass(), "#evalSpaceStructure - Space doodles: " + doodles);

                result.addFiveFactor(TheBigFive.Neuroticism, FloatUtils.random(6.5, 7.5));
            }

            // 画面稀疏
            if (sparseness >= 4) {
                result.addScore(Indicator.Depression, 1, FloatUtils.random(0.6, 0.7));
                Logger.d(this.getClass(), "#evalSpaceStructure - Space sparseness: " + sparseness);

                result.addFiveFactor(TheBigFive.Neuroticism, FloatUtils.random(8.0, 9.0));
            }
            else if (sparseness >= 3) {
                result.addScore(Indicator.Depression, 1, FloatUtils.random(0.5, 0.6));
                Logger.d(this.getClass(), "#evalSpaceStructure - Space sparseness: " + sparseness);

                result.addFiveFactor(TheBigFive.Neuroticism, FloatUtils.random(7.5, 8.5));
            }
            else if (sparseness >= 2) {
                result.addScore(Indicator.Anxiety, 1, FloatUtils.random(0.6, 0.7));
                Logger.d(this.getClass(), "#evalSpaceStructure - Space sparseness: " + sparseness);

                result.addFiveFactor(TheBigFive.Neuroticism, FloatUtils.random(7.0, 8.0));
            }
            else if (sparseness >= 1) {
                result.addScore(Indicator.Anxiety, 1, FloatUtils.random(0.5, 0.6));
                Logger.d(this.getClass(), "#evalSpaceStructure - Space sparseness: " + sparseness);

                result.addFiveFactor(TheBigFive.Neuroticism, FloatUtils.random(6.0, 7.0));
            }

            if (doodles >= 2 && sparseness >= 2) {
                this.reference = Reference.Abnormal;
                Logger.d(this.getClass(), "#evalSpaceStructure - Abnormal: 画幅大面积涂鸦且图形稀疏");
            }
        }

        return result;
    }

    public EvaluationFeature evalFrameStructure() {
        EvaluationFeature result = new EvaluationFeature();

        FrameStructureDescription description = this.calcFrameStructure(this.spaceLayout.getPaintingBox());
        if (description.isWholeTop()) {
            // 整体顶部
            result.addFeature(Term.Idealization, Tendency.Positive);

            result.addFiveFactor(TheBigFive.Achievement, FloatUtils.random(7.0, 8.0));
        }
        else if (description.isWholeBottom()) {
            // 整体底部
            result.addFeature(Term.Instinct, Tendency.Positive);

            result.addFiveFactor(TheBigFive.Achievement, FloatUtils.random(4.0, 5.0));
        }
        else if (description.isWholeLeft()) {
            // 整体左边
            result.addFeature(Term.Nostalgia, Tendency.Positive);

            result.addFiveFactor(TheBigFive.Achievement, FloatUtils.random(2.0, 3.0));
        }
        else if (description.isWholeRight()) {
            // 整体右边
            result.addFeature(Term.Future, Tendency.Positive);

            result.addFiveFactor(TheBigFive.Achievement, FloatUtils.random(7.0, 8.0));
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
            result.addFeature(Term.EnvironmentalDependence, Tendency.Positive);

            result.addScore(Indicator.Independence, 1, FloatUtils.random(0.2, 0.3));

            result.addFiveFactor(TheBigFive.Obligingness, FloatUtils.random(5.5, 6.5));
        }

        if (maxMarginCount > 1) {
            // 未达边缘
            result.addFeature(Term.EnvironmentalAlienation, Tendency.Positive);

            result.addScore(Indicator.Independence, -1, FloatUtils.random(0.2, 0.3));

            result.addFiveFactor(TheBigFive.Obligingness, FloatUtils.random(2.5, 3.5));
        }

        if (minMarginCount >= 2) {
            result.addFiveFactor(TheBigFive.Achievement, FloatUtils.random(8.0, 8.5));
        }

        // 房、树、人各自的大小
        House house = this.painting.getHouse();
        Tree tree = this.painting.getTree();
        Person person = this.painting.getPerson();

        double hAreaRatio = (null != house) ? (double)house.area / (double)this.spaceLayout.getPaintingArea() : -1;
        double tAreaRatio = (null != tree) ? (double)tree.area / (double)this.spaceLayout.getPaintingArea() : -1;
        double pAreaRatio = (null != person) ? (double)person.area / (double)this.spaceLayout.getPaintingArea() : -1;

        // 左半边中线位置
        int halfLeftCenterX = (int) Math.round(this.canvasSize.width * 0.5 * 0.5);
        // 右半边中线位置
        int halfRightCenterX = this.canvasSize.width - halfLeftCenterX;

        if (null != house) {
            int cX = (int) Math.round(house.boundingBox.x + house.boundingBox.width * 0.5);
            if (cX < halfLeftCenterX) {
                // house 中线越过左半边中线
                result.addFeature(Term.PayAttentionToFamily, Tendency.Positive);
                result.addScore(Indicator.Family, -1, FloatUtils.random(0.3, 0.4));
            }
            else if (cX > halfRightCenterX) {
                // house 中线越过右半边中线
                result.addFeature(Term.PayAttentionToFamily, Tendency.Negative);
                result.addScore(Indicator.Family, -1, FloatUtils.random(0.3, 0.4));
            }

            if (hAreaRatio < 0.1) {
                // 房的面积非常小
                result.addFeature(Term.PayAttentionToFamily, Tendency.Negative);
                result.addScore(Indicator.Family, -1, FloatUtils.random(0.5, 0.6));

                result.addScore(Indicator.Depression, 1, FloatUtils.random(0.5, 0.6));

                result.addFiveFactor(TheBigFive.Obligingness, FloatUtils.random(1.0, 2.0));

                if (tAreaRatio > 0 && tAreaRatio < 0.09) {
                    this.reference = Reference.Abnormal;
                    Logger.d(this.getClass(), "#evalFrameStructure - Abnormal: 房面积非常小，树面积非常小");
                }
                else if (pAreaRatio > 0 && pAreaRatio <= 0.08) {
                    this.reference = Reference.Abnormal;
                    Logger.d(this.getClass(), "#evalFrameStructure - Abnormal: 房面积非常小，人面积非常小");
                }
            }

            if (hAreaRatio < 0.08) {
                result.addFiveFactor(TheBigFive.Neuroticism, FloatUtils.random(5.5, 6.5));
            }
            else {
                result.addFiveFactor(TheBigFive.Neuroticism, FloatUtils.random(4.5, 5.5));
            }
        }

        if (null != tree) {
            if (tAreaRatio < 0.1) {
                // 树的面积非常小
                result.addFiveFactor(TheBigFive.Conscientiousness, FloatUtils.random(1.5, 3.5));
            }
        }

        if (null != person) {
            if (pAreaRatio < 0.09) {
                // 人的面积非常小
                result.addFeature(Term.SenseOfSecurity, Tendency.Negative);

                result.addScore(Indicator.Depression, 1, FloatUtils.random(0.6, 0.7));
                result.addScore(Indicator.SenseOfSecurity, -1, FloatUtils.random(0.2, 0.3));
                result.addScore(Indicator.Introversion, 1, FloatUtils.random(0.3, 0.4));

                result.addFiveFactor(TheBigFive.Extraversion, FloatUtils.random(1.5, 3.0));
            }
            else if (pAreaRatio > 0.3) {
                // 人的面积非常大
                result.addFeature(Term.SelfInflated, Tendency.Positive);
                result.addScore(Indicator.Attacking, 1, FloatUtils.random(0.1, 0.2));
                result.addScore(Indicator.Extroversion, 1, FloatUtils.random(0.5, 0.6));

                result.addFiveFactor(TheBigFive.Extraversion, FloatUtils.random(8.5, 9.5));
            }
        }

        if (pAreaRatio > 0 && pAreaRatio <= 0.09 && tAreaRatio > 0 && tAreaRatio < 0.09) {
            // 偏模
            this.reference = Reference.Abnormal;
            Logger.d(this.getClass(), "#evalFrameStructure - Abnormal: 人面积非常小，树面积非常小");
        }
        else if (hAreaRatio > 0 && hAreaRatio < 0.1
                && tAreaRatio > 0 && tAreaRatio < 0.09
                && pAreaRatio > 0 && pAreaRatio < 0.09) {
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

    public EvaluationFeature evalHouse() {
        EvaluationFeature result = new EvaluationFeature();
        if (null == this.painting.getHouses()) {
            return result;
        }

        List<House> houseList = this.painting.getHouses();
        for (House house : houseList) {
            // 立体感
            if (house.hasSidewall()) {
                result.addFeature(Term.Creativity, Tendency.Positive);

                result.addScore(Indicator.Creativity, 1, FloatUtils.random(0.7, 0.8));
            }

            // 房屋类型
            if (Label.Bungalow == house.getLabel()) {
                // 平房
                result.addFeature(Term.Simple, Tendency.Positive);

                result.addScore(Indicator.Simple, 1, FloatUtils.random(0.5, 0.6));
            }
            else if (Label.Villa == house.getLabel()) {
                // 别墅
                result.addFeature(Term.Luxurious, Tendency.Positive);

                result.addScore(Indicator.EvaluationFromOutside, 1, FloatUtils.random(0.5, 0.6));
            }
            else if (Label.Building == house.getLabel()) {
                // 楼房
                result.addFeature(Term.Defensiveness, Tendency.Positive);

                result.addScore(Indicator.Realism, 1, FloatUtils.random(0.5, 0.6));
            }
            else if (Label.Fairyland == house.getLabel()) {
                // 童话房
                result.addFeature(Term.Fantasy, Tendency.Positive);
                result.addFeature(Term.Childish, Tendency.Normal);

                result.addScore(Indicator.Idealism, 1, FloatUtils.random(0.5, 0.6));
            }
            else if (Label.Temple == house.getLabel()) {
                // 庙宇
                result.addFeature(Term.Extreme, Tendency.Positive);

                result.addScore(Indicator.Paranoid, 1, FloatUtils.random(0.5, 0.6));
            }
            else if (Label.Grave == house.getLabel()) {
                // 坟墓
                result.addFeature(Term.WorldWeariness, Tendency.Positive);
            }

            // 房顶
            if (house.hasRoof()) {
                if (house.getRoof().isTextured()) {
                    result.addFeature(Term.Perfectionism, Tendency.Normal);

                    result.addScore(Indicator.Obsession, 1, FloatUtils.random(0.6, 0.7));
                }

                if (house.getRoofHeightRatio() > 0.5f) {
                    // 房顶高
                    result.addFeature(Term.Future, Tendency.Positive);

                    result.addScore(Indicator.AchievementMotivation, 1, FloatUtils.random(0.6, 0.7));

                    result.addFiveFactor(TheBigFive.Conscientiousness, FloatUtils.random(7.0, 9.0));
                }

                if (house.getRoofAreaRatio() > 0.3f) {
                    // 房顶面积大
                    result.addFeature(Term.HighPressure, Tendency.Positive);
                    result.addFeature(Term.Escapism, Tendency.Positive);

                    result.addScore(Indicator.Stress, 1, FloatUtils.random(0.4, 0.5));
                }
            }

            // 天窗
            if (house.hasRoofSkylight()) {
                result.addFeature(Term.Maverick, Tendency.Positive);

                result.addScore(Indicator.Independence, 1, FloatUtils.random(0.6, 0.7));

                result.addFiveFactor(TheBigFive.Achievement, FloatUtils.random(8.5, 9.0));
            }

            // 烟囱
            if (house.hasChimney()) {
                result.addFeature(Term.PursueInterpersonalRelationships, Tendency.Positive);

                result.addScore(Indicator.InterpersonalRelation, 1, FloatUtils.random(0.6, 0.7));

                result.addFiveFactor(TheBigFive.Extraversion, FloatUtils.random(6.0, 7.0));
            }

            // 门和窗
            if (!house.hasDoor() && !house.hasWindow()) {
                result.addFeature(Term.EmotionalIndifference, Tendency.Positive);
            }
            else {
                if (house.hasDoor()) {
                    result.addScore(Indicator.InterpersonalRelation, 1, FloatUtils.random(0.6, 0.7));

                    double areaRatio = house.getMaxDoorAreaRatio();
                    if (areaRatio < 0.05f) {
                        result.addFeature(Term.SocialPowerlessness, Tendency.Positive);
                    }
                    else if (areaRatio >= 0.15f) {
                        result.addFeature(Term.Dependence, Tendency.Positive);
                    }
                    else if (areaRatio > 0.12f) {
                        result.addFeature(Term.PursueInterpersonalRelationships, Tendency.Positive);
                    }

                    // 开启的门
                    if (house.hasOpenDoor()) {
                        result.addFeature(Term.PursueInterpersonalRelationships, Tendency.Positive);

                        result.addScore(Indicator.Extroversion, 1, FloatUtils.random(0.6, 0.7));

                        result.addFiveFactor(TheBigFive.Extraversion, FloatUtils.random(6.5, 7.5));
                    }
                }

                if (house.hasWindow()) {
                    result.addScore(Indicator.InterpersonalRelation, 1, FloatUtils.random(0.6, 0.7));

                    double areaRatio = house.getMaxWindowAreaRatio();
                    if (areaRatio < 0.03f) {
                        result.addFeature(Term.SocialPowerlessness, Tendency.Positive);
                    }
                    else if (areaRatio > 0.11f) {
                        result.addFeature(Term.PursueInterpersonalRelationships, Tendency.Positive);
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
                result.addFeature(Term.Sensitiveness, Tendency.Positive);
                result.addFeature(Term.Suspiciousness, Tendency.Positive);

                result.addScore(Indicator.InterpersonalRelation, 1, FloatUtils.random(0.6, 0.7));
            }

            // 小径
            if (house.hasPath()) {
                result.addFeature(Term.Straightforwardness, Tendency.Positive);

                if (house.hasCurvePath()) {
                    // 弯曲小径
                    result.addFeature(Term.Vigilance, Tendency.Positive);
                }

                if (house.hasCobbledPath()) {
                    // 石头小径
                    result.addFeature(Term.Perfectionism, Tendency.Positive);
                }
            }

            // 栅栏
            if (house.hasFence()) {
                result.addFeature(Term.Defensiveness, Tendency.Positive);
            }


            // 判断房屋是否涂鸦
            if (house.isDoodle()) {
                // 涂鸦的房子
                result.addScore(Indicator.Anxiety, 1, FloatUtils.random(0.7, 0.8));
                Logger.d(this.getClass(), "#evalHouse - House is doodle - " + house.texture.toJSON().toString(4));

                result.addFiveFactor(TheBigFive.Neuroticism, FloatUtils.random(7.5, 8.5));
            }
        }

        return result;
    }

    public EvaluationFeature evalTree() {
        EvaluationFeature result = new EvaluationFeature();
        if (null == this.painting.getTrees()) {
            return result;
        }

        // 整个画面里没有树干
        boolean hasTrunk = false;

        List<Tree> treeList = this.painting.getTrees();
        for (Tree tree : treeList) {
            // 树类型
            if (Label.DeciduousTree == tree.getLabel()) {
                hasTrunk = true;

                // 落叶树
                result.addFeature(Term.ExternalPressure, Tendency.Positive);

                result.addScore(Indicator.Stress, 1, FloatUtils.random(0.6, 0.7));
            }
            else if (Label.DeadTree == tree.getLabel()) {
                hasTrunk = true;

                // 枯树
                result.addFeature(Term.EmotionalDisturbance, Tendency.Positive);

                result.addScore(Indicator.Depression, 1, FloatUtils.random(0.6, 0.7));
                result.addScore(Indicator.Anxiety, 1, FloatUtils.random(0.6, 0.7));

                result.addFiveFactor(TheBigFive.Neuroticism, FloatUtils.random(6.5, 7.5));

                Logger.d(this.getClass(), "#evalTree [Depression] : dead tree");
            }
            else if (Label.PineTree == tree.getLabel()) {
                hasTrunk = true;

                // 松树
                result.addFeature(Term.SelfControl, Tendency.Positive);

                result.addScore(Indicator.AchievementMotivation, 1, FloatUtils.random(0.6, 0.7));
                result.addScore(Indicator.SelfControl, 1, FloatUtils.random(0.6, 0.7));

                result.addFiveFactor(TheBigFive.Conscientiousness, FloatUtils.random(7.0, 8.0));
            }
            else if (Label.WillowTree == tree.getLabel()) {
                hasTrunk = true;

                // 柳树
                result.addFeature(Term.Sensitiveness, Tendency.Positive);
                result.addFeature(Term.Emotionality, Tendency.Positive);

                result.addScore(Indicator.Emotion, 1, FloatUtils.random(0.6, 0.7));
            }
            else if (Label.CoconutTree == tree.getLabel()) {
                hasTrunk = true;

                // 椰子树
                result.addFeature(Term.Emotionality, Tendency.Positive);
                result.addFeature(Term.Creativity, Tendency.Positive);

                result.addScore(Indicator.Emotion, 1, FloatUtils.random(0.6, 0.7));
                result.addScore(Indicator.Creativity, 1, FloatUtils.random(0.6, 0.7));
            }
            else if (Label.Bamboo == tree.getLabel()) {
                hasTrunk = true;

                // 竹子
                result.addFeature(Term.Independence, Tendency.Positive);

                result.addScore(Indicator.Thought, 1, FloatUtils.random(0.6, 0.7));
                result.addScore(Indicator.Independence, 1, FloatUtils.random(0.6, 0.7));

                result.addFiveFactor(TheBigFive.Achievement, FloatUtils.random(8.0, 9.0));
            }
            else {
                // 常青树
                result.addFeature(Term.SelfConfidence, Tendency.Positive);
            }

            // 树干
            if (tree.hasTrunk()) {
                hasTrunk = true;
                double ratio = tree.getTrunkWidthRatio();
                Logger.d(this.getClass(), "#evalTree - Tree trunk width ratio: " + ratio);
                if (ratio < 0.18d) {
                    // 细
                    result.addFeature(Term.Powerlessness, Tendency.Positive);

                    result.addScore(Indicator.Depression, 1, FloatUtils.random(0.3, 0.4));
                    result.addScore(Indicator.SelfEsteem, -1, FloatUtils.random(0.3, 0.4));
                    result.addScore(Indicator.SocialAdaptability, -1, FloatUtils.random(0.3, 0.4));

                    result.addFiveFactor(TheBigFive.Obligingness, FloatUtils.random(1.5, 2.5));
                }
                else if (ratio >= 0.18d && ratio < 0.3d) {
                    // 粗细适度
                    result.addScore(Indicator.Pessimism, -1, FloatUtils.random(0.3, 0.4));

                    result.addFiveFactor(TheBigFive.Neuroticism, FloatUtils.random(4.5, 5.5));
                }
                else if (ratio >= 0.3d && ratio < 0.7d) {
                    // 粗
                    result.addFeature(Term.EmotionalStability, Tendency.Positive);

                    result.addScore(Indicator.Confidence, 1, FloatUtils.random(0.2, 0.3));
                    result.addScore(Indicator.Depression, -1, FloatUtils.random(0.4, 0.5));
                    Logger.d(this.getClass(), "#evalTree [Depression] : -1");

                    result.addFiveFactor(TheBigFive.Obligingness, FloatUtils.random(6.5, 7.0));
                }
                else {
                    // 很粗
                    Score score = result.addScore(Indicator.Depression, -1, FloatUtils.random(0.6, 0.7));
                    Logger.d(this.getClass(), "#evalTree [Depression] : " + score.value);

                    result.addFiveFactor(TheBigFive.Neuroticism, FloatUtils.random(1.5, 2.5));
                }
            }

            // 树根
            if (tree.hasRoot()) {
                result.addFeature(Term.Instinct, Tendency.Positive);

                result.addScore(Indicator.SelfControl, 1, FloatUtils.random(0.6, 0.7));
                result.addScore(Indicator.Paranoid, 1, FloatUtils.random(0.6, 0.7));
            }

            // 树洞
            if (tree.hasHole()) {
                result.addFeature(Term.Trauma, Tendency.Positive);
                result.addFeature(Term.EmotionalDisturbance, Tendency.Positive);

                result.addScore(Indicator.Stress, 1, FloatUtils.random(0.6, 0.7));
                result.addScore(Indicator.SenseOfSecurity, -1, FloatUtils.random(0.1, 0.2));
            }

            // 树冠大小
            if (tree.hasCanopy()) {
                result.addFeature(Term.HighEnergy, Tendency.Positive);
                // 通过评估面积和高度确定树冠大小
                if (tree.getCanopyAreaRatio() >= 0.45) {
                    result.addFeature(Term.SocialDemand, Tendency.Positive);

                    result.addScore(Indicator.Confidence, 1, FloatUtils.random(0.6, 0.7));
                    result.addScore(Indicator.InterpersonalRelation, 1, FloatUtils.random(0.6, 0.7));
                }
                else if (tree.getCanopyAreaRatio() < 0.2) {
                    result.addFeature(Term.SelfConfidence, Tendency.Negative);

                    result.addScore(Indicator.Confidence, -1, FloatUtils.random(0.6, 0.7));
                    result.addScore(Indicator.InterpersonalRelation, -1, FloatUtils.random(0.6, 0.7));
                }

                if (tree.getCanopyHeightRatio() >= 0.33) {
                    result.addFeature(Term.SelfEsteem, Tendency.Positive);

                    result.addScore(Indicator.Confidence, 1, FloatUtils.random(0.6, 0.7));
                    result.addScore(Indicator.InterpersonalRelation, 1, FloatUtils.random(0.6, 0.7));
                }
                else if (tree.getCanopyHeightRatio() < 0.2) {
                    result.addFeature(Term.SelfEsteem, Tendency.Negative);

                    result.addScore(Indicator.Confidence, -1, FloatUtils.random(0.6, 0.7));
                    result.addScore(Indicator.InterpersonalRelation, -1, FloatUtils.random(0.6, 0.7));
                }

                if (tree.getCanopyAreaRatio() < 0.2 && tree.getCanopyHeightRatio() < 0.3) {
                    result.addFeature(Term.Childish, Tendency.Positive);
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
                result.addFeature(Term.PursuitOfAchievement, Tendency.Positive);

                result.addScore(Indicator.AchievementMotivation, 1, FloatUtils.random(0.6, 0.7));

                result.addFiveFactor(TheBigFive.Conscientiousness, FloatUtils.random(7.0, 7.5));
                result.addFiveFactor(TheBigFive.Achievement, FloatUtils.random(7.5, 8.0));

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
                        result.addFeature(Term.ManyGoals, Tendency.Positive);
                        result.addFeature(Term.ManyDesires, Tendency.Positive);
                        result.addFeature(Term.SelfConfidence, Tendency.Positive);
                    }
                    else if (big) {
                        result.addFeature(Term.ManyGoals, Tendency.Positive);
                    }
                    else if (many) {
                        result.addFeature(Term.ManyGoals, Tendency.Positive);
                        result.addFeature(Term.SelfConfidence, Tendency.Negative);
                    }
                    else {
                        result.addFeature(Term.SelfConfidence, Tendency.Negative);

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
                result.addScore(Indicator.Anxiety, 1, FloatUtils.random(0.6, 0.7));
                result.addScore(Indicator.Depression, 1, FloatUtils.random(0.1, 0.2));
                Logger.d(this.getClass(), "#evalTree - Tree is doodle - \n" + tree.texture.toJSON().toString(4));

                result.addFiveFactor(TheBigFive.Neuroticism, FloatUtils.random(7.5, 8.5));
            }
        }

        if (!hasTrunk) {
            // 无树干
            result.addFeature(Term.Introversion, Tendency.Positive);

            result.addScore(Indicator.Introversion, 1, FloatUtils.random(0.3, 0.4));

            result.addFiveFactor(TheBigFive.Neuroticism, FloatUtils.random(8.0, 8.5));
        }

        return result;
    }

    public EvaluationFeature evalPerson() {
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
                    result.addFeature(Term.Defensiveness, Tendency.Positive);
                    result.addFeature(Term.Creativity, Tendency.Negative);

                    result.addScore(Indicator.Creativity, -1, FloatUtils.random(0.3, 0.4));
                }
            }
        }

        if (numStickMan >= 3) {
            result.addScore(Indicator.Obsession, 1, FloatUtils.random(0.5, 0.6));
        }

        if (numStickMan > 0) {
            // 人物没细节
            result.addFiveFactor(TheBigFive.Conscientiousness, FloatUtils.random(2.0, 3.0));
        }

        // 人是否有足够细节
        boolean detailed = false;
        boolean faceDetailed = false;
        for (Person person : this.painting.getPersons()) {
            // 判断细节
            if (person.numComponents() >= 4) {
                detailed = true;
            }

            if (person.numFaceComponents() >= 3) {
                faceDetailed = true;
            }
        }

        if (detailed && faceDetailed) {
            result.addFiveFactor(TheBigFive.Conscientiousness, FloatUtils.random(7.0, 8.0));
        }
        else if (detailed || faceDetailed) {
            result.addFiveFactor(TheBigFive.Conscientiousness, FloatUtils.random(4.5, 6.5));
        }
        else {
            result.addFiveFactor(TheBigFive.Conscientiousness, FloatUtils.random(2.0, 3.0));
        }

        for (Person person : this.painting.getPersons()) {
            // 性别
            if (person.getGender() == Person.Gender.Female) {
                if (this.painting.getAttribute().isMale()) {
                    // 男画女
                    result.addFeature(Term.SocialPowerlessness, Tendency.Positive);
                    result.addScore(Indicator.Emotion, -1, FloatUtils.random(0.3, 0.4));
                    break;
                }
            }
            else if (person.getGender() == Person.Gender.Male) {
                if (this.painting.getAttribute().isFemale()) {
                    // 女画男
                    result.addFeature(Term.SelfDemand, Tendency.Positive);
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
                        result.addFeature(Term.SocialAdaptability, Tendency.Negative);

                        result.addScore(Indicator.Impulsion, 1, FloatUtils.random(0.6, 0.7));
                        result.addScore(Indicator.SocialAdaptability, -1, FloatUtils.random(0.6, 0.7));

                        result.addFiveFactor(TheBigFive.Conscientiousness, FloatUtils.random(1.0, 3.0));
                    }
                }
            }

            // 人物动态、静态判断方式：比较手臂和腿的边界盒形状，边界盒形状越相似则越接近静态，反之为动态。
            // TODO XJW

            // 五官是否完整
            if (!(person.hasEye() && person.hasNose() && person.hasMouth())) {
                // 不完整
                result.addFeature(Term.SelfConfidence, Tendency.Negative);

                result.addScore(Indicator.Confidence, -1, FloatUtils.random(0.6, 0.7));

                result.addFiveFactor(TheBigFive.Conscientiousness, FloatUtils.random(1.0, 2.0));
            }
            else {
                result.addScore(Indicator.Introversion, 1, FloatUtils.random(0.6, 0.7));

                result.addFiveFactor(TheBigFive.Extraversion, FloatUtils.random(7.0, 8.0));
            }

            // 眼
            if (person.hasEye()) {
                // 是否睁开眼
                if (!person.hasOpenEye()) {
                    result.addFeature(Term.Hostility, Tendency.Positive);
                }

                double ratio = person.getMaxEyeAreaRatio();
                if (ratio > 0.018) {
                    // 眼睛大
                    result.addFeature(Term.Sensitiveness, Tendency.Positive);
                    result.addFeature(Term.Alertness, Tendency.Positive);
                }
            }
            else {
                result.addFeature(Term.IntrapsychicConflict, Tendency.Positive);
            }

            // 眉毛
            if (person.hasEyebrow()) {
                result.addFeature(Term.AttentionToDetail, Tendency.Positive);

                result.addScore(Indicator.Paranoid, 1, FloatUtils.random(0.6, 0.7));
            }

            // 嘴
            if (person.hasMouth()) {
                if (person.getMouth().isOpen()) {
                    result.addFeature(Term.LongingForMaternalLove, Tendency.Positive);
                }
                else if (person.getMouth().isStraight()) {
                    result.addFeature(Term.Strong, Tendency.Positive);

                    result.addScore(Indicator.Constrain, 1, FloatUtils.random(0.6, 0.7));
                }
            }

            // 耳朵
            if (!person.hasEar()) {
                // 没有耳朵
                result.addFeature(Term.Stubborn, Tendency.Positive);
            }

            // 头发
            if (person.hasHair()) {
                if (person.hasStraightHair()) {
                    // 直发
                    result.addFeature(Term.Simple, Tendency.Positive);

                    result.addScore(Indicator.Impulsion, 1, FloatUtils.random(0.6, 0.7));

                    result.addFiveFactor(TheBigFive.Conscientiousness, FloatUtils.random(0.5, 1.5));
                }
                else if (person.hasShortHair()) {
                    // 短发
                    result.addFeature(Term.DesireForControl, Tendency.Positive);

                    result.addScore(Indicator.Obsession, 1, FloatUtils.random(0.3, 0.4));

                    result.addFiveFactor(TheBigFive.Achievement, FloatUtils.random(8.5, 9.5));
                }
                else if (person.hasCurlyHair()) {
                    // 卷发
                    result.addFeature(Term.Sentimentality, Tendency.Positive);

                    result.addScore(Indicator.Independence, 1, FloatUtils.random(0.6, 0.7));
                }
                else if (person.hasStandingHair()) {
                    // 竖直头发
                    result.addFeature(Term.Aggression, Tendency.Positive);

                    result.addScore(Indicator.Hostile, 1, FloatUtils.random(0.6, 0.7));

                    result.addFiveFactor(TheBigFive.Obligingness, FloatUtils.random(0.5, 1.5));
                }
            }

            // 发饰
            if (person.hasHairAccessory()) {
                result.addFeature(Term.Narcissism, Tendency.Positive);

                result.addScore(Indicator.Narcissism, 1, FloatUtils.random(0.6, 0.7));
            }

            // 帽子
            if (person.hasCap()) {
                result.addFeature(Term.Powerlessness, Tendency.Positive);

                result.addScore(Indicator.Constrain, 1, FloatUtils.random(0.6, 0.7));
            }

            // 手臂
            if (person.hasTwoArms() && person.hasBody()) {
                // 计算手臂间距离相对于身体的宽度
                double d = person.calcArmsDistance();
                if (d > person.getBody().getWidth() * 0.5) {
                    // 手臂分开
                    result.addFeature(Term.Extroversion, Tendency.Positive);
                }

                result.addScore(Indicator.EvaluationFromOutside, 1, FloatUtils.random(0.6, 0.7));
                result.addScore(Indicator.Paranoid, 1, FloatUtils.random(0.6, 0.7));

                result.addFiveFactor(TheBigFive.Conscientiousness, FloatUtils.random(8.0, 9.0));
            }

            // 腿
            if (person.hasTwoLegs()) {
                double d = person.calcLegsDistance();
                Leg thinLeg = person.getThinnestLeg();
                if (null != thinLeg) {
                    if (d < thinLeg.getWidth() * 0.5) {
                        // 腿的距离较近
                        result.addFeature(Term.Cautious, Tendency.Positive);
                        result.addFeature(Term.Introversion, Tendency.Positive);

                        result.addFiveFactor(TheBigFive.Extraversion, FloatUtils.random(4.0, 5.0));
                    }
                }

                result.addFiveFactor(TheBigFive.Conscientiousness, FloatUtils.random(7.5, 8.5));
            }

            // 判断人是否涂鸦
            if (person.texture.isValid()) {
                if (person.texture.max >= 1.0 && person.texture.max < 2.0) {
                    // 判断标准差和层密度
                    if (person.texture.standardDeviation >= 0.42 && person.texture.hierarchy <= 0.05) {
                        // 涂鸦的人
                        result.addScore(Indicator.Anxiety, 1, FloatUtils.random(0.6, 0.7));
                        Logger.d(this.getClass(), "#evalPerson - Person is doodle - \n" +
                                person.texture.toJSON().toString(4));

                        result.addFiveFactor(TheBigFive.Neuroticism, FloatUtils.random(5.5, 6.5));
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

    public EvaluationFeature evalOthers() {
        EvaluationFeature result = new EvaluationFeature();

        OtherSet other = this.painting.getOther();

        int counter = 0;

        if (other.has(Label.Table)) {
            // 桌子
            result.addFeature(Term.PayAttentionToFamily, Tendency.Positive);
            result.addScore(Indicator.Family, 1, FloatUtils.random(0.7, 0.8));
        }

        if (other.has(Label.Bed)) {
            // 床
            result.addScore(Indicator.Family, -1, FloatUtils.random(0.7, 0.8));

            result.addFiveFactor(TheBigFive.Obligingness, FloatUtils.random(1.0, 2.5));
        }

        if (other.has(Label.Sun)) {
            // 太阳
            result.addFeature(Term.PositiveExpectation, Tendency.Positive);

            result.addScore(Indicator.Optimism, 1, FloatUtils.random(0.7, 0.8));
            result.addScore(Indicator.Depression, -1, FloatUtils.random(0.4, 0.5));

            if (other.get(Label.Sun).isDoodle()) {
                // 涂鸦的太阳
                Logger.d(this.getClass(), "#evalOthers - Sun is doodle - \n"
                        + other.get(Label.Sun).texture.toJSON().toString(4));
                result.addScore(Indicator.Depression, 1, FloatUtils.random(0.7, 0.8));
            }
        }

        if (other.has(Label.Moon)) {
            // 月亮
            result.addFeature(Term.Sentimentality, Tendency.Positive);
        }

        if (other.has(Label.Star)) {
            // 星星
            result.addFeature(Term.Fantasy, Tendency.Positive);
        }

        if (other.has(Label.Mountain)) {
            // 山
            result.addFeature(Term.NeedProtection, Tendency.Positive);
            result.addScore(Indicator.SenseOfSecurity, -1, FloatUtils.random(0.3, 0.4));
        }

        if (other.has(Label.Flower)) {
            // 花
            result.addFeature(Term.Vanity, Tendency.Positive);
            result.addScore(Indicator.EvaluationFromOutside, 1, FloatUtils.random(0.7, 0.8));
            counter += 1;
        }

        if (other.has(Label.Grass)) {
            // 草
            result.addFeature(Term.Stubborn, Tendency.Positive);
        }

        if (other.has(Label.Sea)) {
            // 海
            result.addFeature(Term.DesireForFreedom, Tendency.Normal);
        }

        if (other.has(Label.Pool)) {
            // 池塘
            result.addFeature(Term.Stubborn, Tendency.Normal);
            result.addScore(Indicator.InterpersonalRelation, -1, FloatUtils.random(0.2, 0.3));
        }

        if (other.has(Label.Sunflower)) {
            // 向日葵
            result.addFeature(Term.Extroversion, Tendency.Positive);
            result.addFeature(Term.PursuitOfAchievement, Tendency.Normal);
            result.addScore(Indicator.Extroversion, 1, FloatUtils.random(0.7, 0.8));
            result.addFiveFactor(TheBigFive.Extraversion, FloatUtils.random(8.5, 9.5));
            counter += 1;
        }

        if (other.has(Label.Mushroom)) {
            // 蘑菇
            result.addScore(Indicator.Stress, 1, FloatUtils.random(0.7, 0.8));
            counter += 1;
        }

        if (other.has(Label.Lotus)) {
            // 莲花
            result.addFeature(Term.SelfInflated, Tendency.Positive);
            result.addFeature(Term.Creativity, Tendency.Normal);
            result.addScore(Indicator.Confidence, 1, FloatUtils.random(0.2, 0.3));
            result.addScore(Indicator.MoralSense, 1, FloatUtils.random(0.7, 0.8));
            counter += 1;
        }

        if (other.has(Label.PlumFlower)) {
            // 梅花
            result.addFeature(Term.SelfEsteem, Tendency.Positive);
            result.addScore(Indicator.SelfEsteem, 1, FloatUtils.random(0.7, 0.8));
            counter += 1;
        }

        if (other.has(Label.Rose)) {
            // 玫瑰
            result.addFeature(Term.Creativity, Tendency.Positive);
            result.addScore(Indicator.Creativity, 1, FloatUtils.random(0.7, 0.8));
            counter += 1;
        }

        if (other.has(Label.Cloud)) {
            // 云
            result.addFeature(Term.Imagination, Tendency.Positive);
            result.addScore(Indicator.Optimism, 1, FloatUtils.random(0.7, 0.8));
            result.addScore(Indicator.Idealism, 1, FloatUtils.random(0.7, 0.8));
        }

        if (other.has(Label.Rain)) {
            // 雨
            result.addFeature(Term.HighPressure, Tendency.Positive);
            result.addScore(Indicator.Stress, 1, FloatUtils.random(0.7, 0.8));
        }

        if (other.has(Label.Rainbow)) {
            // 彩虹
            result.addFeature(Term.Future, Tendency.Positive);
            result.addScore(Indicator.Simple, 1, FloatUtils.random(0.7, 0.8));
        }

        if (other.has(Label.Torch)) {
            // 火炬
            result.addScore(Indicator.Hostile, 1, FloatUtils.random(0.3, 0.4));

            result.addFiveFactor(TheBigFive.Obligingness, FloatUtils.random(2.5, 3.5));
        }

        if (other.has(Label.Bonfire)) {
            // 火堆
            result.addScore(Indicator.Hostile, 1, FloatUtils.random(0.3, 0.4));

            result.addFiveFactor(TheBigFive.Obligingness, FloatUtils.random(2.5, 3.5));
        }

        if (other.has(Label.Bird)) {
            // 鸟
            result.addFeature(Term.DesireForFreedom, Tendency.Positive);
            result.addScore(Indicator.InterpersonalRelation, 1, FloatUtils.random(0.7, 0.8));
            result.addScore(Indicator.DesireForFreedom, 1, FloatUtils.random(0.7, 0.8));
            counter += 1;
        }

        if (other.has(Label.Cat)) {
            // 猫
            result.addFeature(Term.SocialDemand, Tendency.Positive);
            result.addScore(Indicator.Emotion, 1, FloatUtils.random(0.7, 0.8));
            result.addScore(Indicator.InterpersonalRelation, 1, FloatUtils.random(0.3, 0.4));
            result.addScore(Indicator.Meekness, 1, FloatUtils.random(0.5, 0.6));
            counter += 1;
        }

        if (other.has(Label.Dog)) {
            // 狗
            result.addFeature(Term.NeedProtection, Tendency.Positive);
            result.addFeature(Term.SenseOfSecurity, Tendency.Negative);
            result.addScore(Indicator.SenseOfSecurity, -1, FloatUtils.random(0.3, 0.4));
            counter += 1;
        }

        if (other.has(Label.Cow)) {
            // 牛
            result.addScore(Indicator.Struggle, 1, FloatUtils.random(0.7, 0.8));
            counter += 1;

            result.addFiveFactor(TheBigFive.Conscientiousness, FloatUtils.random(8.0, 9.5));
        }

        if (other.has(Label.Sheep)) {
            // 羊
            result.addScore(Indicator.Meekness, 1, FloatUtils.random(0.7, 0.8));
            counter += 1;
        }

        if (other.has(Label.Pig)) {
            // 猪
            result.addScore(Indicator.Meekness, 1, FloatUtils.random(0.5, 0.6));
            counter += 1;
        }

        if (other.has(Label.Fish)) {
            // 鱼
            result.addFeature(Term.DesireForFreedom, Tendency.Positive);
            result.addScore(Indicator.InterpersonalRelation, 1, FloatUtils.random(0.5, 0.6));
            result.addScore(Indicator.DesireForFreedom, 1, FloatUtils.random(0.7, 0.8));
            counter += 1;
        }

        if (other.has(Label.Rabbit)) {
            // 兔
            result.addScore(Indicator.Meekness, 1, FloatUtils.random(0.7, 0.8));
            counter += 1;
        }

        if (other.has(Label.Horse)) {
            // 马
            result.addFeature(Term.DesireForFreedom, Tendency.Positive);
            result.addScore(Indicator.DesireForFreedom, 1, FloatUtils.random(0.7, 0.8));
            counter += 1;
        }

        if (other.has(Label.Hawk)) {
            // 鹰
            result.addScore(Indicator.Struggle, 1, FloatUtils.random(0.7, 0.8));
            result.addScore(Indicator.Creativity, 1, FloatUtils.random(0.7, 0.8));
            counter += 1;
        }

        if (other.has(Label.Rat)) {
            // 鼠
            result.addFeature(Term.Sensitiveness, Tendency.Positive);
            result.addScore(Indicator.Confidence, -1, FloatUtils.random(0.7, 0.8));
            counter += 1;
        }

        if (other.has(Label.Butterfly)) {
            // 蝴蝶
            result.addFeature(Term.PursuitOfAchievement, Tendency.Positive);
        }

        if (other.has(Label.Tiger)) {
            // 虎
            result.addFeature(Term.Extroversion, Tendency.Positive);
            result.addFeature(Term.SelfConfidence, Tendency.Positive);
            result.addScore(Indicator.Extroversion, 1, FloatUtils.random(0.7, 0.8));
            result.addScore(Indicator.Confidence, 1, FloatUtils.random(0.5, 0.6));
            result.addFiveFactor(TheBigFive.Extraversion, FloatUtils.random(7.0, 8.0));
            counter += 1;
        }

        if (other.has(Label.Hedgehog)) {
            // 刺猬
            result.addScore(Indicator.SenseOfSecurity, -1, FloatUtils.random(0.7, 0.8));
            counter += 1;
        }

        if (other.has(Label.Snake)) {
            // 蛇
            result.addFeature(Term.Trauma, Tendency.Positive);
            result.addScore(Indicator.Emotion, -1, FloatUtils.random(0.3, 0.4));
        }

        if (other.has(Label.Dragon)) {
            // 龙
            result.addFeature(Term.PursuitOfAchievement, Tendency.Positive);
            result.addScore(Indicator.AchievementMotivation, 1, FloatUtils.random(0.7, 0.8));
            counter += 1;

            result.addFiveFactor(TheBigFive.Conscientiousness, FloatUtils.random(7.5, 8.5));
        }

        if (other.has(Label.Watch)) {
            // 表
            result.addScore(Indicator.Anxiety, 1, FloatUtils.random(0.7, 0.8));

            result.addFiveFactor(TheBigFive.Neuroticism, FloatUtils.random(4.0, 5.0));
        }

        if (other.has(Label.Clock)) {
            // 钟
            result.addScore(Indicator.Anxiety, 1, FloatUtils.random(0.7, 0.8));

            result.addFiveFactor(TheBigFive.Neuroticism, FloatUtils.random(4.0, 5.0));
        }

        if (other.has(Label.MusicalNotation)) {
            // 音乐符号
            result.addScore(Indicator.Psychosis, 1, FloatUtils.random(0.1, 0.2));
        }

        if (other.has(Label.TV)) {
            // 电视
            result.addFeature(Term.PayAttentionToFamily, Tendency.Negative);
            result.addScore(Indicator.Family, -1, FloatUtils.random(0.4, 0.5));
        }

        if (other.has(Label.Pole)) {
            // 电线杆
            result.addFeature(Term.Stubborn, Tendency.Positive);
        }

        if (other.has(Label.Tower)) {
            // 铁塔
            result.addFeature(Term.Stereotype, Tendency.Positive);
            result.addScore(Indicator.MoralSense, 1, FloatUtils.random(0.7, 0.8));
        }

        if (other.has(Label.Lighthouse)) {
            // 灯塔
            result.addFeature(Term.Idealization, Tendency.Positive);
        }

        if (other.has(Label.Gun)) {
            // 枪
            result.addScore(Indicator.Attacking, 1, FloatUtils.random(0.7, 0.8));

            result.addFiveFactor(TheBigFive.Obligingness, FloatUtils.random(0.5, 1.5));
        }

        if (other.has(Label.Sword)) {
            // 剑
            result.addScore(Indicator.Attacking, 1, FloatUtils.random(0.7, 0.8));
        }

        if (other.has(Label.Knife)) {
            // 刀
            result.addScore(Indicator.Attacking, 1, FloatUtils.random(0.7, 0.8));
        }

        if (other.has(Label.Shield)) {
            // 盾
            result.addFeature(Term.Defensiveness, Tendency.Positive);
            result.addFeature(Term.NeedProtection, Tendency.Positive);
            result.addScore(Indicator.Pessimism, 1, FloatUtils.random(0.7, 0.8));
        }

        if (other.has(Label.Sandglass)) {
            // 沙漏
            result.addScore(Indicator.Anxiety, 1, FloatUtils.random(0.7, 0.8));
        }

        if (other.has(Label.Kite)) {
            // 风筝
            result.addFeature(Term.DesireForFreedom, Tendency.Positive);
            result.addScore(Indicator.DesireForFreedom, 1, FloatUtils.random(0.7, 0.8));
            counter += 1;
        }

        if (other.has(Label.Umbrella)) {
            // 伞
            result.addScore(Indicator.SenseOfSecurity, -1, FloatUtils.random(0.7, 0.8));
        }

        if (other.has(Label.Windmill)) {
            // 风车
            result.addFeature(Term.Fantasy, Tendency.Positive);
            result.addScore(Indicator.Simple, 1, FloatUtils.random(0.7, 0.8));
            counter += 1;
        }

        if (other.has(Label.Flag)) {
            // 旗帜
            result.addScore(Indicator.MoralSense, 1, FloatUtils.random(0.7, 0.8));
        }

        if (other.has(Label.Bridge)) {
            // 桥
            result.addFeature(Term.PursueInterpersonalRelationships, Tendency.Positive);
            result.addScore(Indicator.InterpersonalRelation, -1, FloatUtils.random(0.7, 0.8));
        }

        if (other.has(Label.Crossroads)) {
            // 十字路口
            result.addScore(Indicator.Anxiety, 1, FloatUtils.random(0.5, 0.6));

            result.addFiveFactor(TheBigFive.Neuroticism, FloatUtils.random(7.0, 7.5));
        }

        if (other.has(Label.Ladder)) {
            // 梯子
            result.addFeature(Term.PursuitOfAchievement, Tendency.Positive);
            result.addScore(Indicator.AchievementMotivation, 1, FloatUtils.random(0.7, 0.8));
            result.addScore(Indicator.Anxiety, 1, FloatUtils.random(0.5, 0.6));

            result.addFiveFactor(TheBigFive.Conscientiousness, FloatUtils.random(7.0, 9.0));
        }

        if (other.has(Label.Stairs)) {
            // 楼梯
            result.addFeature(Term.EnvironmentalAlienation, Tendency.Positive);
            result.addScore(Indicator.AchievementMotivation, 1, FloatUtils.random(0.7, 0.8));
            result.addScore(Indicator.Anxiety, 1, FloatUtils.random(0.5, 0.6));

            result.addFiveFactor(TheBigFive.Conscientiousness, FloatUtils.random(7.0, 9.0));
        }

        if (other.has(Label.Birdcage)) {
            // 鸟笼
            result.addFeature(Term.DesireForFreedom, Tendency.Positive);
            result.addScore(Indicator.DesireForFreedom, 1, FloatUtils.random(0.7, 0.8));
            counter += 1;
        }

        if (other.has(Label.Car)) {
            // 汽车
            result.addFeature(Term.Luxurious, Tendency.Positive);
        }

        if (other.has(Label.Boat)) {
            // 船
            result.addFeature(Term.DesireForFreedom, Tendency.Positive);
            result.addScore(Indicator.DesireForFreedom, 1, FloatUtils.random(0.7, 0.8));
        }

        if (other.has(Label.Airplane)) {
            // 飞机
            result.addFeature(Term.Escapism, Tendency.Positive);
            result.addScore(Indicator.Independence, 1, FloatUtils.random(0.6, 0.7));
            result.addFiveFactor(TheBigFive.Achievement, FloatUtils.random(6.5, 7.5));
        }

        if (other.has(Label.Bike)) {
            // 自行车
            result.addFeature(Term.EmotionalDisturbance, Tendency.Positive);
            counter += 1;
        }

        if (other.has(Label.Skull)) {
            // 骷髅
            result.addFeature(Term.WorldWeariness, Tendency.Positive);
            result.addScore(Indicator.Psychosis, 1, FloatUtils.random(0.6, 0.7));
        }

        if (other.has(Label.Glasses)) {
            // 眼镜
            result.addFeature(Term.Escapism, Tendency.Positive);
        }

        if (other.has(Label.Swing)) {
            // 秋千
            result.addFeature(Term.Childish, Tendency.Positive);
        }

        if (counter >= 2) {
            result.addFeature(Term.Creativity, Tendency.Positive);
            result.addScore(Indicator.Creativity, counter, FloatUtils.random(0.7, 0.8));
        }

        return result;
    }

    private List<EvaluationFeature> correct(List<EvaluationFeature> list) {
        // 如果有乐观和社会适应性，则社会适应性负分降分
        double optimism = 0;

        // 如果抑郁都是负分，则删除所有抑郁指标
        int depressionValue = 0;
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

                    if (score.value > 0) {
                        w = w - score.weight;
                        ef.removeScore(score);
                        ef.addScore(Indicator.Stress, 1, score.weight);
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
                depressionValue += s.value;
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

        if (depressionValue < 0 && depressionCount == Math.abs(depressionValue)) {
            for (EvaluationFeature ef : list) {
                ef.removeScores(Indicator.Depression);
            }
        }
        else if ((depressionValue < 0 && depression > 0.5) ||
                (depressionValue < 0 && depression > 0.3 && depressionCount >= 5)) {
            // 增加一个权重负值
            Logger.w(this.getClass(), "#correct - depression: " + depressionValue + " - " + depression);
            list.get(list.size() - 1).addScore(Indicator.Depression, -1, FloatUtils.random(0.79, 0.88));
        }
        else if (depressionCount > 0) {
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

    /**
     * 生成评估报告。
     *
     * @return
     */
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
                report = new EvaluationReport(this.painting.getAttribute(), this.reference, list);
                return report;
            }

            List<EvaluationFeature> results = new ArrayList<>();
            results.add(this.evalSpaceStructure());
            results.add(this.evalFrameStructure());
            results.add(this.evalHouse());
            results.add(this.evalTree());
            results.add(this.evalPerson());
            results.add(this.evalOthers());
            // 矫正
            results = this.correct(results);
            report = new EvaluationReport(this.painting.getAttribute(), this.reference, results);
        }
        else {
            Logger.w(this.getClass(), "#makeEvaluationReport - Only for test");
            // 仅用于测试
            EvaluationFeature result = new EvaluationFeature();
            int num = Utils.randomInt(3, 5);
            for (int i = 0; i < num; ++i) {
                int index = Utils.randomInt(0, Term.values().length - 1);
                result.addFeature(Term.values()[index], Tendency.Positive);
            }
            report = new EvaluationReport(this.painting.getAttribute(), this.reference, result);
        }

        return report;
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
