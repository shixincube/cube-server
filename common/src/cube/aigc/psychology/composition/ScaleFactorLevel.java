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
