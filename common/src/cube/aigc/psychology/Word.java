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

package cube.aigc.psychology;

/**
 * 专业描述词。
 */
public enum Word {

    /**
     * 自我存在。
     */
    SelfExistence("自我存在"),

    /**
     * 自尊。
     */
    SelfEsteem("自尊"),

    /**
     * 自信心。
     */
    SelfConfidence("自信心"),

    /**
     * 适应能力。
     */
    Adaptability("适应能力"),

    /**
     * 对环境疏离感。
     */
    EnvironmentalAlienation("环境疏离感"),

    /**
     * 对环境依赖。
     */
    EnvironmentalDependence("环境依赖"),

    /**
     * 对环境亲切感。
     */
    EnvironmentalFriendliness("环境亲切感"),

    /**
     * 内向。
     */
    Introversion("内向"),

    /**
     * 朴素。
     */
    Simple("朴素"),

    

    /**
     * 未知。
     */
    Unknown("未知");

    public final String word;

    Word(String word) {
        this.word = word;
    }
}