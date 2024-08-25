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

public enum BigFivePanorama {

    /**
     * 宜人性/外向性。
     */
    ObligingnessExtraversion(-1, -1),

    /**
     * 外向性/进取性。
     */
    ExtraversionAchievement(-1, 1),

    /**
     * 进去性/尽责性。
     */
    AchievementConscientiousness(1, 1),

    /**
     * 尽责性/宜人性。
     */
    ConscientiousnessObligingness(1, -1)

    ;

    public final int xOri;

    public final int yOri;

    BigFivePanorama(int xOri, int yOri) {
        this.xOri = xOri;
        this.yOri = yOri;
    }

    public static BigFivePanorama parse(double x, double y) {
        for (BigFivePanorama bfp : BigFivePanorama.values()) {
            if (bfp.xOri < 0 && x < 0 && bfp.yOri < 0 && y < 0) {
                return bfp;
            }
        }
        return ObligingnessExtraversion;
    }
}
