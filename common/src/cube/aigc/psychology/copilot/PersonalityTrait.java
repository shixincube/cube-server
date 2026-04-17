/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.aigc.psychology.copilot;

public enum PersonalityTrait {

    BPD("边缘型", "情绪极度不稳定，害怕被抛弃，黑白分明的思维（“你要么全好，要么全坏”），有自伤史。"),

    NPD("自恋型", "夸大自我重要性，需要过度赞美，缺乏共情，对批评极度敏感（即使是隐性的）。"),

    AvPD("回避型", "极度社交抑制，感觉自己不如别人，对负面评价极其恐惧。"),

    OCPD("强迫型", "追求完美、控制欲强、刻板、过分投入工作，情感表达僵硬。"),

    DPD("依赖型", "难以独立做决定，需要他人过度照顾，害怕分离。"),

    APD("反社会型", "表面顺从实则抵抗，或者缺乏悔意。"),

    HFOP("“高功能”普通人", "仅仅是遇到了具体的生活危机（失恋、丧亲、职场倦怠），功能尚可，只是痛苦。"),

    Random("随机", "从以上选项中随机选择。")

    ;

    public final String name;

    public final String description;

    PersonalityTrait(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public static PersonalityTrait parse(String name) {
        for (PersonalityTrait pt : PersonalityTrait.values()) {
            if (pt.name().equalsIgnoreCase(name) || pt.name.equalsIgnoreCase(name)) {
                return pt;
            }
        }
        return PersonalityTrait.Random;
    }
}
