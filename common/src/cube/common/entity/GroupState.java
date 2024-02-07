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

package cube.common.entity;

/**
 * 群状态。
 */
public enum GroupState {

    /**
     * 正常状态。
     */
    Normal(0),

    /**
     * 解散状态。
     */
    Dismissed(1),

    /**
     * 禁用状态。
     */
    Forbidden(2),

    /**
     * 高风险状态。
     */
    HighRisk(3),

    /**
     * 失效状态。
     */
    Disabled(9),

    /**
     * 未知状态。
     */
    Unknown(-1);

    public final int code;

    GroupState(int code) {
        this.code = code;
    }

    public static GroupState parse(int code) {
        switch (code) {
            case 0:
                return Normal;
            case 1:
                return Dismissed;
            case 2:
                return Forbidden;
            case 3:
                return HighRisk;
            case 9:
                return Disabled;
            default:
                return Unknown;
        }
    }
}
