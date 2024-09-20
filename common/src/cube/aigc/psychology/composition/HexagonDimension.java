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

public enum HexagonDimension {

    /**
     * 情绪。
     */
    Emotion("Emotion", "情绪"),

    /**
     * 认知。
     */
    Cognition("Cognition", "认知"),

    /**
     * 行为。
     */
    Behavior("Behavior", "行为"),

    /**
     * 人际关系。
     */
    InterpersonalRelationship("InterpersonalRelationship", "人际关系"),

    /**
     * 自我评价。
     */
    SelfAssessment("SelfAssessment", "自我评价"),

    /**
     * 心理健康。
     */
    MentalHealth("MentalHealth", "心理健康"),

    ;

    public final String name;

    public final String displayName;

    HexagonDimension(String name, String displayName) {
        this.name = name;
        this.displayName = displayName;
    }

    public static HexagonDimension parse(String name) {
        for (HexagonDimension sd : HexagonDimension.values()) {
            if (sd.name.equalsIgnoreCase(name)) {
                return sd;
            }
        }
        return null;
    }
}
