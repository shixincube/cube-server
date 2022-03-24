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

package cube.hub;

/**
 * Hub 模块状态码。
 */
public enum HubStateCode {

    /**
     * 成功。
     */
    Ok(0),

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
     * 未授权。
     */
    Unauthorized(12),

    /**
     * 不支持的信令。
     */
    UnsupportedSignal(13),

    /**
     * 不支持的事件。
     */
    UnsupportedEvent(14),

    /**
     * 数据过期。
     */
    Expired(15),

    /**
     * 控制器错误。
     */
    ControllerError(17),

    /**
     * 未知的状态。
     */
    Unknown(99)

    ;

    public final int code;

    HubStateCode(int code) {
        this.code = code;
    }

    public static HubStateCode parse(int code) {
        for (HubStateCode stateCode : HubStateCode.values()) {
            if (stateCode.code == code) {
                return stateCode;
            }
        }

        return HubStateCode.Unknown;
    }
}
