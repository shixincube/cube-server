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

/**
 * 大五人格。
 */
public enum TheBigFive {

    /**
     * 宜人性。
     */
    Obligingness("Obligingness", "宜人性"),

    /**
     * 尽责性。
     */
    Conscientiousness("Conscientiousness", "尽责性"),

    /**
     * 外向性。
     */
    Extraversion("Extraversion", "外向性"),

    /**
     * 进取性。
     */
    Achievement("Achievement", "进取性"),

    /**
     * 情绪性。
     */
    Neuroticism("Neuroticism", "情绪性"),

    ;

    public final String code;

    public final String name;

    TheBigFive(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static TheBigFive parse(String codeOrName) {
        for (TheBigFive tbf : TheBigFive.values()) {
            if (tbf.code.equalsIgnoreCase(codeOrName) || tbf.name.equals(codeOrName)) {
                return tbf;
            }
        }
        return Neuroticism;
    }
}
