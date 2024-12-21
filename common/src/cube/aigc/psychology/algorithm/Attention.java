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

/**
 * 关注建议。
 */
public enum Attention {

    /**
     * 特殊关注
     */
    SpecialAttention(3, "特殊关注"),

    /**
     * 重点关注
     */
    FocusedAttention(2, "重点关注"),

    /**
     * 一般关注
     */
    GeneralAttention(1, "一般关注"),

    /**
     * 无需关注
     */
    NoAttention(0, "无需关注"),

    /**
     * 审慎关注
     */
    PrudentAttention(5, "审慎关注"),

    ;

    public final int level;

    public final String description;

    Attention(int level, String description) {
        this.level = level;
        this.description = description;
    }

    public static Attention parse(int level) {
        for (Attention as : Attention.values()) {
            if (as.level == level) {
                return as;
            }
        }
        return PrudentAttention;
    }
}
