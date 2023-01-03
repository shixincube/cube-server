/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 Cube Team.
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
 * 会话类型。
 */
public enum ConversationType {

    /**
     * 与联系人的会话。
     */
    Contact(1),

    /**
     * 与群组的会话。
     */
    Group(2),

    /**
     * 与组织的会话。
     */
    Organization(3),

    /**
     * 系统类型会话。
     */
    System(4),

    /**
     * 通知类型会话。
     */
    Notifier(5),

    /**
     * 助手类型会话。
     */
    Assistant(6),

    /**
     * 其他会话类型。
     */
    Other(9);

    public final int code;

    ConversationType(int code) {
        this.code = code;
    }

    public static ConversationType parse(int code) {
        for (ConversationType type : ConversationType.values()) {
            if (type.code == code) {
                return type;
            }
        }

        return Other;
    }
}
