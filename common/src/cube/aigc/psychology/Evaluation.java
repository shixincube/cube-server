/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 Cube Team.
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

package cube.aigc.psychology;

import cube.aigc.psychology.composition.FrameStructure;
import cube.aigc.psychology.composition.Score;
import cube.aigc.psychology.composition.SpaceLayout;
import cube.aigc.psychology.material.*;
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

    public Evaluation(Painting painting) {
        this.painting = painting;
        this.canvasSize = painting.getCanvasSize();
        this.spaceLayout = new SpaceLayout(painting);
    }

    public List<Result> evalSpaceStructure() {
        List<Result> list = new ArrayList<>();

        // 画面大小比例
        double areaRatio = this.spaceLayout.getAreaRatio();
        if (areaRatio > 0) {
            if (areaRatio >= (2.0f / 3.0f)) {
                list.add(new Result(Word.SelfExistence, Score.High));
            }
            else if (areaRatio < (1.0f / 6.0f)) {
                list.add(new Result(Word.SelfEsteem, Score.Low));
                list.add(new Result(Word.SelfConfidence, Score.Low));
                list.add(new Result(Word.Adaptability, Score.Low));
            }
            else {
                list.add(new Result(Word.SelfEsteem, Score.Medium));
                list.add(new Result(Word.SelfConfidence, Score.High));
            }
        }

        // 空间构图
        double minThreshold = this.canvasSize.width * 0.025f;
        double maxThreshold = this.canvasSize.width * 0.15f;
        if (this.spaceLayout.getTopMargin() < minThreshold
                || this.spaceLayout.getRightMargin() < minThreshold
                || this.spaceLayout.getBottomMargin() < minThreshold
                || this.spaceLayout.getLeftMargin() < minThreshold) {
            list.add(new Result(Word.EnvironmentalDependence, Score.High));
        }
        else if (this.spaceLayout.getTopMargin() > maxThreshold
                || this.spaceLayout.getRightMargin() > maxThreshold
                || this.spaceLayout.getBottomMargin() > maxThreshold
                || this.spaceLayout.getLeftMargin() > maxThreshold) {
            list.add(new Result(Word.EnvironmentalAlienation, Score.High));
        }

        // 房、树、人之间的空间关系
        // 中线
        double banding = ((double) this.painting.getCanvasSize().height) * 0.167;
        double bl = (this.painting.getCanvasSize().height - (int) banding) * 0.5;
        int evalRange = (int) Math.round(banding * 0.5);

        House house = this.painting.getHouse();
        Tree tree = this.painting.getTree();
        Person person = this.painting.getPerson();
        if (null != house && null != tree && null != person) {
            // 位置关系
            Point hc = house.getBoundingBox().getCenterPoint();
            Point tc = tree.getBoundingBox().getCenterPoint();
            Point pc = person.getBoundingBox().getCenterPoint();
            if (Math.abs(hc.y - tc.y) < evalRange && Math.abs(hc.y - pc.y) < evalRange) {
                // 基本在一个水平线上
                list.add(new Result(Word.Stereotype, Score.High));
            }
            else {
                // 位置分散
                list.add(new Result(Word.EmotionalStability, Score.Low));
            }

            // 大小关系
            int ha = house.getBoundingBox().calculateArea();
            int ta = tree.getBoundingBox().calculateArea();
            int pa = person.getBoundingBox().calculateArea();
            if (ha >= ta && ha >= pa) {
                // 房大
                list.add(new Result(Word.PayAttentionToFamily, Score.High));
            }
            else if (ta >= ha && ta >= pa) {
                // 树大
                list.add(new Result(Word.SocialDemand, Score.High));
            }
            else if (pa >= ha && pa >= ta) {
                // 人大
                list.add(new Result(Word.IndividualDemand, Score.High));
            }
        }
        else if (null != house && null != tree) {
            Point hc = house.getBoundingBox().getCenterPoint();
            Point tc = tree.getBoundingBox().getCenterPoint();
            if (Math.abs(hc.y - tc.y) < evalRange) {
                // 基本在一个水平线上
                list.add(new Result(Word.Stereotype, Score.High));
            }

            int ha = house.getBoundingBox().calculateArea();
            int ta = tree.getBoundingBox().calculateArea();
            if (ha > ta) {
                // 房大
                list.add(new Result(Word.PayAttentionToFamily, Score.High));
            }
            else {
                // 树大
                list.add(new Result(Word.SocialDemand, Score.High));
            }
        }
        else if (null != house && null != person) {
            Point hc = house.getBoundingBox().getCenterPoint();
            Point pc = person.getBoundingBox().getCenterPoint();
            if (Math.abs(hc.y - pc.y) < evalRange) {
                // 基本在一个水平线上
                list.add(new Result(Word.Stereotype, Score.High));
            }

            int ha = house.getBoundingBox().calculateArea();
            int pa = person.getBoundingBox().calculateArea();
            if (ha > pa) {
                // 房大
                list.add(new Result(Word.PayAttentionToFamily, Score.High));
            }
            else {
                // 人大
                list.add(new Result(Word.IndividualDemand, Score.High));
            }
        }
        else if (null != tree && null != person) {
            Point tc = tree.getBoundingBox().getCenterPoint();
            Point pc = person.getBoundingBox().getCenterPoint();
            if (Math.abs(tc.y - pc.y) < evalRange) {
                // 基本在一个水平线上
                list.add(new Result(Word.Stereotype, Score.High));
            }

            int ta = tree.getBoundingBox().calculateArea();
            int pa = person.getBoundingBox().calculateArea();
            if (ta > pa) {
                // 树大
                list.add(new Result(Word.SocialDemand, Score.High));
            }
            else {
                // 人大
                list.add(new Result(Word.IndividualDemand, Score.High));
            }
        }

        return list;
    }

    public List<Result> evalFrameStructure() {
        List<Result> list = new ArrayList<>();

        FrameStructureDescription description = this.calcFrameStructure(this.spaceLayout.getPaintingBox());
        if (description.isWholeTop()) {
            // 整体顶部
            list.add(new Result(Word.Idealization, Score.High));
        }
        else if (description.isWholeBottom()) {
            // 整体底部
            list.add(new Result(Word.Actualization, Score.High));
        }
        else if (description.isWholeLeft()) {
            // 整体左边
            list.add(new Result(Word.Nostalgia, Score.High));
        }
        else if (description.isWholeRight()) {
            // 整体右边
            list.add(new Result(Word.Future, Score.High));
        }

        return list;
    }

    public List<Result> evalHouse() {
        List<Result> list = new ArrayList<>();

        List<House> houseList = this.painting.getHouses();
        for (House house : houseList) {
            // 立体感
            if (house.hasSidewall()) {
                list.add(new Result(Word.SelfConfidence, Score.High));
            }

            // 房屋类型
            if (Label.Bungalow == house.getLabel()) {
                // 平房
                list.add(new Result(Word.Simple, Score.High));
            }
            else if (Label.Villa == house.getLabel()) {
                // 别墅
                list.add(new Result(Word.Luxurious, Score.High));
            }
            else if (Label.Building == house.getLabel()) {
                // 楼房
                list.add(new Result(Word.Defensiveness, Score.High));
            }
            else if (Label.Fairyland == house.getLabel()) {
                // 童话房
                list.add(new Result(Word.Fantasy, Score.High));
                list.add(new Result(Word.Childish, Score.Medium));
            }
            else if (Label.Temple == house.getLabel()) {
                // 庙宇
                list.add(new Result(Word.Extreme, Score.High));
            }
            else if (Label.Grave == house.getLabel()) {
                // 坟墓
                list.add(new Result(Word.WorldWeariness, Score.High));
            }

            // 房顶
            if (house.hasRoof()) {
                if (house.getRoof().isTextured()) {
                    list.add(new Result(Word.Perfectionism, Score.Medium));
                }

                if (house.getRoofHeightRatio() > 0.5f) {
                    // 房顶高
                    list.add(new Result(Word.Future, Score.High));
                }

                if (house.getRoofAreaRatio() > 0.3f) {
                    // 房顶面积大
                    list.add(new Result(Word.HighPressure, Score.High));
                    list.add(new Result(Word.Escapism, Score.High));
                }
            }

            // 天窗
            if (house.hasRoofSkylight()) {
                list.add(new Result(Word.Maverick, Score.High));
            }

            // 烟囱
            if (house.hasChimney()) {
                list.add(new Result(Word.PursueInterpersonalRelationships, Score.High));
            }

            // 门和窗
            if (!house.hasDoor() && !house.hasWindow()) {
                list.add(new Result(Word.EmotionalIndifference, Score.High));
            }
            else {
                if (house.hasDoor()) {
                    double areaRatio = house.getMaxDoorAreaRatio();
                    if (areaRatio < 0.05f) {
                        list.add(new Result(Word.SocialPowerlessness, Score.High));
                    }
                    else if (areaRatio >= 0.15f) {
                        list.add(new Result(Word.Dependence, Score.High));
                    }
                    else if (areaRatio > 0.12f) {
                        list.add(new Result(Word.PursueInterpersonalRelationships, Score.High));
                    }

                    // 开启的门
                    if (house.hasOpenDoor()) {
                        list.add(new Result(Word.PursueInterpersonalRelationships, Score.High));
                    }
                }

                if (house.hasWindow()) {
                    double areaRatio = house.getMaxWindowAreaRatio();
                    if (areaRatio < 0.03f) {
                        list.add(new Result(Word.SocialPowerlessness, Score.High));
                    }
                    else if (areaRatio > 0.11f) {
                        list.add(new Result(Word.PursueInterpersonalRelationships, Score.High));
                    }
                }
            }

            // 窗帘
            if (house.hasCurtain()) {
                list.add(new Result(Word.Sensitiveness, Score.High));
                list.add(new Result(Word.Suspiciousness, Score.High));
            }

            // 小径
            if (house.hasPath()) {
                list.add(new Result(Word.Straightforwardness, Score.High));

                if (house.hasCurvePath()) {
                    // 弯曲小径
                    list.add(new Result(Word.Vigilance, Score.High));
                }

                if (house.hasCobbledPath()) {
                    // 石头小径
                    list.add(new Result(Word.Perfectionism, Score.High));
                }
            }

            // 栅栏
            if (house.hasFence()) {
                list.add(new Result(Word.Defensiveness, Score.High));
            }
        }

        return list;
    }

    public List<Result> evalTree() {
        List<Result> list = new ArrayList<>();

        List<Tree> treeList = this.painting.getTrees();
        for (Tree tree : treeList) {
            // 树类型
            if (Label.DeciduousTree == tree.getLabel()) {
                // 落叶树
                list.add(new Result(Word.ExternalPressure, Score.High));
            }
            else if (Label.DeadTree == tree.getLabel()) {
                // 枯树
                list.add(new Result(Word.Depression, Score.High));
            }
            else if (Label.PineTree == tree.getLabel()) {
                // 松树
                list.add(new Result(Word.SelfControl, Score.High));
            }
            else if (Label.WillowTree == tree.getLabel()) {
                // 柳树
                list.add(new Result(Word.Sensitiveness, Score.High));
                list.add(new Result(Word.Emotionality, Score.High));
            }
            else if (Label.CoconutTree == tree.getLabel()) {
                // 椰子树
                list.add(new Result(Word.Emotionality, Score.High));
                list.add(new Result(Word.Creativity, Score.High));
            }
            else if (Label.Bamboo == tree.getLabel()) {
                // 竹子
                list.add(new Result(Word.Independent, Score.High));
            }
            else {
                // 常青树
                list.add(new Result(Word.SelfConfidence, Score.High));
            }

            // 树干
            if (tree.hasTrunk()) {
                double ratio = tree.getTrunkWidthRatio();
                if (ratio < 0.2f) {
                    // 细
                    list.add(new Result(Word.SelfPowerlessness, Score.High));
                }
                else if (ratio >= 0.2f && ratio < 0.7f) {
                    // 粗
                    list.add(new Result(Word.EmotionalStability, Score.High));
                }
            }
            else {
                list.add(new Result(Word.Introversion, Score.High));
            }

            // 树根
            if (tree.hasRoot()) {
                list.add(new Result(Word.Instinct, Score.High));
            }

            // 树洞
            if (tree.hasHole()) {
                list.add(new Result(Word.Trauma, Score.High));
                list.add(new Result(Word.EmotionalDisturbance, Score.High));
            }

            // 树冠大小
            if (tree.hasCanopy()) {
                list.add(new Result(Word.HighEnergy, Score.High));
                // 通过评估面积和高度确定树冠大小
                if (tree.getCanopyAreaRatio() >= 0.45) {
                    list.add(new Result(Word.SocialDemand, Score.High));
                }
                else if (tree.getCanopyAreaRatio() < 0.2) {
                    list.add(new Result(Word.SelfConfidence, Score.Low));
                }

                if (tree.getCanopyHeightRatio() >= 0.33) {
                    list.add(new Result(Word.SelfFeeling, Score.High));
                }
                else if (tree.getCanopyHeightRatio() < 0.2) {
                    list.add(new Result(Word.SelfFeeling, Score.Low));
                }

                if (tree.getCanopyAreaRatio() < 0.2 && tree.getCanopyHeightRatio() < 0.3) {
                    list.add(new Result(Word.Childish, Score.High));
                }
            }
            else {
                // 安全感缺失
                list.add(new Result(Word.SenseOfSecurity, Score.Low));
            }

            // 果实
            if (tree.hasFruit()) {
                list.add(new Result(Word.PursuitOfAchievement, Score.High));

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
                        list.add(new Result(Word.ManyGoals, Score.High));
                        list.add(new Result(Word.ManyDesires, Score.High));
                        list.add(new Result(Word.SelfConfidence, Score.High));
                    }
                    else if (big) {
                        list.add(new Result(Word.ManyGoals, Score.High));
                    }
                    else if (many) {
                        list.add(new Result(Word.ManyGoals, Score.High));
                        list.add(new Result(Word.SelfConfidence, Score.Low));
                    }
                    else {
                        list.add(new Result(Word.SelfConfidence, Score.Low));
                    }
                }
            }
        }

        return list;
    }

    public List<Result> evalPerson() {
        List<Result> list = new ArrayList<>();

        for (Person person : this.painting.getPersons()) {
            // 头
            if (person.hasHead()) {
                // 头身比例
                if (person.getHeadHeightRatio() > 0.25) {
                    list.add(new Result(Word.SocialAdaptability, Score.Low));
                }
            }

            // 人物动态、静态判断方式：比较手臂和腿的边界盒形状，边界盒形状越相似则越接近静态，反之为动态。
            // TODO XJW

            // 五官是否完整
            if (!(person.hasEye() && person.hasNose() && person.hasMouth())) {
                list.add(new Result(Word.SelfConfidence, Score.Low));
            }

            // 眼
            if (person.hasEye()) {
                // 是否睁开眼
                if (!person.hasOpenEye()) {
                    list.add(new Result(Word.Hostility, Score.High));
                }

                double ratio = person.getMaxEyeAreaRatio();
                if (ratio > 0.018) {
                    // 眼睛大
                    list.add(new Result(Word.Sensitiveness, Score.High));
                    list.add(new Result(Word.Vigilance, Score.High));
                }
            }
            else {
                list.add(new Result(Word.IntrapsychicConflict, Score.High));
            }

            // 眉毛
            if (person.hasEyebrow()) {
                list.add(new Result(Word.AttentionToDetail, Score.High));
            }

            // 嘴
            if (person.hasMouth()) {
                if (person.getMouth().isOpen()) {
                    list.add(new Result(Word.LongingForMaternalLove, Score.High));
                }
                else if (person.getMouth().isStraight()) {
                    list.add(new Result(Word.Strong, Score.High));
                }
            }

            // 耳朵
            if (!person.hasEar()) {
                // 没有耳朵
                list.add(new Result(Word.Stubborn, Score.High));
            }

            // 头发
            if (person.hasHair()) {
                if (person.hasStraightHair()) {
                    // 直发
                    list.add(new Result(Word.Simple, Score.High));
                }
                else if (person.hasShortHair()) {
                    // 短发
                    list.add(new Result(Word.DesireForControl, Score.High));
                }
                else if (person.hasCurlyHair()) {
                    // 卷发
                    list.add(new Result(Word.Sentimentality, Score.High));
                }
                else if (person.hasStandingHair()) {
                    // 竖直头发
                    list.add(new Result(Word.Aggression, Score.High));
                }
            }

            // 发饰
            if (person.hasHairAccessory()) {
                list.add(new Result(Word.Narcissism, Score.High));
            }

            // 帽子
            if (person.hasCap()) {
                list.add(new Result(Word.SelfPowerlessness, Score.High));
            }

            // 手臂
            if (person.hasTwoArms() && person.hasBody()) {
                // 计算手臂间距离相对于身体的宽度
                double d = person.calcArmsDistance();
                if (d > person.getBody().getWidth() * 0.5) {
                    // 手臂分开
                    list.add(new Result(Word.Extroversion, Score.High));
                }
            }

            // 腿
            if (person.hasTwoLegs()) {
                double d = person.calcLegsDistance();
                Leg thinLeg = person.getThinnestLeg();
                if (null != thinLeg) {
                    if (d < thinLeg.getWidth() * 0.5) {
                        // 腿的距离较近
                        list.add(new Result(Word.Cautious, Score.High));
                        list.add(new Result(Word.Introversion, Score.High));
                    }
                }
            }
        }

        return list;
    }

    public List<Result> evalOthers() {
        List<Result> list = new ArrayList<>();

        if (this.painting.hasSun()) {
            // 太阳
            list.add(new Result(Word.PositiveExpectation, Score.High));
        }

        if (this.painting.hasMoon()) {
            // 月亮
            list.add(new Result(Word.Sentimentality, Score.High));
        }

        if (this.painting.hasCloud()) {
            // 云
            list.add(new Result(Word.Imagination, Score.High));
        }

        if (this.painting.hasAnimal()) {
            // 动物
            list.add(new Result(Word.DelicateEmotions, Score.High));
            if (this.painting.hasBird()) {
                // 鸟
                list.add(new Result(Word.DesireForFreedom, Score.High));
            }
            else if (this.painting.hasDog()) {
                // 狗
                list.add(new Result(Word.Spirituality, Score.High));
            }
            else if (this.painting.hasCat()) {
                // 猫
                list.add(new Result(Word.SocialDemand, Score.High));
            }
        }

        if (this.painting.hasGrass()) {
            // 草
            list.add(new Result(Word.Stubborn, Score.High));
        }

        if (this.painting.hasFlower()) {
            // 花
            list.add(new Result(Word.Vanity, Score.High));
        }

        if (this.painting.hasMountain()) {
            // 山
            list.add(new Result(Word.NeedProtection, Score.High));
        }

        int numMaterials = this.painting.numStars()
                + this.painting.numFlowers() + this.painting.numGrasses()
                + this.painting.numMountains() + this.painting.numAnimals();
        if (numMaterials >= 5) {
            // 绘制的元素多
            list.add(new Result(Word.Creativity, Score.High));
        }

        return list;
    }

    public EvaluationReport makeReport(List<Result> resultList) {
        return null;
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

    public class Result {

        public Word word;

        public Score score;

        public Result(Word word, Score score) {
            this.word = word;
            this.score = score;
        }
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
