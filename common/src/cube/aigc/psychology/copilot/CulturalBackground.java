/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.aigc.psychology.copilot;

import cell.util.Utils;

public enum CulturalBackground {

    EastAsianBackground("东亚背景", "往往强调家庭荣誉、人际关系和谐、孝道、面子文化。来访者的困扰常与“无法满足家庭期待”、“由于独立而产生的内疚感”有关。"),

    WesternBackground("欧美背景", "更强调个人自主、自我实现、边界感。困扰可能更多关于“自我价值缺失”、“无法坚持自我”。"),

    IntergenerationalTrauma("代际创伤", "原生家庭极其压抑，例如强调“孝顺”。"),

    Elitism("精英阶层", "高知或高管，用逻辑防御情感，认为“情绪是软弱的”。面临“跌落焦虑”、过度竞争、空心病（不知道为什么而活）、由于忙碌导致的情感忽视。"),

    WageEarners("工薪阶层", "一般上班族，面临生存焦虑、资源匮乏感、对未来的不安全感、甚至因贫穷产生的羞耻感。"),

    Subcultures("亚文化群体", "面临身份认同压力的少数群体等"),

    Random("随机", "从以上选项中随机选择。")

    ;

    public final String display;

    public final String description;

    CulturalBackground(String display, String description) {
        this.display = display;
        this.description = description;
    }

    public static CulturalBackground random() {
        int index = Utils.randomInt(0, values().length - 2);
        return values()[index];
    }

    public static CulturalBackground parse(String name) {
        for (CulturalBackground cb : CulturalBackground.values()) {
            if (cb.name().equalsIgnoreCase(name) || cb.display.equalsIgnoreCase(name)) {
                return cb;
            }
        }
        return CulturalBackground.Random;
    }
}
