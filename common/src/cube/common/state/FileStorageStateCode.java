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
 * 文件存储模块状态码。
 */
public enum FileStorageStateCode {

    /**
     * 成功。
     */
    Ok(0),

    /**
     * 无效参数。
     */
    InvalidParameter(5),

    /**
     * 系统忙。
     */
    Busy(8),

    /**
     * 遇到故障。
     */
    Failure(9),

    /**
     * 无效域信息。
     */
    InvalidDomain(11),

    /**
     * 无效的参数，禁止访问。
     */
    Forbidden(12),

    /**
     * 未找到指定数据。
     */
    NotFound(13),

    /**
     * 未授权访问。
     */
    Unauthorized(14),

    /**
     * 拒绝操作。
     */
    Reject(15),

    /**
     * 文件标签错误。
     */
    FileLabelError(16),

    /**
     * 正在写入文件。
     */
    Writing(17),

    /**
     * 没有目录。
     */
    NoDirectory(18),

    /**
     * 重名。
     */
    DuplicationOfName(20),

    /**
     * 数据过期。
     */
    DataExpired(21),

    /**
     * 存储空间溢出。
     */
    SpaceSizeOverflow(23),

    /**
     * 搜索条件错误。
     */
    SearchConditionError(25),

    /**
     * 未知的状态。
     */
    Unknown(99)

    ;

    public final int code;

    FileStorageStateCode(int code) {
        this.code = code;
    }
}
