/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology;

/**
 * 测试主题。
 */
public enum Theme {

    Generic("通用", "Generic"),

    HouseTreePerson("房树人绘画", "HTP"),

    PersonInTheRain("雨中人绘画", "PIR"),

    TreeTest("树木绘画", "TT"),

    SelfPortrait("自画像", "SP"),

    AttachmentStyle("依恋类型", "AS"),

    ;

    public final String name;

    public final String code;

    Theme(String name, String code) {
        this.name = name;
        this.code = code;
    }

    public static Theme parse(String nameOrCode) {
        for (Theme th : Theme.values()) {
            if (th.name.equals(nameOrCode) || th.code.equalsIgnoreCase(nameOrCode)) {
                return th;
            }
        }

        return null;
    }
}
