/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology.algorithm;

import cube.aigc.psychology.Indicator;
import cube.common.Language;

public enum AttachmentStyle {

    /**
     * 回避型。
     */
    AvoidantAttachment("Avoidant", "回避型依恋", "Avoidant attachment",
            "吵架", "", "亲密举动", "", "自己的脆弱", ""),

    /**
     * 焦虑型。
     */
    AnxiousAttachment("Anxious", "焦虑型依恋", "Anxious attachment",
            "吵架", "", "关系进度", "", "冲突后的和解", ""),

    /**
     * 安全型。
     */
    SecureAttachment("Secure", "安全型依恋", "Secure attachment",
            "吵架", "", "表达需求", "", "处理分歧", ""),

    /**
     * 恐惧型。
     */
    FearfulAttachment("Fearful", "恐惧型依恋", "Fearful attachment",
            "吵架", "", "关系推进", "", "过去的创伤", ""),

    ;

    public final String style;

    public final String nameCN;

    public final String nameEN;

    public final String scene1CN;

    public final String scene1EN;

    public final String scene2CN;

    public final String scene2EN;

    public final String scene3CN;

    public final String scene3EN;

    AttachmentStyle(String style, String nameCN, String nameEN,
                    String scene1CN, String scene1EN,
                    String scene2CN, String scene2EN,
                    String scene3CN, String scene3EN) {
        this.style = style;
        this.nameCN = nameCN;
        this.nameEN = nameEN;
        this.scene1CN = scene1CN;
        this.scene1EN = scene1EN;
        this.scene2CN = scene2CN;
        this.scene2EN = scene2EN;
        this.scene3CN = scene3CN;
        this.scene3EN = scene3EN;
    }

    public String getScene1Title(Language language) {
        return language.isChinese() ? this.nameCN + this.scene1CN + "场景" :
                "The scene of " + this.nameEN + this.scene1EN;
    }

    public String getScene2Title(Language language) {
        return language.isChinese() ? this.nameCN + this.scene2CN + "场景" :
                "The scene of " + this.nameEN + this.scene2EN;
    }

    public String getScene3Title(Language language) {
        return language.isChinese() ? this.nameCN + this.scene3CN + "场景" :
                "The scene of " + this.nameEN + this.scene3EN;
    }

    public static AttachmentStyle parse(Indicator indicator) {
        switch (indicator) {
            case AvoidantAttachment:
                return AttachmentStyle.AvoidantAttachment;
            case AnxiousAttachment:
                return AttachmentStyle.AnxiousAttachment;
            case SecureAttachment:
                return AttachmentStyle.SecureAttachment;
            case FearfulAttachment:
                return AttachmentStyle.FearfulAttachment;
            default:
                return null;
        }
    }
}
