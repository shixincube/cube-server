/**
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

package cube.common.state;

/**
 * 多方通讯状态码。
 */
public enum MultipointCommStateCode {

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
     * 没有域信息。
     */
    NoDomain(12),

    /**
     * 没有设备信息。
     */
    NoDevice(13),

    /**
     * 没有找到联系人。
     */
    NoContact(14),

    /**
     * 没有找到通讯场域。
     */
    NoCommField(15),

    /**
     * 没有找到媒体单元。
     */
    NoMediaUnit(16),

    /**
     * 没有找到与媒体单元的数据通道。
     */
    NoPipeline(17),

    /**
     * 没有找到 Endpoint 。
     */
    NoCommFieldEndpoint(18),

    /**
     * 没有找到对端。
     */
    NoPeerEndpoint(19),

    /**
     * 数据结构错误。
     */
    DataStructureError(20),

    /**
     * 场域状态错误。
     */
    CommFieldStateError(21),

    /**
     * 媒体单元故障。
     */
    MediaUnitField(23),

    /**
     * 不被支持的信令。
     */
    UnsupportedSignaling(24),

    /**
     * 正在建立通话。
     */
    Calling(30),

    /**
     * 当前线路忙。
     */
    Busy(31),

    /**
     * 通话已接通。
     */
    CallConnected(33),

    /**
     * 通话结束。
     */
    CallBye(35),

    /**
     * 主叫忙。
     */
    CallerBusy(41),

    /**
     * 被叫忙。
     */
    CalleeBusy(42),

    /**
     * 被主叫阻止。
     */
    BeCallerBlocked(45),

    /**
     * 被被叫阻止。
     */
    BeCalleeBlocked(46),

    /**
     * 未知的状态。
     */
    Unknown(99)

    ;

    public final int code;

    MultipointCommStateCode(int code) {
        this.code = code;
    }

    public static MultipointCommStateCode match(int code) {
        for (MultipointCommStateCode state : MultipointCommStateCode.values()) {
            if (state.code == code) {
                return state;
            }
        }

        return MultipointCommStateCode.Unknown;
    }
}
