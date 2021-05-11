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

package cube.service.multipointcomm;

import cube.common.entity.CommField;
import cube.common.entity.CommFieldEndpoint;
import cube.common.state.MultipointCommStateCode;
import org.json.JSONObject;

/**
 * 媒体单元。
 */
public interface MediaUnit {

    /**
     * 订阅指定目标的数据流。
     *
     * @param commField
     * @param endpoint
     * @param target
     * @param offerSDP
     * @param callback
     * @return
     */
    public MultipointCommStateCode subscribe(CommField commField, CommFieldEndpoint endpoint,
            CommFieldEndpoint target, String offerSDP, MediaUnitCallback callback);

    /**
     * 取消定于指定目标的数据流。
     *
     * @param commField
     * @param endpoint
     * @param target
     * @param callback
     * @return
     */
    public MultipointCommStateCode unsubscribe(CommField commField, CommFieldEndpoint endpoint,
            CommFieldEndpoint target, MediaUnitCallback callback);

    /**
     * 订阅指定场域上的输出端口的数据流。
     *
     * @param commField
     * @param endpoint
     * @param offerSDP
     * @param callback
     * @return
     */
    public MultipointCommStateCode subscribe(CommField commField, CommFieldEndpoint endpoint,
            String offerSDP, MediaUnitCallback callback);

    /**
     * 向指定目标添加 ICE candidate 数据。
     *
     * @param commField
     * @param endpoint
     * @param target
     * @param candidate
     * @return
     */
    public MultipointCommStateCode addCandidate(CommField commField, CommFieldEndpoint endpoint,
            CommFieldEndpoint target, JSONObject candidate);

    /**
     * 移除指定的终端的所有数据流。
     *
     * @param commField
     * @param endpoint
     * @return
     */
    public MultipointCommStateCode removeEndpoint(CommField commField, CommFieldEndpoint endpoint);

    /**
     * 释放通讯域的数据通道。
     *
     * @param commField
     * @return
     */
    public MultipointCommStateCode release(CommField commField);

    /**
     * 销毁当前媒体单元。
     */
    public void destroy();
}
