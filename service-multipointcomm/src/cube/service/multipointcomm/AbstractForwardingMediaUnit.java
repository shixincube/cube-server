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
import cube.service.multipointcomm.signaling.CandidateSignaling;
import cube.service.multipointcomm.signaling.OfferSignaling;

/**
 * 抽象的媒体单元。
 */
public abstract class AbstractForwardingMediaUnit {

    public AbstractForwardingMediaUnit() {
    }

    /**
     * 准备数据通道。
     *
     * @param commField
     * @param endpoint
     */
    public abstract void preparePipeline(CommField commField, CommFieldEndpoint endpoint);

    /**
     * 接收指定信令描述的数据流。
     *
     * @param commField
     * @param endpoint
     * @param signaling
     * @param callback
     * @return
     */
    public abstract MultipointCommStateCode receiveFrom(CommField commField,
                                                        CommFieldEndpoint endpoint,
                                                        OfferSignaling signaling,
                                                        MediaUnitCallback callback);

    /**
     * 取消指定目标的数据接收。
     *
     * @param commField
     * @param endpoint
     * @param target
     * @param callback
     * @return
     */
    public abstract MultipointCommStateCode cancelFrom(CommField commField,
                                                       CommFieldEndpoint endpoint,
                                                       CommFieldEndpoint target,
                                                       MediaUnitCallback callback);

    /**
     * 添加 ICE Candidate 到指定终端。
     *
     * @param commField
     * @param endpoint
     * @param signaling
     * @return
     */
    public abstract MultipointCommStateCode addCandidate(CommField commField,
                                                         CommFieldEndpoint endpoint, CandidateSignaling signaling);

    /**
     * 从通讯域移除终端。
     *
     * @param commField
     * @param endpoint
     * @return
     */
    public abstract MultipointCommStateCode removeEndpoint(CommField commField, CommFieldEndpoint endpoint);

    /**
     * 释放通讯域的数据通道。
     *
     * @param commField
     */
    public abstract MultipointCommStateCode release(CommField commField);

    /**
     * 销毁当前媒体单元。
     */
    public abstract void destroy();


    /**
     * {@inheritDoc}
     */
    public void onTick(long now) {

    }
}
