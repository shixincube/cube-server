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

package cube.common.state;

/**
 * CV 模块状态码。
 */
public enum CVStateCode {

    /**
     * 成功。
     */
    Ok(0),

    /**
     * 无效参数。
     */
    InvalidParameter(5),

    /**
     * 未找到指定数据。
     */
    NotFound(6),

    /**
     * 正在处理数据。
     */
    Processing(7),

    /**
     * 数据结构错误。
     */
    DataStructureError(8),

    /**
     * 遇到故障。
     */
    Failure(9),

    /**
     * 无效域信息。
     */
    InvalidDomain(11),

    /**
     * 被中断的操作。
     */
    Interrupted(12),

    /**
     * 令牌不一致。
     */
    InconsistentToken(21),

    /**
     * 没有令牌。
     */
    NoToken(22),

    /**
     * 无有效数据。
     */
    NoData(23),

    /**
     * 无效数据。
     */
    InvalidData(24),

    /**
     * 不被接受的非法操作。
     */
    IllegalOperation(25),

    /**
     * 系统忙。
     */
    Busy(26),

    /**
     * 已停止的动作。
     */
    Stopped(27),

    /**
     * 文件处理错误。
     */
    FileError(32),

    /**
     * 终端节点异常。
     */
    EndpointException(33),

    /**
     * 内容长度越界。
     */
    ContentLengthOverflow(41),

    /**
     * 未知的状态。
     */
    Unknown(99)

    ;

    public final int code;

    CVStateCode(int code) {
        this.code = code;
    }

    public static CVStateCode parse(int code) {
        for (CVStateCode sc : CVStateCode.values()) {
            if (sc.code == code) {
                return sc;
            }
        }
        return Unknown;
    }
}