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

    public Evaluation(Attribute attribute) {
        this.painting = new Painting(attribute);
    }

    public Evaluation(Painting painting) {
        this.painting = painting;
        this.canvasSize = painting.getCanvasSize();
        this.spaceLayout = new SpaceLayout(painting);
    }

    public EvaluationFeature evalSpaceStructure() {
        EvaluationFeature result = new EvaluationFeature();

        // 画面大小比例
        double areaRatio = this.spaceLayout.getAreaRatio();

        Logger.d(this.getClass(), "#evalSpaceStructure - Area ratio: " + areaRatio +
                " - TRBL: " + spaceLayout.getTopMargin() + "," + spaceLayout.getRightMargin() +
                "," + spaceLayout.getBottomMargin() + "," + spaceLayout.getLeftMargin());

        if (areaRatio > 0) {
            if (areaRatio <= 0.08) {
                result.addScore(Indicator.Psychosis, 1, FloatUtils.random(0.7, 0.8));
                result.addScore(Indicator.Confidence, -1, FloatUtils.random(0.5, 0.6));
            }
            else if (areaRatio >= (2.0d / 3.0d)) {
                result.addFeature(Comment.SelfExistence, Tendency.Positive);

                result.addScore(Indicator.Extroversion, 1, FloatUtils.random(0.4, 0.5));
                result.addScore(Indicator.Narcissism, 1, FloatUtils.random(0.2, 0.3));
                // FIXME 画幅大，社会适应性正分
//                result.addScore(Indicator.SocialAdaptability, 1, FloatUtils.random(0.5, 0.6));
            }
            else if (areaRatio < (1.0d / 6.0d)) {
                result.addFeature(Comment.SelfEsteem, Tendency.Negative);
                result.addFeature(Comment.SelfConfidence, Tendency.Negative);
                result.addFeature(Comment.SocialAdaptability, Tendency.Negative);

                result.addScore(Indicator.Introversion, 1, FloatUtils.random(0.2, 0.3));
                result.addScore(Indicator.Confidence, -1, FloatUtils.random(0.4, 0.5));
                result.addScore(Indicator.SelfEsteem, -1, FloatUtils.random(0.4, 0.5));
                result.addScore(Indicator.SocialAdaptability, -1, FloatUtils.random(0.4, 0.5));
            }
            else {
                result.addFeature(Comment.SelfEsteem, Tendency.Negative);
                result.addFeature(Comment.SelfConfidence, Tendency.Negative);

                result.addScore(Indicator.Confidence, -1, FloatUtils.random(0.5, 0.6));
                result.addScore(Indicator.SelfEsteem, -1, FloatUtils.random(0.2, 0.3));
                // FIXME 画幅大，社会适应性正分
//                result.addScore(Indicator.SocialAdaptability, 1, FloatUtils.random(0.2, 0.3));
            }
        }

        // 空间构图
        double minThreshold = this.canvasSize.width * 0.025f;
        double maxThreshold = this.canvasSize.width * 0.15f;
        if (this.spaceLayout.getTopMargin() < minThreshold
                || this.spaceLayout.getRightMargin() < minThreshold
                || this.spaceLayout.getBottomMargin() < minThreshold
                || this.spaceLayout.getLeftMargin() < minThreshold) {
            // 达到边缘
            result.addFeature(Comment.EnvironmentalDependence, Tendency.Positive);

            result.addScore(Indicator.Independence, 1, FloatUtils.random(0.2, 0.3));
        }
        else if (this.spaceLayout.getTopMargin() > maxThreshold
                || this.spaceLayout.getRightMargin() > maxThreshold
                || this.spaceLayout.getBottomMargin() > maxThreshold
                || this.spaceLayout.getLeftMargin() > maxThreshold) {
            // 未达边缘
            result.addFeature(Comment.EnvironmentalAlienation, Tendency.Positive);

            result.addScore(Indicator.Independence, -1, FloatUtils.random(0.2, 0.3));
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
        if (null != person) {
            if (person instanceof StickMan) {
                // 排除火柴人
                person = null;

                result.addFeature(Comment.Creativity, Tendency.Negative);
                result.addScore(Indicator.Creativity, -1, FloatUtils.random(0.3, 0.4));

                for (Person p : this.painting.getPersons()) {
                    if (!(p instanceof StickMan)) {
                        person = p;
                    }
                }
            }
        }

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
                result.addFeature(Comment.Stereotype, Tendency.Positive);

                result.addScore(Indicator.SelfConsciousness, 1, FloatUtils.random(0.3, 0.4));
            }
            else {
                // 位置分散
                result.addFeature(Comment.EmotionalStability, Tendency.Negative);
                result.addScore(Indicator.Emotion, -1, FloatUtils.random(0.3, 0.4));
            }

            // 绝对位置判断
            if (houseTHalf && treeTHalf && personTHalf) {
                // 整体偏上
                result.addFeature(Comment.Idealization, Tendency.Positive);

                result.addScore(Indicator.Idealism, 1, FloatUtils.random(0.3, 0.4));
                result.addScore(Indicator.Emotion, 1, FloatUtils.random(0.3, 0.4));
            }
            else if (houseBHalf && treeBHalf && personBHalf) {
                // 整体偏下
                result.addFeature(Comment.Idealization, Tendency.Negative);

                result.addScore(Indicator.Realism, 1, FloatUtils.random(0.3, 0.4));
                result.addScore(Indicator.Thought, 1, FloatUtils.random(0.3, 0.4));
                result.addScore(Indicator.SenseOfSecurity, -1, FloatUtils.random(0.2, 0.3));
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
                result.addFeature(Comment.PayAttentionToFamily, Tendency.Positive);

                result.addScore(Indicator.Family, 1, FloatUtils.random(0.3, 0.4));
            }
            if (ta >= ha && ta >= pa) {
                // 树大
                result.addFeature(Comment.SocialDemand, Tendency.Positive);

                result.addScore(Indicator.InterpersonalRelation, 1, FloatUtils.random(0.3, 0.4));
            }
            if (pa >= ha && pa >= ta) {
                // 人大
                result.addFeature(Comment.SelfDemand, Tendency.Positive);
                result.addFeature(Comment.SelfInflated, Tendency.Positive);
                result.addFeature(Comment.SelfControl, Tendency.Negative);

                result.addScore(Indicator.SelfConsciousness, 1, FloatUtils.random(0.3, 0.4));
            }

            // 房的面积非常小
            double hR = (double)ha / (double)this.spaceLayout.getPaintingArea();
            if (hR < 0.1) {
                result.addFeature(Comment.PayAttentionToFamily, Tendency.Negative);
                result.addScore(Indicator.Family, -1, FloatUtils.random(0.5, 0.6));
            }

            // 人的面积非常小
            double hP = (double)pa / (double)this.spaceLayout.getPaintingArea();
            if (hP < 0.09) {
                result.addScore(Indicator.Depression, 1, FloatUtils.random(0.6, 0.7));
            }

            // 距离
            int distHT = house.distance(tree);
            int distPH = person.distance(house);
            int distTP = tree.distance(person);
            Logger.d(this.getClass(), "#evalSpaceStructure - distance: " + distHT + "/" + distPH + "/" + distTP);
            if (distHT > 0) {
                result.addScore(Indicator.Depression, -1, FloatUtils.random(0.1, 0.2));
            }
            if (distPH > 0) {
                result.addScore(Indicator.Depression, -1, FloatUtils.random(0.1, 0.2));
            }
            if (distTP > 0) {
                result.addScore(Indicator.Depression, -1, FloatUtils.random(0.1, 0.2));
            }
            else {
                // 树和人重叠
                int area = tree.box.calculateCollisionArea(person.box);
                if (Math.abs((person.box.width * person.box.height) - area) < 100) {
                    // 人和树基本重叠
                    result.addScore(Indicator.Depression, 1, FloatUtils.random(0.6, 0.7));
                }
            }

            if (distHT > 10 && distPH > 10 && distTP > 10) {
                // HTP 距离大，判断所占画幅大小
                double paintingArea = this.spaceLayout.getPaintingBox().calculateArea() * 0.9;
                double rH = house.boundingBox.calculateArea() * 0.9 / paintingArea;
                double rT = tree.boundingBox.calculateArea() * 0.9 / paintingArea;
                double rP = person.boundingBox.calculateArea() * 0.9 / paintingArea;
                Logger.d(this.getClass(), "#evalSpaceStructure - Area ratio: " + rH + "," + rT + "," + rP);
                if (rH < 0.1 || rT < 0.1 || rP < 0.1) {
                    result.addFeature(Comment.SocialPowerlessness, Tendency.Positive);
                    result.addScore(Indicator.Depression, 1, FloatUtils.random(0.3, 0.4));
                }
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
                result.addFeature(Comment.Stereotype, Tendency.Positive);

                result.addScore(Indicator.SelfConsciousness, 1, FloatUtils.random(0.3, 0.4));
            }

            // 绝对位置判断
            if (houseTHalf && treeTHalf) {
                // 整体偏上
                result.addFeature(Comment.Idealization, Tendency.Positive);

                result.addScore(Indicator.Idealism, 1, FloatUtils.random(0.3, 0.4));
                result.addScore(Indicator.Emotion, 1, FloatUtils.random(0.3, 0.4));
            }
            else if (houseBHalf && treeBHalf) {
                // 整体偏下
                result.addFeature(Comment.Idealization, Tendency.Negative);

                result.addScore(Indicator.Realism, 1, FloatUtils.random(0.3, 0.4));
                result.addScore(Indicator.Thought, 1, FloatUtils.random(0.3, 0.4));
                result.addScore(Indicator.SenseOfSecurity, -1, FloatUtils.random(0.2, 0.3));
            }
            else {
                result.addScore(Indicator.SelfConsciousness, 1, FloatUtils.random(0.3, 0.4));
            }

            int ha = house.area;
            int ta = tree.area;
            if (ha > ta) {
                // 房大
                result.addFeature(Comment.PayAttentionToFamily, Tendency.Positive);

                result.addScore(Indicator.Family, 1, FloatUtils.random(0.3, 0.4));
            }
            else {
                // 树大
                result.addFeature(Comment.SocialDemand, Tendency.Positive);

                result.addScore(Indicator.InterpersonalRelation, 1, FloatUtils.random(0.3, 0.4));
            }

            // 房的面积非常小
            double hR = (double)ha / (double)this.spaceLayout.getPaintingArea();
            if (hR < 0.1) {
                result.addFeature(Comment.PayAttentionToFamily, Tendency.Negative);
                result.addScore(Indicator.Family, -1, FloatUtils.random(0.5, 0.6));
            }

            // 间距
            Logger.d(this.getClass(), "#evalSpaceStructure - distance: HT - " + house.distance(tree));
            if (house.distance(tree) > 0) {
                result.addScore(Indicator.Depression, -1, FloatUtils.random(0.1, 0.2));
            }

            // 没有人
            Logger.d(this.getClass(), "#evalSpaceStructure [Depression] : No person");
            result.addScore(Indicator.Depression, 1,
                    hR < 0.1 ? FloatUtils.random(1.0, 1.1) : FloatUtils.random(0.5, 0.6));
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
                result.addFeature(Comment.Stereotype, Tendency.Positive);

                result.addScore(Indicator.SelfConsciousness, 1, FloatUtils.random(0.3, 0.4));
            }

            // 绝对位置判断
            if (houseTHalf && personTHalf) {
                // 整体偏上
                result.addFeature(Comment.Idealization, Tendency.Positive);

                result.addScore(Indicator.Idealism, 1, FloatUtils.random(0.3, 0.4));
                result.addScore(Indicator.Emotion, 1, FloatUtils.random(0.3, 0.4));
            }
            else if (houseBHalf && personBHalf) {
                // 整体偏下
                result.addFeature(Comment.Idealization, Tendency.Negative);

                result.addScore(Indicator.Realism, 1, FloatUtils.random(0.3, 0.4));
                result.addScore(Indicator.Thought, 1, FloatUtils.random(0.3, 0.4));
                result.addScore(Indicator.SenseOfSecurity, -1, FloatUtils.random(0.2, 0.3));
            }
            else {
                result.addScore(Indicator.SelfConsciousness, 1, FloatUtils.random(0.3, 0.4));
            }

            int ha = house.area;
            int pa = person.area;
            if (ha > pa) {
                // 房大
                result.addFeature(Comment.PayAttentionToFamily, Tendency.Positive);

                result.addScore(Indicator.Family, 1, FloatUtils.random(0.3, 0.4));
            }
            else {
                // 人大
                result.addFeature(Comment.SelfDemand, Tendency.Positive);
                result.addFeature(Comment.SelfInflated, Tendency.Positive);
                result.addFeature(Comment.SelfControl, Tendency.Negative);

                result.addScore(Indicator.SelfConsciousness, 1, FloatUtils.random(0.3, 0.4));
            }

            // 房的面积非常小
            double hR = (double)ha / (double)this.spaceLayout.getPaintingArea();
            if (hR < 0.1) {
                result.addFeature(Comment.PayAttentionToFamily, Tendency.Negative);
                result.addScore(Indicator.Family, -1, FloatUtils.random(0.5, 0.6));
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
                result.addFeature(Comment.Stereotype, Tendency.Positive);

                result.addScore(Indicator.SelfConsciousness, 1, FloatUtils.random(0.3, 0.4));
            }

            // 绝对位置判断
            if (treeTHalf && personTHalf) {
                // 整体偏上
                result.addFeature(Comment.Idealization, Tendency.Positive);

                result.addScore(Indicator.Idealism, 1, FloatUtils.random(0.3, 0.4));
                result.addScore(Indicator.Emotion, 1, FloatUtils.random(0.3, 0.4));
            }
            else if (treeBHalf && personBHalf) {
                // 整体偏下
                result.addFeature(Comment.Idealization, Tendency.Negative);

                result.addScore(Indicator.Realism, 1, FloatUtils.random(0.3, 0.4));
                result.addScore(Indicator.Thought, 1, FloatUtils.random(0.3, 0.4));
                result.addScore(Indicator.SenseOfSecurity, -1, FloatUtils.random(0.2, 0.3));
            }
            else {
                result.addScore(Indicator.SelfConsciousness, 1, FloatUtils.random(0.3, 0.4));
            }

            int ta = tree.area;
            int pa = person.area;
            if (ta > pa) {
                // 树大
                result.addFeature(Comment.SocialDemand, Tendency.Positive);

                result.addScore(Indicator.InterpersonalRelation, 1, FloatUtils.random(0.3, 0.4));
            }
            else {
                // 人大
                result.addFeature(Comment.SelfDemand, Tendency.Positive);
                result.addFeature(Comment.SelfInflated, Tendency.Positive);
                result.addFeature(Comment.SelfControl, Tendency.Negative);

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
        int paintingArea = this.spaceLayout.getPaintingBox().calculateArea();
        if (null != person) {
            // 人整体大小
            int personArea = person.area;
            if (((double)personArea / (double)paintingArea) <= tinyRatio) {
                // 人很小
                result.addFeature(Comment.SenseOfSecurity, Tendency.Negative);

                result.addScore(Indicator.SenseOfSecurity, -1, FloatUtils.random(0.3, 0.4));
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
                result.addScore(Indicator.Depression, 1, FloatUtils.random(0.6, 0.7));
                Logger.d(this.getClass(), "#evalSpaceStructure - Space doodles: " + doodles);
            }
            else if (doodles >= 1) {
                // 画面有1/4画幅涂鸦
                result.addScore(Indicator.Depression, 1, FloatUtils.random(0.2, 0.3));
                Logger.d(this.getClass(), "#evalSpaceStructure - Space doodles: " + doodles);
            }

            // 画面稀疏
            if (sparseness >= 4) {
                result.addScore(Indicator.Depression, 1, FloatUtils.random(0.7, 0.8));
                Logger.d(this.getClass(), "#evalSpaceStructure - Space sparseness: " + sparseness);
            }
            else if (sparseness >= 3) {
                result.addScore(Indicator.Depression, 1, FloatUtils.random(0.5, 0.6));
                Logger.d(this.getClass(), "#evalSpaceStructure - Space sparseness: " + sparseness);
            }
            else if (sparseness >= 2) {
                result.addScore(Indicator.Depression, 1, FloatUtils.random(0.3, 0.4));
                Logger.d(this.getClass(), "#evalSpaceStructure - Space sparseness: " + sparseness);
            }
            else if (sparseness >= 1) {
                result.addScore(Indicator.Depression, 1, FloatUtils.random(0.1, 0.2));
                Logger.d(this.getClass(), "#evalSpaceStructure - Space sparseness: " + sparseness);
            }
        }

        return result;
    }

    public EvaluationFeature evalFrameStructure() {
        EvaluationFeature result = new EvaluationFeature();

        FrameStructureDescription description = this.calcFrameStructure(this.spaceLayout.getPaintingBox());
        if (description.isWholeTop()) {
            // 整体顶部
            result.addFeature(Comment.Idealization, Tendency.Positive);
        }
        else if (description.isWholeBottom()) {
            // 整体底部
            result.addFeature(Comment.Instinct, Tendency.Positive);
        }
        else if (description.isWholeLeft()) {
            // 整体左边
            result.addFeature(Comment.Nostalgia, Tendency.Positive);
        }
        else if (description.isWholeRight()) {
            // 整体右边
            result.addFeature(Comment.Future, Tendency.Positive);
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
                result.addFeature(Comment.Creativity, Tendency.Positive);

                result.addScore(Indicator.Creativity, 1, FloatUtils.random(0.7, 0.8));
            }

            // 房屋类型
            if (Label.Bungalow == house.getLabel()) {
                // 平房
                result.addFeature(Comment.Simple, Tendency.Positive);

                result.addScore(Indicator.Simple, 1, FloatUtils.random(0.5, 0.6));
            }
            else if (Label.Villa == house.getLabel()) {
                // 别墅
                result.addFeature(Comment.Luxurious, Tendency.Positive);

                result.addScore(Indicator.EvaluationFromOutside, 1, FloatUtils.random(0.5, 0.6));
            }
            else if (Label.Building == house.getLabel()) {
                // 楼房
                result.addFeature(Comment.Defensiveness, Tendency.Positive);

                result.addScore(Indicator.Realism, 1, FloatUtils.random(0.5, 0.6));
            }
            else if (Label.Fairyland == house.getLabel()) {
                // 童话房
                result.addFeature(Comment.Fantasy, Tendency.Positive);
                result.addFeature(Comment.Childish, Tendency.Normal);

                result.addScore(Indicator.Idealism, 1, FloatUtils.random(0.5, 0.6));
            }
            else if (Label.Temple == house.getLabel()) {
                // 庙宇
                result.addFeature(Comment.Extreme, Tendency.Positive);

                result.addScore(Indicator.Paranoid, 1, FloatUtils.random(0.5, 0.6));
            }
            else if (Label.Grave == house.getLabel()) {
                // 坟墓
                result.addFeature(Comment.WorldWeariness, Tendency.Positive);
            }

            // 房顶
            if (house.hasRoof()) {
                if (house.getRoof().isTextured()) {
                    result.addFeature(Comment.Perfectionism, Tendency.Normal);

                    result.addScore(Indicator.Obsession, 1, FloatUtils.random(0.6, 0.7));
                }

                if (house.getRoofHeightRatio() > 0.5f) {
                    // 房顶高
                    result.addFeature(Comment.Future, Tendency.Positive);

                    result.addScore(Indicator.AchievementMotivation, 1, FloatUtils.random(0.6, 0.7));
                }

                if (house.getRoofAreaRatio() > 0.3f) {
                    // 房顶面积大
                    result.addFeature(Comment.HighPressure, Tendency.Positive);
                    result.addFeature(Comment.Escapism, Tendency.Positive);
                    result.addScore(Indicator.Stress, 1, FloatUtils.random(0.4, 0.5));
                }
            }

            // 天窗
            if (house.hasRoofSkylight()) {
                result.addFeature(Comment.Maverick, Tendency.Positive);

                result.addScore(Indicator.Independence, 1, FloatUtils.random(0.6, 0.7));
            }

            // 烟囱
            if (house.hasChimney()) {
                result.addFeature(Comment.PursueInterpersonalRelationships, Tendency.Positive);

                result.addScore(Indicator.InterpersonalRelation, 1, FloatUtils.random(0.6, 0.7));
            }

            // 门和窗
            if (!house.hasDoor() && !house.hasWindow()) {
                result.addFeature(Comment.EmotionalIndifference, Tendency.Positive);
            }
            else {
                if (house.hasDoor()) {
                    result.addScore(Indicator.InterpersonalRelation, 1, FloatUtils.random(0.6, 0.7));

                    double areaRatio = house.getMaxDoorAreaRatio();
                    if (areaRatio < 0.05f) {
                        result.addFeature(Comment.SocialPowerlessness, Tendency.Positive);
                    }
                    else if (areaRatio >= 0.15f) {
                        result.addFeature(Comment.Dependence, Tendency.Positive);
                    }
                    else if (areaRatio > 0.12f) {
                        result.addFeature(Comment.PursueInterpersonalRelationships, Tendency.Positive);
                    }

                    // 开启的门
                    if (house.hasOpenDoor()) {
                        result.addFeature(Comment.PursueInterpersonalRelationships, Tendency.Positive);

                        result.addScore(Indicator.Extroversion, 1, FloatUtils.random(0.6, 0.7));
                    }
                }

                if (house.hasWindow()) {
                    result.addScore(Indicator.InterpersonalRelation, 1, FloatUtils.random(0.6, 0.7));

                    double areaRatio = house.getMaxWindowAreaRatio();
                    if (areaRatio < 0.03f) {
                        result.addFeature(Comment.SocialPowerlessness, Tendency.Positive);
                    }
                    else if (areaRatio > 0.11f) {
                        result.addFeature(Comment.PursueInterpersonalRelationships, Tendency.Positive);
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
                result.addFeature(Comment.Sensitiveness, Tendency.Positive);
                result.addFeature(Comment.Suspiciousness, Tendency.Positive);

                result.addScore(Indicator.InterpersonalRelation, 1, FloatUtils.random(0.6, 0.7));
            }

            // 小径
            if (house.hasPath()) {
                result.addFeature(Comment.Straightforwardness, Tendency.Positive);

                if (house.hasCurvePath()) {
                    // 弯曲小径
                    result.addFeature(Comment.Vigilance, Tendency.Positive);
                }

                if (house.hasCobbledPath()) {
                    // 石头小径
                    result.addFeature(Comment.Perfectionism, Tendency.Positive);
                }
            }

            // 栅栏
            if (house.hasFence()) {
                result.addFeature(Comment.Defensiveness, Tendency.Positive);
            }


            // 判断房屋是否涂鸦
            if (house.isDoodle()) {
                // 涂鸦的房子
                result.addScore(Indicator.Anxiety, 1, FloatUtils.random(0.7, 0.8));
                Logger.d(this.getClass(), "#evalHouse - House is doodle - " + house.texture.toJSON().toString(4));
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
                result.addFeature(Comment.ExternalPressure, Tendency.Positive);

                result.addScore(Indicator.Stress, 1, FloatUtils.random(0.6, 0.7));
            }
            else if (Label.DeadTree == tree.getLabel()) {
                hasTrunk = true;

                // 枯树
                result.addFeature(Comment.EmotionalDisturbance, Tendency.Positive);

                result.addScore(Indicator.Depression, 1, FloatUtils.random(0.6, 0.7));
                result.addScore(Indicator.Anxiety, 1, FloatUtils.random(0.6, 0.7));

                Logger.d(this.getClass(), "#evalTree [Depression] : dead tree");
            }
            else if (Label.PineTree == tree.getLabel()) {
                hasTrunk = true;

                // 松树
                result.addFeature(Comment.SelfControl, Tendency.Positive);

                result.addScore(Indicator.AchievementMotivation, 1, FloatUtils.random(0.6, 0.7));
                result.addScore(Indicator.SelfControl, 1, FloatUtils.random(0.6, 0.7));
            }
            else if (Label.WillowTree == tree.getLabel()) {
                hasTrunk = true;

                // 柳树
                result.addFeature(Comment.Sensitiveness, Tendency.Positive);
                result.addFeature(Comment.Emotionality, Tendency.Positive);

                result.addScore(Indicator.Emotion, 1, FloatUtils.random(0.6, 0.7));
            }
            else if (Label.CoconutTree == tree.getLabel()) {
                hasTrunk = true;

                // 椰子树
                result.addFeature(Comment.Emotionality, Tendency.Positive);
                result.addFeature(Comment.Creativity, Tendency.Positive);

                result.addScore(Indicator.Emotion, 1, FloatUtils.random(0.6, 0.7));
                result.addScore(Indicator.Creativity, 1, FloatUtils.random(0.6, 0.7));
            }
            else if (Label.Bamboo == tree.getLabel()) {
                hasTrunk = true;

                // 竹子
                result.addFeature(Comment.Independence, Tendency.Positive);

                result.addScore(Indicator.Thought, 1, FloatUtils.random(0.6, 0.7));
                result.addScore(Indicator.Independence, 1, FloatUtils.random(0.6, 0.7));
            }
            else {
                // 常青树
                result.addFeature(Comment.SelfConfidence, Tendency.Positive);
            }

            // 树干
            if (tree.hasTrunk()) {
                hasTrunk = true;
                double ratio = tree.getTrunkWidthRatio();
                Logger.d(this.getClass(), "#evalTree - Tree trunk width ratio: " + ratio);
                if (ratio < 0.16d) {
                    // 细
                    result.addFeature(Comment.Powerlessness, Tendency.Positive);

                    result.addScore(Indicator.Depression, 1, FloatUtils.random(0.3, 0.4));
                    result.addScore(Indicator.SelfEsteem, -1, FloatUtils.random(0.3, 0.4));
                    result.addScore(Indicator.SocialAdaptability, -1, FloatUtils.random(0.3, 0.4));
                }
                else if (ratio >= 0.16d && ratio < 0.3d) {
                    // 粗细适度
                    result.addScore(Indicator.Pessimism, -1, FloatUtils.random(0.3, 0.4));
                }
                else if (ratio >= 0.3d && ratio < 0.7d) {
                    // 粗
                    result.addFeature(Comment.EmotionalStability, Tendency.Positive);

                    result.addScore(Indicator.Confidence, 1, FloatUtils.random(0.2, 0.3));
                    result.addScore(Indicator.Depression, -1, FloatUtils.random(0.4, 0.5));
                }
                else {
                    // 很粗
                    Score score = result.addScore(Indicator.Depression, -1, FloatUtils.random(0.6, 0.7));
                    Logger.d(this.getClass(), "#evalTree [Depression] : " + score.value);
                }
            }

            // 树根
            if (tree.hasRoot()) {
                result.addFeature(Comment.Instinct, Tendency.Positive);

                result.addScore(Indicator.SelfControl, 1, FloatUtils.random(0.6, 0.7));
                result.addScore(Indicator.Paranoid, 1, FloatUtils.random(0.6, 0.7));
            }

            // 树洞
            if (tree.hasHole()) {
                result.addFeature(Comment.Trauma, Tendency.Positive);
                result.addFeature(Comment.EmotionalDisturbance, Tendency.Positive);

                result.addScore(Indicator.Stress, 1, FloatUtils.random(0.6, 0.7));
                result.addScore(Indicator.SenseOfSecurity, -1, FloatUtils.random(0.1, 0.2));
            }

            // 树冠大小
            if (tree.hasCanopy()) {
                result.addFeature(Comment.HighEnergy, Tendency.Positive);
                // 通过评估面积和高度确定树冠大小
                if (tree.getCanopyAreaRatio() >= 0.45) {
                    result.addFeature(Comment.SocialDemand, Tendency.Positive);

                    result.addScore(Indicator.Confidence, 1, FloatUtils.random(0.6, 0.7));
                    result.addScore(Indicator.InterpersonalRelation, 1, FloatUtils.random(0.6, 0.7));
                }
                else if (tree.getCanopyAreaRatio() < 0.2) {
                    result.addFeature(Comment.SelfConfidence, Tendency.Negative);

                    result.addScore(Indicator.Confidence, -1, FloatUtils.random(0.6, 0.7));
                    result.addScore(Indicator.InterpersonalRelation, -1, FloatUtils.random(0.6, 0.7));
                }

                if (tree.getCanopyHeightRatio() >= 0.33) {
                    result.addFeature(Comment.SelfEsteem, Tendency.Positive);

                    result.addScore(Indicator.Confidence, 1, FloatUtils.random(0.6, 0.7));
                    result.addScore(Indicator.InterpersonalRelation, 1, FloatUtils.random(0.6, 0.7));
                }
                else if (tree.getCanopyHeightRatio() < 0.2) {
                    result.addFeature(Comment.SelfEsteem, Tendency.Negative);

                    result.addScore(Indicator.Confidence, -1, FloatUtils.random(0.6, 0.7));
                    result.addScore(Indicator.InterpersonalRelation, -1, FloatUtils.random(0.6, 0.7));
                }

                if (tree.getCanopyAreaRatio() < 0.2 && tree.getCanopyHeightRatio() < 0.3) {
                    result.addFeature(Comment.Childish, Tendency.Positive);
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
                result.addFeature(Comment.PursuitOfAchievement, Tendency.Positive);

                result.addScore(Indicator.AchievementMotivation, 1, FloatUtils.random(0.6, 0.7));

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
                        result.addFeature(Comment.ManyGoals, Tendency.Positive);
                        result.addFeature(Comment.ManyDesires, Tendency.Positive);
                        result.addFeature(Comment.SelfConfidence, Tendency.Positive);
                    }
                    else if (big) {
                        result.addFeature(Comment.ManyGoals, Tendency.Positive);
                    }
                    else if (many) {
                        result.addFeature(Comment.ManyGoals, Tendency.Positive);
                        result.addFeature(Comment.SelfConfidence, Tendency.Negative);
                    }
                    else {
                        result.addFeature(Comment.SelfConfidence, Tendency.Negative);

                        result.addScore(Indicator.Confidence, -1, FloatUtils.random(0.6, 0.7));
                    }
                }
            }

            // 水果数量
            if (tree.numFruits() >= 5 && tree.numFruits() < 8) {
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
            }
        }

        if (!hasTrunk) {
            // 无树干
            result.addFeature(Comment.Introversion, Tendency.Positive);

            result.addScore(Indicator.Introversion, 1, FloatUtils.random(0.3, 0.4));
        }

        return result;
    }

    public EvaluationFeature evalPerson() {
        EvaluationFeature result = new EvaluationFeature();
        if (null == this.painting.getPersons()) {
            return result;
        }

        for (Person person : this.painting.getPersons()) {
            // 人类型
            if (person instanceof StickMan) {
                // 火柴人
                result.addScore(Indicator.Obsession, 1, FloatUtils.random(0.5, 0.6));
                result.addScore(Indicator.Depression, 1, FloatUtils.random(0.4, 0.5));
            }

            // 性别
            if (person.getGender() == Person.Gender.Female) {
                if (this.painting.getAttribute().isMale()) {
                    // 男画女
                    result.addFeature(Comment.SocialPowerlessness, Tendency.Positive);
                    result.addScore(Indicator.Emotion, -1, FloatUtils.random(0.3, 0.4));
                }
            }
            else if (person.getGender() == Person.Gender.Male) {
                if (this.painting.getAttribute().isFemale()) {
                    // 女画男
                    result.addFeature(Comment.SelfDemand, Tendency.Positive);
                    result.addScore(Indicator.SelfConsciousness, 1, FloatUtils.random(0.3, 0.4));
                }
            }

            // 头
            if (person.hasHead()) {
                // 头身比例
                Logger.d(this.getClass(), "#evalPerson - Head height ratio: " + person.getHeadHeightRatio());
                if (person.getHeadHeightRatio() > 0.47d) {
                    // 头大
                    result.addFeature(Comment.SocialAdaptability, Tendency.Negative);

                    result.addScore(Indicator.Impulsion, 1, FloatUtils.random(0.6, 0.7));
                    result.addScore(Indicator.SocialAdaptability, -1, FloatUtils.random(0.6, 0.7));
                }
            }

            // 人物动态、静态判断方式：比较手臂和腿的边界盒形状，边界盒形状越相似则越接近静态，反之为动态。
            // TODO XJW

            // 五官是否完整
            if (!(person.hasEye() && person.hasNose() && person.hasMouth())) {
                // 不完整
                result.addFeature(Comment.SelfConfidence, Tendency.Negative);

                result.addScore(Indicator.Confidence, -1, FloatUtils.random(0.6, 0.7));
            }
            else {
                result.addScore(Indicator.Introversion, 1, FloatUtils.random(0.6, 0.7));
            }

            // 眼
            if (person.hasEye()) {
                // 是否睁开眼
                if (!person.hasOpenEye()) {
                    result.addFeature(Comment.Hostility, Tendency.Positive);
                }

                double ratio = person.getMaxEyeAreaRatio();
                if (ratio > 0.018) {
                    // 眼睛大
                    result.addFeature(Comment.Sensitiveness, Tendency.Positive);
                    result.addFeature(Comment.Alertness, Tendency.Positive);
                }
            }
            else {
                result.addFeature(Comment.IntrapsychicConflict, Tendency.Positive);
            }

            // 眉毛
            if (person.hasEyebrow()) {
                result.addFeature(Comment.AttentionToDetail, Tendency.Positive);

                result.addScore(Indicator.Paranoid, 1, FloatUtils.random(0.6, 0.7));
            }

            // 嘴
            if (person.hasMouth()) {
                if (person.getMouth().isOpen()) {
                    result.addFeature(Comment.LongingForMaternalLove, Tendency.Positive);
                }
                else if (person.getMouth().isStraight()) {
                    result.addFeature(Comment.Strong, Tendency.Positive);

                    result.addScore(Indicator.Constrain, 1, FloatUtils.random(0.6, 0.7));
                }
            }

            // 耳朵
            if (!person.hasEar()) {
                // 没有耳朵
                result.addFeature(Comment.Stubborn, Tendency.Positive);
            }

            // 头发
            if (person.hasHair()) {
                if (person.hasStraightHair()) {
                    // 直发
                    result.addFeature(Comment.Simple, Tendency.Positive);

                    result.addScore(Indicator.Impulsion, 1, FloatUtils.random(0.6, 0.7));
                }
                else if (person.hasShortHair()) {
                    // 短发
                    result.addFeature(Comment.DesireForControl, Tendency.Positive);

                    result.addScore(Indicator.Obsession, 1, FloatUtils.random(0.6, 0.7));
                }
                else if (person.hasCurlyHair()) {
                    // 卷发
                    result.addFeature(Comment.Sentimentality, Tendency.Positive);

                    result.addScore(Indicator.Independence, 1, FloatUtils.random(0.6, 0.7));
                }
                else if (person.hasStandingHair()) {
                    // 竖直头发
                    result.addFeature(Comment.Aggression, Tendency.Positive);

                    result.addScore(Indicator.Hostile, 1, FloatUtils.random(0.6, 0.7));
                }
            }

            // 发饰
            if (person.hasHairAccessory()) {
                result.addFeature(Comment.Narcissism, Tendency.Positive);

                result.addScore(Indicator.Narcissism, 1, FloatUtils.random(0.6, 0.7));
            }

            // 帽子
            if (person.hasCap()) {
                result.addFeature(Comment.Powerlessness, Tendency.Positive);

                result.addScore(Indicator.Constrain, 1, FloatUtils.random(0.6, 0.7));
            }

            // 手臂
            if (person.hasTwoArms() && person.hasBody()) {
                // 计算手臂间距离相对于身体的宽度
                double d = person.calcArmsDistance();
                if (d > person.getBody().getWidth() * 0.5) {
                    // 手臂分开
                    result.addFeature(Comment.Extroversion, Tendency.Positive);
                }

                result.addScore(Indicator.EvaluationFromOutside, 1, FloatUtils.random(0.6, 0.7));
                result.addScore(Indicator.Paranoid, 1, FloatUtils.random(0.6, 0.7));
            }

            // 腿
            if (person.hasTwoLegs()) {
                double d = person.calcLegsDistance();
                Leg thinLeg = person.getThinnestLeg();
                if (null != thinLeg) {
                    if (d < thinLeg.getWidth() * 0.5) {
                        // 腿的距离较近
                        result.addFeature(Comment.Cautious, Tendency.Positive);
                        result.addFeature(Comment.Introversion, Tendency.Positive);
                    }
                }
            }

            // 判断人是否涂鸦
            if (person.texture.isValid()) {
                if (person.texture.max >= 1.0 && person.texture.max < 2.0) {
                    // 判断标准差和层密度
                    if (person.texture.standardDeviation >= 0.42 && person.texture.hierarchy <= 0.05) {
                        // 涂鸦的人
                        result.addScore(Indicator.Anxiety, 1, FloatUtils.random(0.6, 0.7));
                        Logger.d(this.getClass(), "#evalPerson - Person is doodle - \n" + person.texture.toJSON().toString(4));
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
            result.addFeature(Comment.PayAttentionToFamily, Tendency.Positive);
            result.addScore(Indicator.Family, 1, FloatUtils.random(0.7, 0.8));
        }

        if (other.has(Label.Bed)) {
            // 床
            result.addScore(Indicator.Family, -1, FloatUtils.random(0.7, 0.8));
        }

        if (other.has(Label.Sun)) {
            // 太阳
            result.addFeature(Comment.PositiveExpectation, Tendency.Positive);

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
            result.addFeature(Comment.Sentimentality, Tendency.Positive);
        }

        if (other.has(Label.Star)) {
            // 星星
            result.addFeature(Comment.Fantasy, Tendency.Positive);
        }

        if (other.has(Label.Mountain)) {
            // 山
            result.addFeature(Comment.NeedProtection, Tendency.Positive);
            result.addScore(Indicator.SenseOfSecurity, -1, FloatUtils.random(0.3, 0.4));
        }

        if (other.has(Label.Flower)) {
            // 花
            result.addFeature(Comment.Vanity, Tendency.Positive);
            result.addScore(Indicator.EvaluationFromOutside, 1, FloatUtils.random(0.7, 0.8));
            counter += 1;
        }

        if (other.has(Label.Grass)) {
            // 草
            result.addFeature(Comment.Stubborn, Tendency.Positive);
        }

        if (other.has(Label.Sea)) {
            // 海
            result.addFeature(Comment.DesireForFreedom, Tendency.Normal);
        }

        if (other.has(Label.Pool)) {
            // 池塘
            result.addFeature(Comment.Stubborn, Tendency.Normal);
            result.addScore(Indicator.InterpersonalRelation, -1, FloatUtils.random(0.2, 0.3));
        }

        if (other.has(Label.Sunflower)) {
            // 向日葵
            result.addFeature(Comment.Extroversion, Tendency.Positive);
            result.addFeature(Comment.PursuitOfAchievement, Tendency.Normal);
            result.addScore(Indicator.Extroversion, 1, FloatUtils.random(0.7, 0.8));
            counter += 1;
        }

        if (other.has(Label.Mushroom)) {
            // 蘑菇
            result.addScore(Indicator.Stress, 1, FloatUtils.random(0.7, 0.8));
            counter += 1;
        }

        if (other.has(Label.Lotus)) {
            // 莲花
            result.addFeature(Comment.SelfInflated, Tendency.Positive);
            result.addFeature(Comment.Creativity, Tendency.Normal);
            result.addScore(Indicator.Confidence, 1, FloatUtils.random(0.2, 0.3));
            result.addScore(Indicator.MoralSense, 1, FloatUtils.random(0.7, 0.8));
            counter += 1;
        }

        if (other.has(Label.PlumFlower)) {
            // 梅花
            result.addFeature(Comment.SelfEsteem, Tendency.Positive);
            result.addScore(Indicator.SelfEsteem, 1, FloatUtils.random(0.7, 0.8));
            counter += 1;
        }

        if (other.has(Label.Rose)) {
            // 玫瑰
            result.addFeature(Comment.Creativity, Tendency.Positive);
            result.addScore(Indicator.Creativity, 1, FloatUtils.random(0.7, 0.8));
            counter += 1;
        }

        if (other.has(Label.Cloud)) {
            // 云
            result.addFeature(Comment.Imagination, Tendency.Positive);
            result.addScore(Indicator.Optimism, 1, FloatUtils.random(0.7, 0.8));
            result.addScore(Indicator.Idealism, 1, FloatUtils.random(0.7, 0.8));
        }

        if (other.has(Label.Rain)) {
            // 雨
            result.addFeature(Comment.HighPressure, Tendency.Positive);
            result.addScore(Indicator.Stress, 1, FloatUtils.random(0.7, 0.8));
        }

        if (other.has(Label.Rainbow)) {
            // 彩虹
            result.addFeature(Comment.Future, Tendency.Positive);
            result.addScore(Indicator.Simple, 1, FloatUtils.random(0.7, 0.8));
        }

        if (other.has(Label.Torch)) {
            // 火炬
            result.addScore(Indicator.Hostile, 1, FloatUtils.random(0.3, 0.4));
        }

        if (other.has(Label.Bonfire)) {
            // 火堆
            result.addScore(Indicator.Hostile, 1, FloatUtils.random(0.3, 0.4));
        }

        if (other.has(Label.Bird)) {
            // 鸟
            result.addFeature(Comment.DesireForFreedom, Tendency.Positive);
            result.addScore(Indicator.InterpersonalRelation, 1, FloatUtils.random(0.7, 0.8));
            result.addScore(Indicator.DesireForFreedom, 1, FloatUtils.random(0.7, 0.8));
            counter += 1;
        }

        if (other.has(Label.Cat)) {
            // 猫
            result.addFeature(Comment.SocialDemand, Tendency.Positive);
            result.addScore(Indicator.Emotion, 1, FloatUtils.random(0.7, 0.8));
            result.addScore(Indicator.InterpersonalRelation, 1, FloatUtils.random(0.3, 0.4));
            result.addScore(Indicator.Meekness, 1, FloatUtils.random(0.5, 0.6));
            counter += 1;
        }

        if (other.has(Label.Dog)) {
            // 狗
            result.addFeature(Comment.NeedProtection, Tendency.Positive);
            result.addFeature(Comment.SenseOfSecurity, Tendency.Negative);
            result.addScore(Indicator.SenseOfSecurity, -1, FloatUtils.random(0.3, 0.4));
            counter += 1;
        }

        if (other.has(Label.Cow)) {
            // 牛
            result.addScore(Indicator.Struggle, 1, FloatUtils.random(0.7, 0.8));
            counter += 1;
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
            result.addFeature(Comment.DesireForFreedom, Tendency.Positive);
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
            result.addFeature(Comment.DesireForFreedom, Tendency.Positive);
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
            result.addFeature(Comment.Sensitiveness, Tendency.Positive);
            result.addScore(Indicator.Confidence, -1, FloatUtils.random(0.7, 0.8));
            counter += 1;
        }

        if (other.has(Label.Butterfly)) {
            // 蝴蝶
            result.addFeature(Comment.PursuitOfAchievement, Tendency.Positive);
        }

        if (other.has(Label.Tiger)) {
            // 虎
            result.addFeature(Comment.Extroversion, Tendency.Positive);
            result.addFeature(Comment.SelfConfidence, Tendency.Positive);
            result.addScore(Indicator.Extroversion, 1, FloatUtils.random(0.7, 0.8));
            result.addScore(Indicator.Confidence, 1, FloatUtils.random(0.5, 0.6));
            counter += 1;
        }

        if (other.has(Label.Hedgehog)) {
            // 刺猬
            result.addScore(Indicator.SenseOfSecurity, -1, FloatUtils.random(0.7, 0.8));
            counter += 1;
        }

        if (other.has(Label.Snake)) {
            // 蛇
            result.addFeature(Comment.Trauma, Tendency.Positive);
            result.addScore(Indicator.Emotion, -1, FloatUtils.random(0.3, 0.4));
        }

        if (other.has(Label.Dragon)) {
            // 龙
            result.addFeature(Comment.PursuitOfAchievement, Tendency.Positive);
            result.addScore(Indicator.AchievementMotivation, 1, FloatUtils.random(0.7, 0.8));
            counter += 1;
        }

        if (other.has(Label.Watch)) {
            // 表
            result.addScore(Indicator.Anxiety, 1, FloatUtils.random(0.7, 0.8));
        }

        if (other.has(Label.Clock)) {
            // 钟
            result.addScore(Indicator.Anxiety, 1, FloatUtils.random(0.7, 0.8));
        }

        if (other.has(Label.MusicalNotation)) {
            // 音乐符号
            result.addScore(Indicator.Psychosis, 1, FloatUtils.random(0.7, 0.8));
        }

        if (other.has(Label.TV)) {
            // 电视
            result.addFeature(Comment.PayAttentionToFamily, Tendency.Negative);
            result.addScore(Indicator.Family, -1, FloatUtils.random(0.4, 0.5));
        }

        if (other.has(Label.Pole)) {
            // 电线杆
            result.addFeature(Comment.Stubborn, Tendency.Positive);
        }

        if (other.has(Label.Tower)) {
            // 铁塔
            result.addFeature(Comment.Stereotype, Tendency.Positive);
            result.addScore(Indicator.MoralSense, 1, FloatUtils.random(0.7, 0.8));
        }

        if (other.has(Label.Lighthouse)) {
            // 灯塔
            result.addFeature(Comment.Idealization, Tendency.Positive);
        }

        if (other.has(Label.Gun)) {
            // 枪
            result.addScore(Indicator.Attacking, 1, FloatUtils.random(0.7, 0.8));
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
            result.addFeature(Comment.Defensiveness, Tendency.Positive);
            result.addFeature(Comment.NeedProtection, Tendency.Positive);
            result.addScore(Indicator.Pessimism, 1, FloatUtils.random(0.7, 0.8));
        }

        if (other.has(Label.Sandglass)) {
            // 沙漏
            result.addScore(Indicator.Anxiety, 1, FloatUtils.random(0.7, 0.8));
        }

        if (other.has(Label.Kite)) {
            // 风筝
            result.addFeature(Comment.DesireForFreedom, Tendency.Positive);
            result.addScore(Indicator.DesireForFreedom, 1, FloatUtils.random(0.7, 0.8));
            counter += 1;
        }

        if (other.has(Label.Umbrella)) {
            // 伞
            result.addScore(Indicator.SenseOfSecurity, -1, FloatUtils.random(0.7, 0.8));
        }

        if (other.has(Label.Windmill)) {
            // 风车
            result.addFeature(Comment.Fantasy, Tendency.Positive);
            result.addScore(Indicator.Simple, 1, FloatUtils.random(0.7, 0.8));
            counter += 1;
        }

        if (other.has(Label.Flag)) {
            // 旗帜
            result.addScore(Indicator.MoralSense, 1, FloatUtils.random(0.7, 0.8));
        }

        if (other.has(Label.Bridge)) {
            // 桥
            result.addFeature(Comment.PursueInterpersonalRelationships, Tendency.Positive);
            result.addScore(Indicator.InterpersonalRelation, -1, FloatUtils.random(0.7, 0.8));
        }

        if (other.has(Label.Crossroads)) {
            // 十字路口
            result.addScore(Indicator.Anxiety, 1, FloatUtils.random(0.5, 0.6));
        }

        if (other.has(Label.Ladder)) {
            // 梯子
            result.addFeature(Comment.PursuitOfAchievement, Tendency.Positive);
            result.addScore(Indicator.AchievementMotivation, 1, FloatUtils.random(0.7, 0.8));
            result.addScore(Indicator.Anxiety, 1, FloatUtils.random(0.5, 0.6));
        }

        if (other.has(Label.Stairs)) {
            // 楼梯
            result.addFeature(Comment.EnvironmentalAlienation, Tendency.Positive);
            result.addScore(Indicator.AchievementMotivation, 1, FloatUtils.random(0.7, 0.8));
            result.addScore(Indicator.Anxiety, 1, FloatUtils.random(0.5, 0.6));
        }

        if (other.has(Label.Birdcage)) {
            // 鸟笼
            result.addFeature(Comment.DesireForFreedom, Tendency.Positive);
            result.addScore(Indicator.DesireForFreedom, 1, FloatUtils.random(0.7, 0.8));
            counter += 1;
        }

        if (other.has(Label.Car)) {
            // 汽车
            result.addFeature(Comment.Luxurious, Tendency.Positive);
        }

        if (other.has(Label.Boat)) {
            // 船
            result.addFeature(Comment.DesireForFreedom, Tendency.Positive);
            result.addScore(Indicator.DesireForFreedom, 1, FloatUtils.random(0.7, 0.8));
        }

        if (other.has(Label.Airplane)) {
            // 飞机
            result.addFeature(Comment.Escapism, Tendency.Positive);
            result.addScore(Indicator.Independence, 1, FloatUtils.random(0.6, 0.7));
        }

        if (other.has(Label.Bike)) {
            // 自行车
            result.addFeature(Comment.EmotionalDisturbance, Tendency.Positive);
            counter += 1;
        }

        if (other.has(Label.Skull)) {
            // 骷髅
            result.addFeature(Comment.WorldWeariness, Tendency.Positive);
            result.addScore(Indicator.Psychosis, 1, FloatUtils.random(0.7, 0.8));
        }

        if (other.has(Label.Glasses)) {
            // 眼镜
            result.addFeature(Comment.Escapism, Tendency.Positive);
        }

        if (other.has(Label.Swing)) {
            // 秋千
            result.addFeature(Comment.Childish, Tendency.Positive);
        }

        if (counter >= 2) {
            result.addFeature(Comment.Creativity, Tendency.Positive);
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

        for (EvaluationFeature ef : list) {
            Score score = ef.getScore(Indicator.Optimism);
            if (null != score && score.value > 0) {
                optimism += score.weight;
            }

            List<Score> scores = ef.getScores(Indicator.Depression);
            for (Score s : scores) {
                depressionCount += 1;
                depressionValue += s.value;
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
                report = new EvaluationReport(this.painting.getAttribute(), list);
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
            report = new EvaluationReport(this.painting.getAttribute(), results);
        }
        else {
            Logger.w(this.getClass(), "#makeEvaluationReport - Only for test");
            // 仅用于测试
            EvaluationFeature result = new EvaluationFeature();
            int num = Utils.randomInt(3, 5);
            for (int i = 0; i < num; ++i) {
                int index = Utils.randomInt(0, Comment.values().length - 1);
                result.addFeature(Comment.values()[index], Tendency.Positive);
            }
            report = new EvaluationReport(this.painting.getAttribute(), result);
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
