/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.aigc.psychology.copilot;

import cell.util.Utils;

public enum Empathy {

    Idealization("理想化", "来访者：“你是我的救世主，只有你能救我。”"),

    Belittling("贬低", "来访者：“你也就是个混饭吃的，你根本不懂我。”"),

    Seduction("诱惑", "试图打破专业边界（测试咨询师的伦理）。"),

    Cooperation("合作", "理性地配合，但内心封闭。"),

    Random("随机", "从以上选项中随机选择。")

    ;

    public final String display;

    public final String description;

    Empathy(String display, String description) {
        this.display = display;
        this.description = description;
    }

    public static Empathy random() {
        int index = Utils.randomInt(0, values().length - 2);
        return values()[index];
    }

    public static Empathy parse(String name) {
        for (Empathy empathy : Empathy.values()) {
            if (empathy.name().equalsIgnoreCase(name) || empathy.display.equalsIgnoreCase(name)) {
                return empathy;
            }
        }
        return Empathy.Random;
    }
}
