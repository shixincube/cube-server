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

package cube.service.multipointcomm;

import cell.util.CachedQueueExecutor;
import cell.util.Utils;
import cell.util.log.Logger;
import cube.common.action.MultipointCommAction;
import cube.common.entity.CommField;
import cube.common.entity.CommFieldEndpoint;
import cube.common.state.MultipointCommStateCode;
import cube.service.multipointcomm.signaling.CandidateSignaling;
import cube.service.multipointcomm.signaling.OfferSignaling;
import cube.service.multipointcomm.signaling.Signaling;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * 媒体单元的主机。
 */
public class MediaUnitLeader implements MediaUnitListener {

    private ExecutorService executor;

    private List<AbstractMediaUnit> mediaUnitList;

    private ConcurrentHashMap<Long, MediaUnitBundle> bundles;

    /**
     * 构造函数。
     */
    public MediaUnitLeader() {
        this.mediaUnitList = new ArrayList<>();
        this.bundles = new ConcurrentHashMap<>();
    }

    /**
     * 启动。
     */
    public void start(MultipointCommService service, Properties properties) {
        this.executor = CachedQueueExecutor.newCachedQueueThreadPool(10);

        // 读取配置
        this.loadMediaUnit(properties, service);
    }

    /**
     * 停止。
     */
    public void stop() {
        for (AbstractMediaUnit mu : this.mediaUnitList) {
            try {
                mu.destroy();
            } catch (Throwable e) {
                Logger.w(this.getClass(), "stop", e);
            }
        }

        this.mediaUnitList.clear();

        this.executor.shutdown();
    }

    /**
     * 分配媒体单元。
     *
     * @param commField
     * @return
     */
    public AbstractMediaUnit assign(CommField commField) {
        AbstractMediaUnit mediaUnit = this.selectMediaUnit(commField);
        return mediaUnit;
    }

    /**
     * 向 Media Unit 分发信令。
     *
     * @param commField
     * @param endpoint
     * @param signaling
     * @param signalingCallback
     */
    public void dispatch(CommField commField, CommFieldEndpoint endpoint, Signaling signaling, SignalingCallback signalingCallback) {
        AbstractMediaUnit mediaUnit = this.queryMediaUnit(commField);
        if (null == mediaUnit) {
            signalingCallback.on(MultipointCommStateCode.NoMediaUnit, signaling);
            return;
        }

        if (MultipointCommAction.Candidate.name.equals(signaling.getName())) {
            // 处理 ICE Candidate
            MultipointCommStateCode stateCode = mediaUnit.addCandidate(commField, endpoint, (CandidateSignaling) signaling);
            if (stateCode != MultipointCommStateCode.Ok) {
                Logger.w(this.getClass(), "Endpoint \"" + endpoint.getName() + "\" add addCandidate failed");
            }
        }
        else if (MultipointCommAction.Offer.name.equals(signaling.getName())) {
            // 从媒体单元接收数据
            MultipointCommStateCode stateCode = mediaUnit.receiveFrom(commField, endpoint, (OfferSignaling) signaling);
            // 回调
            signalingCallback.on(stateCode, signaling);
        }
        else if (MultipointCommAction.Bye.name.equals(signaling.getName())) {
            // 关闭指定的终端媒体
            MultipointCommStateCode stateCode = mediaUnit.removeEndpoint(commField, endpoint);
            // 回调
            signalingCallback.on(stateCode, signaling);
        }
        else {
            signalingCallback.on(MultipointCommStateCode.UnsupportedSignaling, signaling);
        }
    }

    /**
     * 释放指定的场域。
     *
     * @param commField
     * @return
     */
    public boolean release(CommField commField) {
        AbstractMediaUnit mediaUnit = this.queryMediaUnit(commField);
        if (null == mediaUnit) {
            return false;
        }

        // 释放当前场域上的所有终端资源
        MultipointCommStateCode stateCode = mediaUnit.release(commField);
        return stateCode == MultipointCommStateCode.Ok;
    }

    /**
     * 选择媒体单元节点。
     *
     * @param commField
     * @return
     */
    private AbstractMediaUnit selectMediaUnit(CommField commField) {
        MediaUnitBundle bundle = this.bundles.get(commField.getId());

        if (null == bundle) {
            // 随机媒体单元，需要通过服务能力进行选择
            int index = Utils.randomInt(0, this.mediaUnitList.size() - 1);
            AbstractMediaUnit mediaUnit = this.mediaUnitList.get(index);
            bundle = new MediaUnitBundle(mediaUnit, commField);
            this.bundles.put(commField.getId(), bundle);
        }

        return bundle.mediaUnit;
    }

    /**
     * 查找已分配的媒体单元。
     *
     * @param commField
     * @return
     */
    private AbstractMediaUnit queryMediaUnit(CommField commField) {
        MediaUnitBundle bundle = this.bundles.get(commField.getId());
        if (null == bundle) {
            return null;
        }

        return bundle.mediaUnit;
    }

    private void loadMediaUnit(Properties properties, MultipointCommService service) {
        // 读取 Unit 配置
        for (int i = 1; i <= 50; ++i) {
            String keyUrl = "unit." + i + ".kms.url";
            if (properties.containsKey(keyUrl)) {
                KurentoMediaUnit kurentoMediaUnit = new KurentoMediaUnit(service, new Portal() {
                    @Override
                    public void emit(CommFieldEndpoint endpoint, Signaling signaling) {
                        service.pushSignaling(endpoint, signaling);
                    }

                    @Override
                    public CommField getCommField(Long commFieldId) {
                        return service.getCommField(commFieldId);
                    }
                }, properties.getProperty(keyUrl), this.executor);
                this.mediaUnitList.add(kurentoMediaUnit);
            }
        }
    }

    @Override
    public Signaling onSignaling(Signaling signaling) {

        return signaling;
    }

    public void onTick(long now) {
        for (AbstractMediaUnit mediaUnit : this.mediaUnitList) {
            mediaUnit.onTick(now);
        }
    }
}
