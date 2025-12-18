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
    SpecialAttention(3, "关注4级", "表示受测人心理状态有显著症状，应予以重视。具有出现精神症状的风险，同时自身抵抗力较弱，目前处于低心理健康水平，建议尽快进行调整，可向心理咨询机构、正规医院心理科等专业机构寻求帮助。"),

    /**
     * 重点关注
     */
    FocusedAttention(2, "关注3级", "表示受测人心理状态有较明显症状表现，应当予以进一步处置。具有出现精神症状的风险，但由于自身抵抗力较高或获得了充分的社会支持，目前处于中等心理健康水平，建议关注得分较高的消极心理健康相关侧面，适当进行调整。"),

    /**
     * 一般关注
     */
    GeneralAttention(1, "关注2级", "表示受测人心理状态有轻微症状表现，不影响正常生活和学习。受测人目前暂无患精神疾病的风险，但自身抵抗力较弱，一旦遇到现实困扰有可能引发心理问题，目前处于中等心理健康水平，建议关注积极心理健康相关侧面，适当进行改善。"),

    /**
     * 无需关注
     */
    NoAttention(0, "关注1级", "表示受测人心理状态无明显异常，心理状态良好。心理健康，保持现状。"),

    /**
     * 审慎关注
     */
    PrudentAttention(5, "审慎关注", ""),

    ;

    public final int level;

    public final String name;

    public final String description;

    Attention(int level, String name, String description) {
        this.level = level;
        this.name = name;
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
