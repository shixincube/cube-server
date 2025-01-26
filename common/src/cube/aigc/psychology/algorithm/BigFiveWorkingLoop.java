/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
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
