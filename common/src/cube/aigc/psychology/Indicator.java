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
    Depression("抑郁倾向", "Depression", 96),

    /**
     * 焦虑。
     */
    Anxiety("焦虑情绪", "Anxiety", 95),

    /**
     * 强迫。
     */
    Obsession("强迫", "Obsession", 94),

    /**
     * 创造力。
     */
    Creativity("创造力", "Creativity", 93),

    /**
     * 悲观。
     */
    Pessimism("悲观者", "Pessimism", 92),

    /**
     * 乐观。
     */
    Optimism("乐观者", "Optimism", 91),

    /**
     * 安全感。
     */
    SenseOfSecurity("安全感", "SenseOfSecurity", 90),

    /**
     * 外倾。
     */
    Extroversion("外倾", "Extroversion", 89),

    /**
     * 内倾。
     */
    Introversion("内倾", "Introversion", 88),

    /**
     * 压力。
     */
    Stress("压力", "Stress", 87),

    /**
     * 思考。
     */
    Thought("思考", "Thought", 86),

    /**
     * 冲动。
     */
    Impulsion("冲动性", "Impulsion", 84),

    /**
     * 自信。
     */
    Confidence("自信心", "Confidence", 83),

    /**
     * 情绪。
     */
    Mood("情绪", "Mood", 82),

    /**
     * 压抑。
     */
    Repression("压抑", "Repression", 81),

    /**
     * 独立。
     */
    Independence("独立", "Independence", 80),

    /**
     * 成就动机。
     */
    AchievementMotivation("成就动机", "AchievementMotivation", 79),

    /**
     * 偏执。
     */
    Paranoid("偏执", "Paranoid", 78),

    /**
     * 敌对。
     */
    Hostile("敌对", "Hostile", 77),

    /**
     * 社会适应性。
     */
    SocialAdaptability("社会适应性", "SocialAdaptability", 76),

    /**
     * 进取性。
     */
    Struggle("进取性", "Struggle", 75),

    /**
     * 自恋。
     */
    Narcissism("自恋", "Narcissism", 74),

    /**
     * 外在评价。
     */
    EvaluationFromOutside("外在评价", "EvaluationFromOutside", 73),

    /**
     * 攻击性。
     */
    Aggression("攻击性", "Aggression", 72),

    /**
     * 自尊。
     */
    SelfEsteem("自尊", "SelfEsteem", 69),

    /**
     * 家庭关系。
     */
    Family("重视家庭关系", "Family", 68),

    /**
     * 人际关系。
     */
    InterpersonalRelation("人际关系敏感", "InterpersonalRelation", 67),

    /**
     * 单纯。
     */
    Simple("单纯", "Simple", 66),

    /**
     * 温顺。
     */
    Meekness("温顺", "Meekness", 65),

    /**
     * 道德感。
     */
    MoralSense("道德感", "MoralSense", 64),

    /**
     * 自我控制。
     */
    SelfControl("自我控制", "SelfControl", 63),

    /**
     * 向往自由。
     */
    DesireForFreedom("向往自由", "DesireForFreedom", 62),

    /**
     * 自我意识。
     */
    SelfConsciousness("自我意识", "SelfConsciousness", 61),

    /**
     * 理想主义。
     */
    Idealism("理想主义", "Idealism", 59),

    /**
     * 现实主义。
     */
    Realism("现实主义", "Realism", 58),

    /**
     * 精神病性。
     */
    Psychosis("精神病性", "Psychosis", 56),

    /**
     * 逻辑思维。
     */
    LogicalThinking("逻辑思维", "LogicalThinking", 15),



    /**
     * 安全型依恋。
     */
    SecureAttachment("安全型依恋", "SecureAttachment", 54),

    /**
     * 焦虑型依恋。
     */
    AnxiousPreoccupiedAttachment("焦虑型依恋", "AnxiousPreoccupiedAttachment", 53),

    /**
     * 回避型依恋。
     */
    DismissiveAvoidantAttachment("回避型依恋", "DismissiveAvoidantAttachment", 52),

    /**
     * 混乱型依恋。
     */
    DisorganizedAttachment("混乱型依恋", "DisorganizedAttachment", 51),

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
            if (indicator.name.equalsIgnoreCase(valueOrName) || indicator.code.equalsIgnoreCase(valueOrName)) {
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
