/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology.composition;

import org.json.JSONObject;

/**
 * 量表因子等级。
 */
public enum ScaleFactorLevel {

    /**
     * 无分级。
     */
    None(0, "无分级"),

    /**
     * 有。
     */
    Slight(1, "有"),

    /**
     * 低。
     */
    Mild(2, "低"),

    /**
     * 中。
     */
    Moderate(3, "中"),

    /**
     * 高。
     */
    Severe(4, "高"),

    ;

    public final int level;

    public final String prefix;

    ScaleFactorLevel(int level, String prefix) {
        this.level = level;
        this.prefix = prefix;
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("level", this.level);
        json.put("prefix", this.prefix);
        return json;
    }
}
