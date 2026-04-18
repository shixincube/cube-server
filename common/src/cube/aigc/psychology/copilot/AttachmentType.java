/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.aigc.psychology.copilot;

import cell.util.Utils;

public enum AttachmentType {

    Secure("安全型", "信任咨询师，能建立联盟。"),

    Anxious("焦虑型", "时刻担心咨询师不喜欢自己，需要大量确认。"),

    Avoidant("回避型", "拒绝亲密，觉得咨询没用，甚至想逃离。"),

    Disorganized("混乱型", "既想靠近又想推开，通常有创伤史。"),

    Random("随机", "从以上选项中随机选择。")

    ;

    public final String display;

    public final String description;

    AttachmentType(String display, String description) {
        this.display = display;
        this.description = description;
    }

    public static AttachmentType random() {
        int index = Utils.randomInt(0, values().length - 2);
        return values()[index];
    }

    public static AttachmentType parse(String name) {
        for (AttachmentType at : AttachmentType.values()) {
            if (at.name().equalsIgnoreCase(name) || at.display.equalsIgnoreCase(name)) {
                return at;
            }
        }
        return AttachmentType.Random;
    }
}
