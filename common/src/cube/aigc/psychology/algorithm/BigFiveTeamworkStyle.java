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
