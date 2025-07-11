/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology.composition;

public enum HexagonDimension {

    /**
     * 情绪。
     */
    Mood("Mood", "情绪"),

    /**
     * 认知。
     */
    Cognition("Cognition", "认知"),

    /**
     * 行为。
     */
    Behavior("Behavior", "行为"),

    /**
     * 人际关系敏感。
     */
    InterpersonalRelationship("InterpersonalRelationship", "人际关系敏感"),

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
        for (HexagonDimension hd : HexagonDimension.values()) {
            if (hd.name.equalsIgnoreCase(name) || hd.displayName.equalsIgnoreCase(name)) {
                return hd;
            }
        }
        // 兼容旧版本
        if (name.equalsIgnoreCase("Emotion")) {
            return HexagonDimension.Mood;
        }
        return null;
    }
}
