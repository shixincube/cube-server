/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 Ambrose Xu.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
