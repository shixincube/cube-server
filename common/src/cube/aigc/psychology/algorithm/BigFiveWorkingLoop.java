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

import cube.vision.Point;

public enum BigFiveWorkingLoop {

    /**
     * 想法。
     */
    Ideation("Ideation"),

    /**
     * 评估。
     */
    Evaluation("Evaluation"),

    /**
     * 决策。
     */
    Decision("Decision"),

    /**
     * 行动。
     */
    Action("Action"),

    ;

    public final String name;

    BigFiveWorkingLoop(String name) {
        this.name = name;
    }

    /**
     * 通过坐标解析所处的工作环位置。
     *
     * @param coordinate
     * @return
     */
    public static BigFiveWorkingLoop parse(Point coordinate) {
        Point start = new Point(0, 0);
        Point base = new Point(coordinate.x, coordinate.y);
        Point end = null;
        double location = 0;
        if (coordinate.x < 0 && coordinate.y < 0) {
            end = new Point(-10, -10);
            location = Point.pointLocation(start, end, base);
            if (location > 0) {
                return Ideation;
            }
            else {
                return Action;
            }
        }
        else if (coordinate.x > 0 && coordinate.y < 0) {
            end = new Point(10, -10);
            location = Point.pointLocation(start, end, base);
            if (location > 0) {
                return Evaluation;
            }
            else {
                return Ideation;
            }
        }
        else if (coordinate.x > 0 && coordinate.y > 0) {
            end = new Point(10, 10);
            location = Point.pointLocation(start, end, base);
            if (location > 0) {
                return Decision;
            }
            else {
                return Evaluation;
            }
        }
        else {
            end = new Point(-10, 10);
            location = Point.pointLocation(start, end, base);
            if (location > 0) {
                return Action;
            }
            else {
                return Decision;
            }
        }
    }
}
