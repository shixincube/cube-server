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

package cube.aigc.psychology.algorithm;

import cube.aigc.psychology.Painting;

public class PaintingConfidence {

    public final static int LEVEL_HIGHER = 5;

    public final static int LEVEL_HIGH = 4;

    public final static int LEVEL_NORMAL = 3;

    public final static int LEVEL_LOW = 2;

    public final static int LEVEL_LOWER = 1;

    private int confidenceLevel;

    public PaintingConfidence(Painting painting) {
        this.build(painting);
    }

    public PaintingConfidence(int level) {
        this.confidenceLevel = level;
    }

    public int getConfidenceLevel() {
        return this.confidenceLevel;
    }

    private void build(Painting painting) {
        if (!painting.isValid()) {
            this.confidenceLevel = LEVEL_LOWER;
            return;
        }

        if (painting.hasHouse() && painting.hasTree() && painting.hasPerson()) {
            this.confidenceLevel = LEVEL_HIGH;

            if (painting.getHouse().numComponents() >= 2
                    && painting.getTree().numComponents() >= 2
                    && painting.getPerson().numComponents() >= 2) {
                this.confidenceLevel = LEVEL_HIGHER;
            }
            else if (painting.getHouse().numComponents() > 3
                    || painting.getTree().numComponents() > 3
                    || painting.getPerson().numComponents() > 3) {
                this.confidenceLevel = LEVEL_HIGHER;
            }
            else {
                if (painting.getOther().getAll().size() > 4) {
                    this.confidenceLevel = LEVEL_HIGHER;
                }
            }
        }
        else if ((painting.hasHouse() && painting.hasTree()) ||
                (painting.hasTree() && painting.hasPerson()) ||
                (painting.hasPerson() && painting.hasHouse())) {
            this.confidenceLevel = LEVEL_NORMAL;
        }
        else {
            this.confidenceLevel = LEVEL_LOW;
        }
    }
}
