/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 Shixin Cube Team.
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

package cube.app.server.account;

/**
 * 登录状态码。
 */
public enum StateCode {

    /**
     * 成功。
     */
    Success(0),

    /**
     * 不被允许的行为。
     */
    NotAllowed(1),

    /**
     * 找不到用户。
     */
    NotFindAccount(5),

    /**
     * 无效的令牌。
     */
    InvalidToken(6),

    /**
     * 找不到令牌。
     */
    NotFindToken(7),

    /**
     * 无效账号。
     */
    InvalidAccount(8),

    /**
     * 查询数据错误。
     */
    DataError(9),

    /**
     * 其他状态。
     */
    Other(99);

    /**
     * 编码。
     */
    public final int code;

    StateCode(int code) {
        this.code = code;
    }
}
