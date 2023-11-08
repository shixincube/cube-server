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

package cube.aigc.psychology.composition;

import cube.aigc.psychology.Painting;
import cube.aigc.psychology.material.Thing;
import cube.vision.BoundingBox;

/**
 * 空间布局。
 */
public class SpaceLayout {

    private float areaRatio;

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
    }

    public float getAreaRatio() {
        return this.areaRatio;
    }
}
