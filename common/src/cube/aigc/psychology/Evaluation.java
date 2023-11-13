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
import cube.aigc.psychology.material.House;
import cube.aigc.psychology.material.Label;
import cube.aigc.psychology.material.Tree;
import cube.vision.BoundingBox;
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
