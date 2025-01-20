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
 * 文件处理状态码。
 */
public enum FileProcessorStateCode {

    /**
     * 成功。
     */
    Ok(0),

    /**
     * 未授权的请求。
     */
    Unauthorized(4),

    /**
     * 无效参数。
     */
    InvalidParameter(5),

    /**
     * 遇到故障。
     */
    Failure(9),

    /**
     * 无效域信息。
     */
    InvalidDomain(11),

    /**
     * 操作超时。
     */
    OptTimeout(12),

    /**
     * 无文件。
     */
    NoFile(14),

    /**
     * 文件数据异常。
     */
    FileDataException(15),

    /**
     * 与 CV 服务器无连接。
     */
    NoCVConnection(21),

    /**
     * 未知的状态。
     */
    Unknown(99)

    ;

    public final int code;

    FileProcessorStateCode(int code) {
        this.code = code;
    }

    public static FileProcessorStateCode parse(int code) {
        for (FileProcessorStateCode sc : FileProcessorStateCode.values()) {
            if (sc.code == code) {
                return sc;
            }
        }
        return FileProcessorStateCode.Unknown;
    }
}
