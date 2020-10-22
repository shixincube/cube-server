/**
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Shixin Cube Team.
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
 * 消息状态。
 */
public enum MessageState {

    /**
     * 未发送状态。
     */
    Unsent(1),

    /**
     * 正在发送状态。
     */
    Sending(1 << 1),

    /**
     * 已发送状态。
     */
    Sent(1 << 2),

    /**
     * 已被阅读状态。
     */
    Read(1 << 3),

    /**
     * 已召回。
     */
    Recalled(1 << 4),

    /**
     * 消息处理失败。
     */
    Fault(1 << 5),

    /**
     * 未知状态。
     */
    Unknown(0);

    private int code;

    MessageState(int code) {
        this.code = code;
    }

    public int getCode() {
        return this.code;
    }

    public static MessageState parse(int code) {
        switch (code) {
            case 1:
                return Unsent;
            case 1 << 1:
                return Sent;
            case 1 << 2:
                return Read;
            case 1 << 3:
                return Fault;
            default:
                return Unknown;
        }
    }
}
