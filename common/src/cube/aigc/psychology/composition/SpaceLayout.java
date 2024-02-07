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
            BoundingBox box = thing.getBoundingBox();
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
        this.topMargin = y - paddingH;
        this.rightMargin = painting.getCanvasSize().width - x2 - paddingW;
        this.bottomMargin = painting.getCanvasSize().height - y2 - paddingH;
        this.leftMargin = x - paddingW;
    }

    public BoundingBox getPaintingBox() {
        return this.paintingBox;
    }

    public double getAreaRatio() {
        return this.areaRatio;
    }

    public double[] getMargin() {
        return new double[] {
                this.topMargin,
                this.rightMargin,
                this.bottomMargin,
                this.leftMargin
        };
    }

    public double getTopMargin() {
        return this.topMargin;
    }

    public double getRightMargin() {
        return this.rightMargin;
    }

    public double getBottomMargin() {
        return this.bottomMargin;
    }

    public double getLeftMargin() {
        return this.leftMargin;
    }
}
