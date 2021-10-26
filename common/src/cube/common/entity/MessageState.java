/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 Cube Team.
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
     * 消息处理失败。
     */
    Fault(1),

    /**
     * 未发送状态。
     */
    Unsent(5),

    /**
     * 正在发送状态。
     */
    Sending(9),

    /**
     * 已发送状态。
     */
    Sent(10),

    /**
     * 已被阅读状态。
     */
    Read(20),

    /**
     * 已召回。
     */
    Recalled(30),

    /**
     * 已删除。
     */
    Deleted(40),

    /**
     * 被阻止发送。
     */
    SendBlocked(51),

    /**
     * 被阻止接收。
     */
    ReceiveBlocked(52),

    /**
     * 未知状态。
     */
    Unknown(0);

    public final int code;

    MessageState(int code) {
        this.code = code;
    }

    public int getCode() {
        return this.code;
    }

    public static MessageState parse(int code) {
        switch (code) {
            case 5:
                return Unsent;
            case 9:
                return Sending;
            case 10:
                return Sent;
            case 20:
                return Read;
            case 30:
                return Recalled;
            case 40:
                return Deleted;
            case 51:
                return SendBlocked;
            case 52:
                return ReceiveBlocked;
            case 1:
                return Fault;
            default:
                return Unknown;
        }
    }
}
