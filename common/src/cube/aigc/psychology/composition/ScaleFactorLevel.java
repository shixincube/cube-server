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
     * 没有。
     */
    None(0, "没有"),

    /**
     * 略有。
     */
    Slight(1, "略有"),

    /**
     * 轻度。
     */
    Mild(2, "轻度"),

    /**
     * 中度。
     */
    Moderate(3, "中度"),

    /**
     * 重度。
     */
    Severe(4, "重度"),

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
