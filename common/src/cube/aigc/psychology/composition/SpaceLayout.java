/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology.composition;

import cube.aigc.psychology.Painting;
import cube.aigc.psychology.material.Thing;
import cube.vision.BoundingBox;

/**
 * 空间布局。
 */
public class SpaceLayout {

    private double correctedValue = 0.95f;

    private double topMargin = 0f;
    private double rightMargin = 0f;
    private double bottomMargin = 0f;
    private double leftMargin = 0f;

    private BoundingBox paintingBox;

    private double areaRatio = 0f;

    public SpaceLayout(Painting painting) {
        this.parse(painting);
    }

    private void parse(Painting painting) {
        // 计算所有元素的边界盒
        int x = painting.getCanvasSize().width;
        int y = painting.getCanvasSize().height;
        int x2 = 0;
        int y2 = 0;

        for (Thing thing : painting.getAllThings()) {
            BoundingBox box = thing.boundingBox;
            if (box.x < x) {
                x = box.x;
            }
            if (box.y < y) {
                y = box.y;
            }
            if (box.getX2() > x2) {
                x2 = box.getX2();
            }
            if (box.getY2() > y2) {
                y2 = box.getY2();
            }
        }

        this.paintingBox = new BoundingBox(x, y, (x2 - x), (y2 - y));

        double pW = x2 - x;
        double pH = y2 - y;
        double canvasW = painting.getCanvasSize().width * this.correctedValue;
        double canvasH = painting.getCanvasSize().height * this.correctedValue;

        if (pW > 0 && pH > 0) {
            double area = canvasW * canvasH;
            this.areaRatio = (pW * pH) / area;
        }

        // 计算画面边距
        double paddingW = (painting.getCanvasSize().width - canvasW) * 0.5f;
        double paddingH = (painting.getCanvasSize().height - canvasH) * 0.5f;
        this.topMargin = Math.max(y - paddingH, 0);
        this.rightMargin = Math.max(painting.getCanvasSize().width - x2 - paddingW, 0);
        this.bottomMargin = Math.max(painting.getCanvasSize().height - y2 - paddingH, 0);
        this.leftMargin = Math.max(x - paddingW, 0);
    }

    public BoundingBox getPaintingBox() {
        return this.paintingBox;
    }

    public double getAreaRatio() {
        return this.areaRatio;
    }

    public long getPaintingArea() {
        return Math.round((double)(this.paintingBox.width * this.paintingBox.height) * this.correctedValue);
    }

    public double[] getMargin() {
        return new double[] {
                this.topMargin,
                this.rightMargin,
                this.bottomMargin,
                this.leftMargin
        };
    }

    public int getTopMargin() {
        return (int) Math.round(this.topMargin);
    }

    public int getRightMargin() {
        return (int) Math.round(this.rightMargin);
    }

    public int getBottomMargin() {
        return (int) Math.round(this.bottomMargin);
    }

    public int getLeftMargin() {
        return (int) Math.round(this.leftMargin);
    }
}
