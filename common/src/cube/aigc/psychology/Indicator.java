/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology;

import org.json.JSONObject;

import java.util.*;

/**
 * 评价指标。
 */
public enum Indicator {

    /**
     * 抑郁。
     */
    Depression("抑郁", "Depression", 92),

    /**
     * 焦虑。
     */
    Anxiety("焦虑", "Anxiety", 91),

    /**
     * 强迫。
     */
    Obsession("强迫", "Obsession", 90),

    /**
     * 创造力。
     */
    Creativity("创造力", "Creativity", 88),

    /**
     * 悲观。
     */
    Pessimism("悲观", "Pessimism", 82),

    /**
     * 乐观。
     */
    Optimism("乐观", "Optimism", 81),

    /**
     * 安全感。
     */
    SenseOfSecurity("安全感", "SenseOfSecurity", 80),

    /**
     * 外倾。
     */
    Extroversion("外倾", "Extroversion", 78),

    /**
     * 内倾。
     */
    Introversion("内倾", "Introversion", 77),

    /**
     * 压力。
     */
    Stress("压力", "Stress", 74),

    /**
     * 思考。
     */
    Thought("思考", "Thought", 71),

    /**
     * 冲动。
     */
    Impulsion("冲动", "Impulsion", 66),

    /**
     * 自信。
     */
    Confidence("自信", "Confidence", 64),

    /**
     * 情绪。
     */
    Mood("情绪", "Mood", 63),

    /**
     * 压抑。
     */
    Repression("压抑", "Repression", 62),

    /**
     * 独立。
     */
    Independence("独立", "Independence", 61),

    /**
     * 成就动机。
     */
    AchievementMotivation("成就动机", "AchievementMotivation", 55),

    /**
     * 偏执。
     */
    Paranoid("偏执", "Paranoid", 54),

    /**
     * 敌对。
     */
    Hostile("敌对", "Hostile", 53),

    /**
     * 社会适应性。
     */
    SocialAdaptability("社会适应性", "SocialAdaptability", 51),

    /**
     * 进取性。
     */
    Struggle("进取性", "Struggle", 50),

    /**
     * 自恋。
     */
    Narcissism("自恋", "Narcissism", 49),

    /**
     * 外在评价。
     */
    EvaluationFromOutside("外在评价", "EvaluationFromOutside", 41),

    /**
     * 攻击性。
     */
    Attacking("攻击性", "Attacking", 40),

    /**
     * 自尊。
     */
    SelfEsteem("自尊", "SelfEsteem", 35),

    /**
     * 家庭关系。
     */
    Family("家庭关系", "Family", 34),

    /**
     * 人际关系。
     */
    InterpersonalRelation("人际关系", "InterpersonalRelation", 33),

    /**
     * 单纯。
     */
    Simple("单纯", "Simple", 32),

    /**
     * 温顺。
     */
    Meekness("温顺", "Meekness", 31),

    /**
     * 道德感。
     */
    MoralSense("道德感", "MoralSense", 30),

    /**
     * 自我控制。
     */
    SelfControl("自我控制", "SelfControl", 29),

    /**
     * 向往自由。
     */
    DesireForFreedom("向往自由", "DesireForFreedom", 28),

    /**
     * 自我意识。
     */
    SelfConsciousness("自我意识", "SelfConsciousness", 19),

    /**
     * 理想主义。
     */
    Idealism("理想主义", "Idealism", 12),

    /**
     * 现实主义。
     */
    Realism("现实主义", "Realism", 11),

    /**
     * 精神病性。
     */
    Psychosis("精神病性", "Psychosis", 10),

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

        if (valueOrName.equalsIgnoreCase("Emotion")) {
            return Mood;
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
