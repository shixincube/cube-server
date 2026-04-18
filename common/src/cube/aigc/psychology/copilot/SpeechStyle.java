/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.aigc.psychology.copilot;

import cell.util.Utils;

public enum SpeechStyle {

    Silent("沉默型", "问十句答一句，或者长时间沉默。"),

    Rambling("滔滔不绝型", "抢占所有时间，不让你插嘴。"),

    Metaphorical("隐喻型", "说话像写诗或谜语，从不直说。"),

    Vulgar("粗俗", "使用脏话，言语刻薄。"),

    Random("随机", "从以上选项中随机选择。")

    ;

    public final String display;

    public final String description;

    SpeechStyle(String display, String description) {
        this.display = display;
        this.description = description;
    }

    public static SpeechStyle random() {
        int index = Utils.randomInt(0, values().length - 2);
        return values()[index];
    }

    public static SpeechStyle parse(String name) {
        for (SpeechStyle ss : SpeechStyle.values()) {
            if (ss.name().equalsIgnoreCase(name) || ss.display.equalsIgnoreCase(name)) {
                return ss;
            }
        }
        return SpeechStyle.Random;
    }
}
