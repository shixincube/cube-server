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

import org.json.JSONObject;

import java.util.*;

/**
 * 评价指标。
 */
public enum Indicator {

    /**
     * 精神病性。
     */
    Psychosis("精神病性", "Psychosis", 10),

    /**
     * 外倾。
     */
    Extroversion("外倾", "Extroversion", 78),

    /**
     * 内倾。
     */
    Introversion("内倾", "Introversion", 77),

    /**
     * 乐观。
     */
    Optimism("乐观", "Optimism", 81),

    /**
     * 悲观。
     */
    Pessimism("悲观", "Pessimism", 82),

    /**
     * 自恋。
     */
    Narcissism("自恋", "Narcissism", 49),

    /**
     * 自信。
     */
    Confidence("自信", "Confidence", 64),

    /**
     * 自尊。
     */
    SelfEsteem("自尊", "SelfEsteem", 35),

    /**
     * 社会适应性。
     */
    SocialAdaptability("社会适应性", "SocialAdaptability", 51),

    /**
     * 独立。
     */
    Independence("独立", "Independence", 61),

    /**
     * 理想主义。
     */
    Idealism("理想主义", "Idealism", 12),

    /**
     * 现实主义。
     */
    Realism("现实主义", "Realism", 11),

    /**
     * 情绪。
     */
    Emotion("情绪", "Emotion", 63),

    /**
     * 自我意识。
     */
    SelfConsciousness("自我意识", "SelfConsciousness", 19),

    /**
     * 思考。
     */
    Thought("思考", "Thought", 71),

    /**
     * 安全感。
     */
    SenseOfSecurity("安全感", "SenseOfSecurity", 81),

    /**
     * 强迫。
     */
    Obsession("强迫", "Obsession", 90),

    /**
     * 压抑。
     */
    Constrain("压抑", "Constrain", 62),

    /**
     * 自我控制。
     */
    SelfControl("自我控制", "SelfControl", 29),

    /**
     * 焦虑。
     */
    Anxiety("焦虑", "Anxiety", 91),

    /**
     * 抑郁。
     */
    Depression("抑郁", "Depression", 92),

    /**
     * 单纯。
     */
    Simple("单纯", "Simple", 32),

    /**
     * 温顺。
     */
    Meekness("温顺", "Meekness", 31),

    /**
     * 敌对。
     */
    Hostile("敌对", "Hostile", 53),

    /**
     * 攻击性。
     */
    Attacking("攻击性", "Attacking", 40),

    /**
     * 家庭关系。
     */
    Family("家庭关系", "Family", 32),

    /**
     * 人际关系。
     */
    InterpersonalRelation("人际关系", "InterpersonalRelation", 33),

    /**
     * 外在评价。
     */
    EvaluationFromOutside("外在评价", "EvaluationFromOutside", 41),

    /**
     * 偏执。
     */
    Paranoid("偏执", "Paranoid", 54),

    /**
     * 成就动机。
     */
    AchievementMotivation("成就动机", "AchievementMotivation", 55),

    /**
     * 压力。
     */
    Stress("压力", "Stress", 74),

    /**
     * 创造力。
     */
    Creativity("创造力", "Creativity", 88),

    /**
     * 冲动。
     */
    Impulsion("冲动", "Impulsion", 61),

    /**
     * 奋斗。
     */
    Struggle("奋斗", "Struggle", 50),

    /**
     * 道德感。
     */
    MoralSense("道德感", "MoralSense", 31),

    /**
     * 向往自由。
     */
    DesireForFreedom("向往自由", "DesireForFreedom", 30),

    /**
     * 未知。（服务无法进行处理）
     */
    Unknown("未知", "Unknown", 0)

    ;

    public final String name;

    public final String code;

    public final int priority;

    Indicator(String name, String code, int priority) {
        this.name = name;
        this.code = code;
        this.priority = priority;
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("name", this.name);
        json.put("code", this.code);
        return json;
    }

    public static Indicator parse(String valueOrName) {
        for (Indicator indicator : Indicator.values()) {
            if (indicator.name.equals(valueOrName) || indicator.code.equalsIgnoreCase(valueOrName)) {
                return indicator;
            }
        }

        return Unknown;
    }

    public static List<Indicator> sortByPriority() {
        List<Indicator> result = new ArrayList<>();
        result.addAll(Arrays.asList(values()));
        result.remove(Unknown); // 删除 Unknown
        Collections.sort(result, new Comparator<Indicator>() {
            @Override
            public int compare(Indicator i1, Indicator i2) {
                return i2.priority - i1.priority;
            }
        });
        return result;
    }
}
