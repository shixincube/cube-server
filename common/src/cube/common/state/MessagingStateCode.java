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

package cube.common.state;

/**
 * 消息模块状态码。
 */
public enum MessagingStateCode {

    /**
     * 成功。
     */
    Ok(0),

    /**
     * 遇到故障。
     */
    Failure(9),

    /**
     * 无效域信息。
     */
    InvalidDomain(11),

    /**
     * 没有域信息。
     */
    NoDomain(12),

    /**
     * 没有设备信息。
     */
    NoDevice(15),

    /**
     * 没有找到联系人。
     */
    NoContact(16),

    /**
     * 未知的状态。
     */
    Unknown(99)

    ;

    public final int code;

    MessagingStateCode(int code) {
        this.code = code;
    }
}
