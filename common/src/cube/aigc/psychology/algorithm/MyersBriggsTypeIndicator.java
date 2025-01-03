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

public enum MyersBriggsTypeIndicator {

    /**
     * 内倾。
     */
    Introversion("内倾", "I"),

    /**
     * 外倾。
     */
    Extraversion("外倾", "E"),

    /**
     * 感觉。
     */
    Sensing("感觉", "S"),

    /**
     * 直觉。
     */
    Intuition("直觉", "N"),

    /**
     * 情感。
     */
    Feeling("情感", "F"),

    /**
     * 思考。
     */
    Thinking("思考", "T"),

    /**
     * 判断。
     */
    Judging("判断", "J"),

    /**
     * 知觉。
     */
    Perceiving("知觉", "P")

    ;

    public final String displayName;

    public final String code;

    MyersBriggsTypeIndicator(String displayName, String code) {
        this.displayName = displayName;
        this.code = code;
    }

    public static MyersBriggsTypeIndicator parse(String nameOrCode) {
        for (MyersBriggsTypeIndicator indicator : MyersBriggsTypeIndicator.values()) {
            if (indicator.displayName.equals(nameOrCode) || indicator.code.equalsIgnoreCase(nameOrCode)) {
                return indicator;
            }
        }

        return null;
    }
}
