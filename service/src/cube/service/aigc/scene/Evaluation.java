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
import cube.aigc.psychology.composition.FrameStructure;
import cube.aigc.psychology.composition.Score;
import cube.aigc.psychology.composition.SpaceLayout;
import cube.aigc.psychology.composition.Tendency;
import cube.aigc.psychology.material.House;
import cube.aigc.psychology.material.Label;
import cube.aigc.psychology.material.Person;
import cube.aigc.psychology.material.Tree;
import cube.aigc.psychology.material.person.Leg;
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
        if (areaRatio > 0) {
            if (areaRatio >= (2.0f / 3.0f)) {
                result.addFeature(Comment.SelfExistence, Tendency.Positive);

                result.addScore(ScoreIndicator.Extroversion, 1);
                result.addScore(ScoreIndicator.Narcissism, 1);
            }
            else if (areaRatio < (1.0f / 6.0f)) {
                result.addFeature(Comment.SelfEsteem, Tendency.Negative);
                result.addFeature(Comment.SelfConfidence, Tendency.Negative);
                result.addFeature(Comment.SocialAdaptability, Tendency.Negative);

                result.addScore(ScoreIndicator.Introversion, 1);
                result.addScore(ScoreIndicator.Confidence, -1);
                result.addScore(ScoreIndicator.SelfEsteem, -1);
                result.addScore(ScoreIndicator.SocialAdaptability, -1);
            }
            else {
                result.addFeature(Comment.SelfEsteem, Tendency.Normal);
                result.addFeature(Comment.SelfConfidence, Tendency.Positive);

                result.addScore(ScoreIndicator.Confidence, 1);
                result.addScore(ScoreIndicator.SelfEsteem, 1);
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

            result.addScore(ScoreIndicator.Independence, 1);
        }
        else if (this.spaceLayout.getTopMargin() > maxThreshold
                || this.spaceLayout.getRightMargin() > maxThreshold
                || this.spaceLayout.getBottomMargin() > maxThreshold
                || this.spaceLayout.getLeftMargin() > maxThreshold) {
            // 未达边缘
            result.addFeature(Comment.EnvironmentalAlienation, Tendency.Positive);

            result.addScore(ScoreIndicator.Independence, -1);
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
                result.addFeature(Comment.Stereotype, Tendency.Positive);

                result.addScore(ScoreIndicator.SelfConsciousness, 1);
            }
            else {
                // 位置分散
                result.addFeature(Comment.EmotionalStability, Tendency.Negative);
            }

            // 绝对位置判断
            if (houseTHalf && treeTHalf && personTHalf) {
                // 整体偏上
                result.addFeature(Comment.Idealization, Tendency.Positive);

                result.addScore(ScoreIndicator.Idealism, 1);
                result.addScore(ScoreIndicator.Emotion, 1);
            }
            else if (houseBHalf && treeBHalf && personBHalf) {
                // 整体偏下
                result.addFeature(Comment.Idealization, Tendency.Negative);

                result.addScore(ScoreIndicator.Realism, 1);
                result.addScore(ScoreIndicator.Thought, 1);
                result.addScore(ScoreIndicator.SenseOfSecurity, 1);
            }
            else {
                result.addScore(ScoreIndicator.SelfConsciousness, 1);
            }

            // 大小关系
            int ha = house.area;
            int ta = tree.area;
            int pa = person.area;
            if (ha >= ta && ha >= pa) {
                // 房大
                result.addFeature(Comment.PayAttentionToFamily, Tendency.Positive);

                result.addScore(ScoreIndicator.Family, 1);
            }
            if (ta >= ha && ta >= pa) {
                // 树大
                result.addFeature(Comment.SocialDemand, Tendency.Positive);

                result.addScore(ScoreIndicator.InterpersonalRelation, 1);
            }
            if (pa >= ha && pa >= ta) {
                // 人大
                result.addFeature(Comment.SelfDemand, Tendency.Positive);
                result.addFeature(Comment.SelfInflated, Tendency.Positive);
                result.addFeature(Comment.SelfControl, Tendency.Negative);

                result.addScore(ScoreIndicator.SelfConsciousness, 1);
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

                result.addScore(ScoreIndicator.SelfConsciousness, 1);
            }

            // 绝对位置判断
            if (houseTHalf && treeTHalf) {
                // 整体偏上
                result.addFeature(Comment.Idealization, Tendency.Positive);

                result.addScore(ScoreIndicator.Idealism, 1);
                result.addScore(ScoreIndicator.Emotion, 1);
            }
            else if (houseBHalf && treeBHalf) {
                // 整体偏下
                result.addFeature(Comment.Idealization, Tendency.Negative);

                result.addScore(ScoreIndicator.Realism, 1);
                result.addScore(ScoreIndicator.Thought, 1);
                result.addScore(ScoreIndicator.SenseOfSecurity, 1);
            }
            else {
                result.addScore(ScoreIndicator.SelfConsciousness, 1);
            }

            int ha = house.area;
            int ta = tree.area;
            if (ha > ta) {
                // 房大
                result.addFeature(Comment.PayAttentionToFamily, Tendency.Positive);

                result.addScore(ScoreIndicator.Family, 1);
            }
            else {
                // 树大
                result.addFeature(Comment.SocialDemand, Tendency.Positive);

                result.addScore(ScoreIndicator.InterpersonalRelation, 1);
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
                result.addFeature(Comment.Stereotype, Tendency.Positive);

                result.addScore(ScoreIndicator.SelfConsciousness, 1);
            }

            // 绝对位置判断
            if (houseTHalf && personTHalf) {
                // 整体偏上
                result.addFeature(Comment.Idealization, Tendency.Positive);

                result.addScore(ScoreIndicator.Idealism, 1);
                result.addScore(ScoreIndicator.Emotion, 1);
            }
            else if (houseBHalf && personBHalf) {
                // 整体偏下
                result.addFeature(Comment.Idealization, Tendency.Negative);

                result.addScore(ScoreIndicator.Realism, 1);
                result.addScore(ScoreIndicator.Thought, 1);
                result.addScore(ScoreIndicator.SenseOfSecurity, 1);
            }
            else {
                result.addScore(ScoreIndicator.SelfConsciousness, 1);
            }

            int ha = house.area;
            int pa = person.area;
            if (ha > pa) {
                // 房大
                result.addFeature(Comment.PayAttentionToFamily, Tendency.Positive);

                result.addScore(ScoreIndicator.Family, 1);
            }
            else {
                // 人大
                result.addFeature(Comment.SelfDemand, Tendency.Positive);
                result.addFeature(Comment.SelfInflated, Tendency.Positive);
                result.addFeature(Comment.SelfControl, Tendency.Negative);

                result.addScore(ScoreIndicator.SelfConsciousness, 1);
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

                result.addScore(ScoreIndicator.SelfConsciousness, 1);
            }

            // 绝对位置判断
            if (treeTHalf && personTHalf) {
                // 整体偏上
                result.addFeature(Comment.Idealization, Tendency.Positive);

                result.addScore(ScoreIndicator.Idealism, 1);
                result.addScore(ScoreIndicator.Emotion, 1);
            }
            else if (treeBHalf && personBHalf) {
                // 整体偏下
                result.addFeature(Comment.Idealization, Tendency.Negative);

                result.addScore(ScoreIndicator.Realism, 1);
                result.addScore(ScoreIndicator.Thought, 1);
                result.addScore(ScoreIndicator.SenseOfSecurity, 1);
            }
            else {
                result.addScore(ScoreIndicator.SelfConsciousness, 1);
            }

            int ta = tree.area;
            int pa = person.area;
            if (ta > pa) {
                // 树大
                result.addFeature(Comment.SocialDemand, Tendency.Positive);

                result.addScore(ScoreIndicator.InterpersonalRelation, 1);
            }
            else {
                // 人大
                result.addFeature(Comment.SelfDemand, Tendency.Positive);
                result.addFeature(Comment.SelfInflated, Tendency.Positive);
                result.addFeature(Comment.SelfControl, Tendency.Negative);

                result.addScore(ScoreIndicator.SelfConsciousness, 1);
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

                result.addScore(ScoreIndicator.SenseOfSecurity, -1);
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
                result.addFeature(Comment.SelfConfidence, Tendency.Positive);

                result.addScore(ScoreIndicator.Confidence, 1);
            }

            // 房屋类型
            if (Label.Bungalow == house.getLabel()) {
                // 平房
                result.addFeature(Comment.Simple, Tendency.Positive);

                result.addScore(ScoreIndicator.Simple, 1);
            }
            else if (Label.Villa == house.getLabel()) {
                // 别墅
                result.addFeature(Comment.Luxurious, Tendency.Positive);

                result.addScore(ScoreIndicator.EvaluationFromOutside, 1);
            }
            else if (Label.Building == house.getLabel()) {
                // 楼房
                result.addFeature(Comment.Defensiveness, Tendency.Positive);

                result.addScore(ScoreIndicator.Realism, 1);
            }
            else if (Label.Fairyland == house.getLabel()) {
                // 童话房
                result.addFeature(Comment.Fantasy, Tendency.Positive);
                result.addFeature(Comment.Childish, Tendency.Normal);

                result.addScore(ScoreIndicator.Idealism, 1);
            }
            else if (Label.Temple == house.getLabel()) {
                // 庙宇
                result.addFeature(Comment.Extreme, Tendency.Positive);

                result.addScore(ScoreIndicator.Paranoid, 1);
            }
            else if (Label.Grave == house.getLabel()) {
                // 坟墓
                result.addFeature(Comment.WorldWeariness, Tendency.Positive);
            }

            // 房顶
            if (house.hasRoof()) {
                if (house.getRoof().isTextured()) {
                    result.addFeature(Comment.Perfectionism, Tendency.Normal);

                    result.addScore(ScoreIndicator.Obsession, 1);
                }

                if (house.getRoofHeightRatio() > 0.5f) {
                    // 房顶高
                    result.addFeature(Comment.Future, Tendency.Positive);

                    result.addScore(ScoreIndicator.AchievementMotivation, 1);
                }

                if (house.getRoofAreaRatio() > 0.3f) {
                    // 房顶面积大
                    result.addFeature(Comment.HighPressure, Tendency.Positive);
                    result.addFeature(Comment.Escapism, Tendency.Positive);
                }
            }

            // 天窗
            if (house.hasRoofSkylight()) {
                result.addFeature(Comment.Maverick, Tendency.Positive);

                result.addScore(ScoreIndicator.Independence, 1);
            }

            // 烟囱
            if (house.hasChimney()) {
                result.addFeature(Comment.PursueInterpersonalRelationships, Tendency.Positive);

                result.addScore(ScoreIndicator.InterpersonalRelation, 1);
            }

            // 门和窗
            if (!house.hasDoor() && !house.hasWindow()) {
                result.addFeature(Comment.EmotionalIndifference, Tendency.Positive);
            }
            else {
                if (house.hasDoor()) {
                    result.addScore(ScoreIndicator.InterpersonalRelation, 1);

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

                        result.addScore(ScoreIndicator.Extroversion, 1);
                    }
                }

                if (house.hasWindow()) {
                    result.addScore(ScoreIndicator.InterpersonalRelation, 1);

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
                    result.addScore(ScoreIndicator.InterpersonalRelation, -1);
                }
            }

            // 窗帘
            if (house.hasCurtain()) {
                result.addFeature(Comment.Sensitiveness, Tendency.Positive);
                result.addFeature(Comment.Suspiciousness, Tendency.Positive);

                result.addScore(ScoreIndicator.InterpersonalRelation, 1);
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
        }

        return result;
    }

    public EvaluationFeature evalTree() {
        EvaluationFeature result = new EvaluationFeature();
        if (null == this.painting.getTrees()) {
            return result;
        }

        List<Tree> treeList = this.painting.getTrees();
        for (Tree tree : treeList) {
            // 树类型
            if (Label.DeciduousTree == tree.getLabel()) {
                // 落叶树
                result.addFeature(Comment.ExternalPressure, Tendency.Positive);

                result.addScore(ScoreIndicator.Stress, 1);
            }
            else if (Label.DeadTree == tree.getLabel()) {
                // 枯树
                result.addFeature(Comment.Depression, Tendency.Positive);

                result.addScore(ScoreIndicator.Depression, 1);
            }
            else if (Label.PineTree == tree.getLabel()) {
                // 松树
                result.addFeature(Comment.SelfControl, Tendency.Positive);

                result.addScore(ScoreIndicator.AchievementMotivation, 1);
                result.addScore(ScoreIndicator.SelfControl, 1);
            }
            else if (Label.WillowTree == tree.getLabel()) {
                // 柳树
                result.addFeature(Comment.Sensitiveness, Tendency.Positive);
                result.addFeature(Comment.Emotionality, Tendency.Positive);

                result.addScore(ScoreIndicator.Emotion, 1);
            }
            else if (Label.CoconutTree == tree.getLabel()) {
                // 椰子树
                result.addFeature(Comment.Emotionality, Tendency.Positive);
                result.addFeature(Comment.Creativity, Tendency.Positive);

                result.addScore(ScoreIndicator.Emotion, 1);
                result.addScore(ScoreIndicator.Creativity, 1);
            }
            else if (Label.Bamboo == tree.getLabel()) {
                // 竹子
                result.addFeature(Comment.Independence, Tendency.Positive);

                result.addScore(ScoreIndicator.Thought, 1);
                result.addScore(ScoreIndicator.Independence, 1);
            }
            else {
                // 常青树
                result.addFeature(Comment.SelfConfidence, Tendency.Positive);
            }

            // 树干
            if (tree.hasTrunk()) {
                double ratio = tree.getTrunkWidthRatio();
                if (ratio < 0.2f) {
                    // 细
                    result.addFeature(Comment.Powerlessness, Tendency.Positive);

                    result.addScore(ScoreIndicator.Depression, 1);
                    result.addScore(ScoreIndicator.SelfEsteem, -1);
                    result.addScore(ScoreIndicator.SocialAdaptability, -1);
                }
                else if (ratio >= 0.2f && ratio < 0.7f) {
                    // 粗
                    result.addFeature(Comment.EmotionalStability, Tendency.Positive);

                    result.addScore(ScoreIndicator.Confidence, 1);
                }
            }
            else {
                // 无树干
                result.addFeature(Comment.Introversion, Tendency.Positive);

                result.addScore(ScoreIndicator.Depression, 1);
            }

            // 树根
            if (tree.hasRoot()) {
                result.addFeature(Comment.Instinct, Tendency.Positive);

                result.addScore(ScoreIndicator.SelfControl, 1);
                result.addScore(ScoreIndicator.Paranoid, 1);
            }

            // 树洞
            if (tree.hasHole()) {
                result.addFeature(Comment.Trauma, Tendency.Positive);
                result.addFeature(Comment.EmotionalDisturbance, Tendency.Positive);

                result.addScore(ScoreIndicator.Stress, 1);
            }

            // 树冠大小
            if (tree.hasCanopy()) {
                result.addFeature(Comment.HighEnergy, Tendency.Positive);
                // 通过评估面积和高度确定树冠大小
                if (tree.getCanopyAreaRatio() >= 0.45) {
                    result.addFeature(Comment.SocialDemand, Tendency.Positive);

                    result.addScore(ScoreIndicator.Confidence, 1);
                    result.addScore(ScoreIndicator.InterpersonalRelation, 1);
                }
                else if (tree.getCanopyAreaRatio() < 0.2) {
                    result.addFeature(Comment.SelfConfidence, Tendency.Negative);

                    result.addScore(ScoreIndicator.Confidence, -1);
                    result.addScore(ScoreIndicator.InterpersonalRelation, -1);
                }

                if (tree.getCanopyHeightRatio() >= 0.33) {
                    result.addFeature(Comment.SelfEsteem, Tendency.Positive);

                    result.addScore(ScoreIndicator.Confidence, 1);
                    result.addScore(ScoreIndicator.InterpersonalRelation, 1);
                }
                else if (tree.getCanopyHeightRatio() < 0.2) {
                    result.addFeature(Comment.SelfEsteem, Tendency.Negative);

                    result.addScore(ScoreIndicator.Confidence, -1);
                    result.addScore(ScoreIndicator.InterpersonalRelation, -1);
                }

                if (tree.getCanopyAreaRatio() < 0.2 && tree.getCanopyHeightRatio() < 0.3) {
                    result.addFeature(Comment.Childish, Tendency.Positive);
                }
            }
            else {
                // 安全感缺失
                result.addFeature(Comment.SenseOfSecurity, Tendency.Negative);

                result.addScore(ScoreIndicator.SenseOfSecurity, -1);
            }

            // 果实
            if (tree.hasFruit()) {
                result.addFeature(Comment.PursuitOfAchievement, Tendency.Positive);

                result.addScore(ScoreIndicator.AchievementMotivation, 1);

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

                        result.addScore(ScoreIndicator.Confidence, -1);
                    }
                }
            }
        }

        return result;
    }

    public EvaluationFeature evalPerson() {
        EvaluationFeature result = new EvaluationFeature();
        if (null == this.painting.getPersons()) {
            return result;
        }

        for (Person person : this.painting.getPersons()) {
            // 头
            if (person.hasHead()) {
                // 头身比例
                if (person.getHeadHeightRatio() > 0.25) {
                    // 头大
                    result.addFeature(Comment.SocialAdaptability, Tendency.Negative);

                    result.addScore(ScoreIndicator.Impulsion, 1);
                    result.addScore(ScoreIndicator.SocialAdaptability, -1);
                }
            }

            // 人物动态、静态判断方式：比较手臂和腿的边界盒形状，边界盒形状越相似则越接近静态，反之为动态。
            // TODO XJW

            // 五官是否完整
            if (!(person.hasEye() && person.hasNose() && person.hasMouth())) {
                // 不完整
                result.addFeature(Comment.SelfConfidence, Tendency.Negative);

                result.addScore(ScoreIndicator.Confidence, -1);
            }
            else {
                result.addScore(ScoreIndicator.Introversion, 1);
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

                result.addScore(ScoreIndicator.Paranoid, 1);
            }

            // 嘴
            if (person.hasMouth()) {
                if (person.getMouth().isOpen()) {
                    result.addFeature(Comment.LongingForMaternalLove, Tendency.Positive);
                }
                else if (person.getMouth().isStraight()) {
                    result.addFeature(Comment.Strong, Tendency.Positive);

                    result.addScore(ScoreIndicator.Constrain, 1);
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

                    result.addScore(ScoreIndicator.Impulsion, 1);
                }
                else if (person.hasShortHair()) {
                    // 短发
                    result.addFeature(Comment.DesireForControl, Tendency.Positive);

                    result.addScore(ScoreIndicator.Obsession, 1);
                }
                else if (person.hasCurlyHair()) {
                    // 卷发
                    result.addFeature(Comment.Sentimentality, Tendency.Positive);

                    result.addScore(ScoreIndicator.Independence, 1);
                }
                else if (person.hasStandingHair()) {
                    // 竖直头发
                    result.addFeature(Comment.Aggression, Tendency.Positive);

                    result.addScore(ScoreIndicator.Hostile, 1);
                }
            }

            // 发饰
            if (person.hasHairAccessory()) {
                result.addFeature(Comment.Narcissism, Tendency.Positive);

                result.addScore(ScoreIndicator.Narcissism, 1);
            }

            // 帽子
            if (person.hasCap()) {
                result.addFeature(Comment.Powerlessness, Tendency.Positive);

                result.addScore(ScoreIndicator.Constrain, 1);
            }

            // 手臂
            if (person.hasTwoArms() && person.hasBody()) {
                // 计算手臂间距离相对于身体的宽度
                double d = person.calcArmsDistance();
                if (d > person.getBody().getWidth() * 0.5) {
                    // 手臂分开
                    result.addFeature(Comment.Extroversion, Tendency.Positive);
                }

                result.addScore(ScoreIndicator.EvaluationFromOutside, 1);
                result.addScore(ScoreIndicator.Paranoid, 1);
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
        }

        return result;
    }

    public EvaluationFeature evalOthers() {
        EvaluationFeature result = new EvaluationFeature();

        if (this.painting.hasSun()) {
            // 太阳
            result.addFeature(Comment.PositiveExpectation, Tendency.Positive);

            result.addScore(ScoreIndicator.Optimism, 1);
        }

        if (this.painting.hasMoon()) {
            // 月亮
            result.addFeature(Comment.Sentimentality, Tendency.Positive);
        }

        if (this.painting.hasCloud()) {
            // 云
            result.addFeature(Comment.Imagination, Tendency.Positive);

            result.addScore(ScoreIndicator.Optimism, 1);
            result.addScore(ScoreIndicator.Idealism, 1);
        }

        if (this.painting.hasAnimal()) {
            // 动物
            result.addFeature(Comment.DelicateEmotions, Tendency.Positive);

            result.addScore(ScoreIndicator.InterpersonalRelation, 1);

            if (this.painting.hasBird()) {
                // 鸟
                result.addFeature(Comment.DesireForFreedom, Tendency.Positive);
            }
            else if (this.painting.hasDog()) {
                // 狗
                result.addFeature(Comment.NeedProtection, Tendency.Positive);
            }
            else if (this.painting.hasCat()) {
                // 猫
                result.addFeature(Comment.SocialDemand, Tendency.Positive);
            }
        }

        if (this.painting.hasGrass()) {
            // 草
            result.addFeature(Comment.Stubborn, Tendency.Positive);
        }

        if (this.painting.hasFlower()) {
            // 花
            result.addFeature(Comment.Vanity, Tendency.Positive);

            result.addScore(ScoreIndicator.EvaluationFromOutside, 1);
        }

        if (this.painting.hasMountain()) {
            // 山
            result.addFeature(Comment.NeedProtection, Tendency.Positive);

            result.addScore(ScoreIndicator.SenseOfSecurity, 1);
        }

        int numMaterials = this.painting.numStars()
                + this.painting.numFlowers() + this.painting.numGrasses()
                + this.painting.numMountains() + this.painting.numAnimals();
        if (numMaterials >= 5) {
            // 绘制的元素多
            result.addFeature(Comment.Creativity, Tendency.Positive);
        }

        return result;
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
                report = new EvaluationReport(this.painting.getAttribute(), new ArrayList<>());
                return report;
            }

            List<EvaluationFeature> results = new ArrayList<>();
            results.add(this.evalSpaceStructure());
            results.add(this.evalFrameStructure());
            results.add(this.evalHouse());
            results.add(this.evalTree());
            results.add(this.evalPerson());
            results.add(this.evalOthers());
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
