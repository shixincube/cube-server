/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology.algorithm;

import org.json.JSONObject;

/**
 * 指标评级。
 */
public enum IndicatorRate {

    /**
     * 无。
     */
    None(0, "略"),

    /**
     * 很低。
     */
    Lowest(1, "很低"),

    /**
     * 低。
     */
    Low(2, "低"),

    /**
     * 中等。
     */
    Medium(3, "中等"),

    /**
     * 高。
     */
    High(4, "高"),

    /**
     * 很高。
     */
    Highest(5, "很高")

    ;

    public final int value;

    public final String displayName;

    IndicatorRate(int value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("value", this.value);
        json.put("displayName", this.displayName);
        return json;
    }

    public static IndicatorRate parse(JSONObject json) {
        int value = json.getInt("value");
        return parse(value);
    }

    public static IndicatorRate parse(int value) {
        for (IndicatorRate ir : IndicatorRate.values()) {
            if (ir.value == value) {
                return ir;
            }
        }
        return null;
    }
}
