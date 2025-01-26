/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology.algorithm;

import cube.vision.Point;

public enum BigFiveTeamworkStyle {

    /**
     * 竞争。
     */
    Competition("Competition"),

    /**
     * 协作。
     */
    Collaboration("Collaboration"),

    /**
     * 顺应。
     */
    Accommodation("Accommodation"),

    /**
     * 回避。
     */
    Avoidance("Avoidance"),

    /**
     * 折中。
     */
    Compromise("Compromise")

    ;

    public final String name;

    BigFiveTeamworkStyle(String name) {
        this.name = name;
    }

    public static BigFiveTeamworkStyle parse(Point coordinate) {
        if (coordinate.x < -5.0 && coordinate.y > 5.0) {
            return Competition;
        }
        else if (coordinate.x > 5.0 && coordinate.y > 5.0) {
            return Collaboration;
        }
        else if (coordinate.x > 5.0 && coordinate.y < -5.0) {
            return Accommodation;
        }
        else if (coordinate.x < -5.0 && coordinate.y < -5.0) {
            return Avoidance;
        }
        else {
            return Compromise;
        }
    }
}
