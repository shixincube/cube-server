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

    Generic("通用", "Generic", "请你在纸上至少画出“房、树、人”三个元素（其他元素任意选择），共同构成一副有意义的画面。绘画时间5到10分钟。"),

    HouseTreePerson("房树人绘画", "HTP", "请你在纸上至少画出“房、树、人”三个元素（其他元素任意选择），共同构成一副有意义的画面。绘画时间5到10分钟。"),

    PersonInRain("雨中人绘画", "PIR", "请你闭上眼睛，想象你现在正处于雨中……然后请睁开眼睛，用笔画一张雨中人，不画漫画人和火柴人。"),

    TreeTest("树木绘画", "TT", ""),

    SelfPortrait("自画像", "SP", ""),

    AttachmentStyle("依恋类型", "AS", "请你在纸上画出你和你当下认为最重要的人（或你认为最亲近的人）正在一起做某件事的场景。不限人数，可以是现实中发生过的，也可以是你希望发生的，或者你想象中的样子。"),

    

    ;

    public final String name;

    public final String code;

    public final String instruction;

    Theme(String name, String code, String instruction) {
        this.name = name;
        this.code = code;
        this.instruction = instruction;
    }

    public static Theme parse(String nameOrCode) {
        for (Theme th : Theme.values()) {
            if (th.name.equalsIgnoreCase(nameOrCode) || th.code.equalsIgnoreCase(nameOrCode)) {
                return th;
            }
        }

        return null;
    }
}
