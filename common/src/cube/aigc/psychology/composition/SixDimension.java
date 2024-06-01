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

public enum SixDimension {

    /**
     * 情绪。
     */
    Emotion("Emotion"),

    /**
     * 认知。
     */
    Cognition("Cognition"),

    /**
     * 行为。
     */
    Behavior("Behavior"),

    /**
     * 人际关系。
     */
    InterpersonalRelationship("InterpersonalRelationship"),

    /**
     * 自我评价。
     */
    SelfAssessment("SelfAssessment"),

    /**
     * 心理健康。
     */
    MentalHealth("MentalHealth"),

    ;

    public final String name;

    SixDimension(String name) {
        this.name = name;
    }

    public static SixDimension parse(String name) {
        for (SixDimension sd : SixDimension.values()) {
            if (sd.name.equalsIgnoreCase(name)) {
                return sd;
            }
        }
        return null;
    }
}
