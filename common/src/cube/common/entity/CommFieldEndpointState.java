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
 * 通信节点状态。
 */
public enum CommFieldEndpointState {

    /**
     * 正常状态。
     */
    Normal(0),

    /**
     * 正在建立通话。
     */
    Calling(10),

    /**
     * 当前线路忙。
     */
    Busy(11),

    /**
     * 通话已接通。
     */
    CallConnected(13),

    /**
     * 通话结束。
     */
    CallBye(15),

    /**
     * 未知的状态。
     */
    Unknown(99);

    public final int code;

    CommFieldEndpointState(int code) {
        this.code = code;
    }

    public final static CommFieldEndpointState parse(int code) {
        for (CommFieldEndpointState state : CommFieldEndpointState.values()) {
            if (state.code == code) {
                return state;
            }
        }

        return CommFieldEndpointState.Unknown;
    }
}
