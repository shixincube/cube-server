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

/**
 * 空间示意。
 */
public enum SpatialRepresentation {

    /**
     * 精神。
     */
    Spirit("Spirit"),

    /**
     * 物质。
     */
    Matter("Matter"),

    /**
     * 过去。
     */
    Past("Past"),

    /**
     * 未来。
     */
    Future("Future"),

    /**
     * 内向者。
     */
    Introvert("Introvert"),

    /**
     * 外向者。
     */
    Extravert("Extravert"),

    /**
     * 被动性。
     */
    Passivity("Passivity"),

    /**
     * 主动性。
     */
    Proactivity("Proactivity"),

    /**
     * 退行。
     */
    Regression("Regression"),

    /**
     * 冲动。
     */
    Impulse("Impulse"),


    Unknown("Unknown")
    ;

    public final String name;

    SpatialRepresentation(String name) {
        this.name = name;
    }
}