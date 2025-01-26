/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology.algorithm;

/**
 * 关注建议。
 */
public enum Attention {

    /**
     * 特殊关注
     */
    SpecialAttention(3, "特殊关注"),

    /**
     * 重点关注
     */
    FocusedAttention(2, "重点关注"),

    /**
     * 一般关注
     */
    GeneralAttention(1, "一般关注"),

    /**
     * 无需关注
     */
    NoAttention(0, "无需关注"),

    /**
     * 审慎关注
     */
    PrudentAttention(5, "审慎关注"),

    ;

    public final int level;

    public final String description;

    Attention(int level, String description) {
        this.level = level;
        this.description = description;
    }

    public static Attention parse(int level) {
        for (Attention as : Attention.values()) {
            if (as.level == level) {
                return as;
            }
        }
        return PrudentAttention;
    }
}
