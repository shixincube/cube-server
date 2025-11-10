/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.util.calc;

import cube.util.Functions;
import cube.vision.BoundingBox;
import cube.vision.Size;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FrameStructureCalculator {

    public FrameStructureCalculator() {
    }

    public FrameStructureDescription calcFrameStructure(Size canvasSize, BoundingBox bbox) {
        int halfHeight = (int) (canvasSize.height * 0.5);
        int halfWidth = (int) (canvasSize.width * 0.5);

        BoundingBox topSpaceBox = new BoundingBox(0, 0,
                canvasSize.width, halfHeight);
        BoundingBox bottomSpaceBox = new BoundingBox(0, halfHeight,
                canvasSize.width, halfHeight);
        BoundingBox leftSpaceBox = new BoundingBox(0, 0,
                halfWidth, canvasSize.height);
        BoundingBox rightSpaceBox = new BoundingBox(halfWidth, 0,
                halfWidth, canvasSize.height);

        FrameStructureDescription fsd = new FrameStructureDescription();

        // 判断上下空间
        int topArea = topSpaceBox.calculateCollisionArea(bbox);
        int bottomArea = bottomSpaceBox.calculateCollisionArea(bbox);
        if (topArea > bottomArea) {
            fsd.addFrameStructure(FrameStructure.TopSpace);
        }
        else {
            fsd.addFrameStructure(FrameStructure.BottomSpace);
        }

        // 判断左右空间
        int leftArea = leftSpaceBox.calculateCollisionArea(bbox);
        int rightArea = rightSpaceBox.calculateCollisionArea(bbox);
        if (leftArea > rightArea) {
            fsd.addFrameStructure(FrameStructure.LeftSpace);
        }
        else {
            fsd.addFrameStructure(FrameStructure.RightSpace);
        }

        // 中间区域
        int paddingWidth = Math.round(((float) canvasSize.width) / 6.0f);
        int paddingHeight = Math.round(((float) canvasSize.height) / 6.0f);
        BoundingBox centerBox = new BoundingBox(paddingWidth, paddingHeight,
                canvasSize.width - paddingWidth * 2,
                canvasSize.height - paddingHeight * 2);
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

        List<AreaDesc> centerList = new ArrayList<>(4);
        centerList.add(new AreaDesc(topLeftArea, FrameStructure.CenterTopLeftSpace));
        centerList.add(new AreaDesc(topRightArea, FrameStructure.CenterTopRightSpace));
        centerList.add(new AreaDesc(bottomLeftArea, FrameStructure.CenterBottomLeftSpace));
        centerList.add(new AreaDesc(bottomRightArea, FrameStructure.CenterBottomRightSpace));

        // 面积从小到达排列
        Collections.sort(centerList, new Comparator<AreaDesc>() {
            @Override
            public int compare(AreaDesc ad1, AreaDesc ad2) {
                return (int)(ad1.area - ad2.area);
            }
        });

        fsd.addFrameStructure(centerList.get(centerList.size() - 1).structure);

        // 判断角落位置
        topLeftBox = new BoundingBox(0, 0, halfWidth, halfHeight);
        topRightBox = new BoundingBox(halfWidth, 0, halfWidth, halfHeight);
        bottomLeftBox = new BoundingBox(0, halfHeight, halfWidth, halfHeight);
        bottomRightBox = new BoundingBox(halfWidth, halfHeight, halfWidth, halfHeight);

        topLeftArea = topLeftBox.calculateCollisionArea(bbox);
        topRightArea = topRightBox.calculateCollisionArea(bbox);
        bottomLeftArea = bottomLeftBox.calculateCollisionArea(bbox);
        bottomRightArea = bottomRightBox.calculateCollisionArea(bbox);

        // 计算变异系数
        double[] data = new double[] { topLeftArea, topRightArea, bottomLeftArea, bottomRightArea };
        if (Functions.sampleStandardDeviation(data) / Functions.mean(data) >= 0.5) {
            // 变异系数大于 0.5 说明明显偏移在角落
            List<AreaDesc> cornerList = new ArrayList<>(4);
            cornerList.add(new AreaDesc(topLeftArea, FrameStructure.TopLeftCorner));
            cornerList.add(new AreaDesc(topRightArea, FrameStructure.TopRightCorner));
            cornerList.add(new AreaDesc(bottomLeftArea, FrameStructure.BottomLeftCorner));
            cornerList.add(new AreaDesc(bottomRightArea, FrameStructure.BottomRightCorner));

            // 面积从小到达排列
            Collections.sort(cornerList, new Comparator<AreaDesc>() {
                @Override
                public int compare(AreaDesc ad1, AreaDesc ad2) {
                    return (int)(ad1.area - ad2.area);
                }
            });

            fsd.addFrameStructure(cornerList.get(cornerList.size() - 1).structure);
        }
        else {
            fsd.addFrameStructure(FrameStructure.NotInCorner);
        }

        return fsd;
    }
}
