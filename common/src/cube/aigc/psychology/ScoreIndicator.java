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

package cube.aigc.psychology;

public enum ScoreIndicator {

    Psychosis("精神病性", "Psychosis"),

    Optimism("乐观", "Optimism"),

    Pessimism("悲观", "Pessimism"),

    Narcissism("自恋", "Narcissism"),

    Confidence("自信", "Confidence"),

    SelfEsteem("自尊", "SelfEsteem"),

    SocialAdaptability("社会适应性", "SocialAdaptability"),

    Independence("独立", "Independence"),

    Idealism("理想主义", "Idealism"),

    Emotion("情感", "Emotion"),

    SelfConsciousness("自我意识", "SelfConsciousness"),

    Realism("现实主义", "Realism"),

    Thought("思考", "Thought"),

    SenseOfSecurity("安全感", "SenseOfSecurity"),

    Obsession("强迫", "Obsession"),

    Constrain("压抑", "Constrain"),

    SelfControl("自我控制", "SelfControl"),

    Anxiety("焦虑", "Anxiety"),

    Depression("抑郁", "Depression"),

    ;

    public final String name;

    public final String code;

    ScoreIndicator(String name, String code) {
        this.name = name;
        this.code = code;
    }
}
