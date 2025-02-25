/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.multipointcomm;

import cell.util.CachedQueueExecutor;
import cell.util.Utils;
import cell.util.log.Logger;
import cube.common.action.MultipointCommAction;
import cube.common.entity.CommField;
import cube.common.entity.CommFieldEndpoint;
import cube.common.state.MultipointCommStateCode;
import cube.service.multipointcomm.signaling.ByeSignaling;
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

    private List<AbstractForwardingMediaUnit> forwardingMediaUnitList;

    private List<AbstractCompositeMediaUnit> compositeMediaUnitList;

    private ConcurrentHashMap<Long, MediaUnitBundle> bundles;

    private SnapshotDaemon snapshotDaemon;

    /**
     * 构造函数。
     */
    public MediaUnitLeader() {
        this.forwardingMediaUnitList = new ArrayList<>();
        this.compositeMediaUnitList = new ArrayList<>();
        this.bundles = new ConcurrentHashMap<>();
    }

    /**
     * 启动。
     */
    public void start(MultipointCommService service, Properties properties) {
        this.executor = CachedQueueExecutor.newCachedQueueThreadPool(10);

        // 读取配置
        this.loadMediaUnit(properties, service);

        ArrayList<MediaUnit> list = new ArrayList<>(this.compositeMediaUnitList);
        list.addAll(this.forwardingMediaUnitList);
        this.snapshotDaemon = new SnapshotDaemon(this.executor, list);
        this.snapshotDaemon.start();
    }

    /**
     * 停止。
     */
    public void stop() {
        for (AbstractForwardingMediaUnit mu : this.forwardingMediaUnitList) {
            try {
                mu.destroy();
            } catch (Throwable e) {
                Logger.w(this.getClass(), "stop", e);
            }
        }

        for (AbstractCompositeMediaUnit cmu : this.compositeMediaUnitList) {
            try {
                cmu.destroy();
            } catch (Throwable e) {
                Logger.w(this.getClass(), "stop", e);
            }
        }

        this.forwardingMediaUnitList.clear();
        this.compositeMediaUnitList.clear();

        if (null != this.executor) {
            this.executor.shutdown();
        }

        if (null != this.snapshotDaemon) {
            this.snapshotDaemon.stop();
            this.snapshotDaemon = null;
        }
    }

    /**
     * 分配媒体单元。
     *
     * @param commField
     * @return
     */
    public AbstractForwardingMediaUnit assignForwardingUnit(CommField commField) {
        AbstractForwardingMediaUnit mediaUnit = (AbstractForwardingMediaUnit)
                this.selectMediaUnit(commField, MediaUnitType.Forwarding);
        return mediaUnit;
    }

    /**
     * 分配媒体单元。
     *
     * @param commField
     * @return
     */
    public AbstractCompositeMediaUnit assignCompositeUnit(CommField commField) {
        AbstractCompositeMediaUnit mediaUnit = (AbstractCompositeMediaUnit)
                this.selectMediaUnit(commField, MediaUnitType.Composite);
        return mediaUnit;
    }

    /**
     * 向 Media Unit 分发信令。
     *
     * @param commField
     * @param endpoint
     * @param signaling
     * @param processCallback
     * @param completeCallback
     */
    public void dispatch(CommField commField, CommFieldEndpoint endpoint, Signaling signaling,
                         SignalingCallback processCallback, MediaUnitCallback completeCallback) {
        MediaUnit mediaUnit = this.queryMediaUnit(commField);
        if (null == mediaUnit) {
            processCallback.on(MultipointCommStateCode.NoMediaUnit, signaling);
            return;
        }

        if (MultipointCommAction.Candidate.name.equals(signaling.getName())) {
            // 处理 ICE Candidate
            CandidateSignaling candidateSignaling = (CandidateSignaling) signaling;

            MultipointCommStateCode stateCode = null;
            CommFieldEndpoint target = candidateSignaling.getTarget();

            if (null != target) {
                stateCode = mediaUnit.addCandidate(commField, endpoint, target,
                        candidateSignaling.getCandidate());
            }
            else {
                stateCode = mediaUnit.addCandidate(candidateSignaling.getSN(), commField, endpoint,
                        candidateSignaling.getCandidate());
            }

            if (stateCode != MultipointCommStateCode.Ok) {
                Logger.w(this.getClass(), "Endpoint \"" + endpoint.getName() + "\" add addCandidate failed");
            }
        }
        else if (MultipointCommAction.Offer.name.equals(signaling.getName())) {
            MultipointCommStateCode stateCode = null;

            OfferSignaling offerSignaling = (OfferSignaling) signaling;
            CommFieldEndpoint target = offerSignaling.getTarget();
            if (null != target) {
                // 从媒体单元接收数据
                stateCode = mediaUnit.subscribe(offerSignaling.getSN(), commField, endpoint,
                        target, offerSignaling.getSDP(), completeCallback);
            }
            else {
                // 从媒体单元接收数据
                // 订阅场域的混码流
                stateCode = mediaUnit.subscribe(offerSignaling.getSN(), commField, endpoint,
                        offerSignaling.getSDP(), completeCallback);
            }

            // 回调进行应答
            OfferSignaling ackSignaling = new OfferSignaling(offerSignaling.getSN(), commField,
                    endpoint.getContact(), endpoint.getDevice());
            processCallback.on(stateCode, ackSignaling);
        }
        else if (MultipointCommAction.Bye.name.equals(signaling.getName())) {
            ByeSignaling byeSignaling = (ByeSignaling) signaling;

            CommFieldEndpoint target = byeSignaling.getTarget();

            ByeSignaling ackSignaling = new ByeSignaling(commField, endpoint.getContact(), endpoint.getDevice());
            ackSignaling.setTarget(target);

            if (null == target) {
                // 关闭指定的终端媒体
                MultipointCommStateCode stateCode = mediaUnit.removeEndpoint(commField, endpoint);
                // 回调
                processCallback.on(stateCode, ackSignaling);
                this.executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        completeCallback.on(commField, endpoint);
                    }
                });
            }
            else {
                // 取消对指定终端的数据接收
                MultipointCommStateCode stateCode = mediaUnit.unsubscribe(commField, endpoint,
                        target, completeCallback);
                // 回调
                processCallback.on(stateCode, ackSignaling);
            }
        }
        else {
            // 回调
            processCallback.on(MultipointCommStateCode.UnsupportedSignaling, signaling);
        }
    }

    /**
     * 释放指定的场域。
     *
     * @param commField
     * @return
     */
    public boolean release(CommField commField) {
        MediaUnit mediaUnit = this.queryMediaUnit(commField);
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
    private MediaUnit selectMediaUnit(CommField commField, MediaUnitType type) {
        MediaUnitBundle bundle = this.bundles.get(commField.getId());

        if (null == bundle) {
            if (MediaUnitType.Forwarding == type) {
                // 随机媒体单元，需要通过服务能力进行选择
                int index = Utils.randomInt(0, this.forwardingMediaUnitList.size() - 1);
                AbstractForwardingMediaUnit mediaUnit = this.forwardingMediaUnitList.get(index);
                bundle = new MediaUnitBundle(mediaUnit, commField);
                this.bundles.put(commField.getId(), bundle);

                return bundle.forwardingMediaUnit;
            }
            else if (MediaUnitType.Composite == type) {
                // 随机媒体单元，需要通过服务能力进行选择
                int index = Utils.randomInt(0, this.compositeMediaUnitList.size() - 1);
                AbstractCompositeMediaUnit mediaUnit = this.compositeMediaUnitList.get(index);
                bundle = new MediaUnitBundle(mediaUnit, commField);
                this.bundles.put(commField.getId(), bundle);

                return bundle.compositeMediaUnit;
            }
        }
        else {
            if (MediaUnitType.Forwarding == type) {
                return bundle.forwardingMediaUnit;
            }
            else if (MediaUnitType.Composite == type) {
                return bundle.compositeMediaUnit;
            }
        }

        return null;
    }

    /**
     * 查找已分配的媒体单元。
     *
     * @param commField
     * @return
     */
    private MediaUnit queryMediaUnit(CommField commField) {
        MediaUnitBundle bundle = this.bundles.get(commField.getId());
        if (null == bundle) {
            return null;
        }

        if (null != bundle.forwardingMediaUnit) {
            return bundle.forwardingMediaUnit;
        }
        else {
            return bundle.compositeMediaUnit;
        }
    }

    private void loadMediaUnit(Properties properties, MultipointCommService service) {
        Portal portal = new Portal() {
            @Override
            public void emit(CommFieldEndpoint endpoint, Signaling signaling) {
                service.pushSignaling(endpoint, signaling);
            }

            @Override
            public CommField getCommField(Long commFieldId) {
                return service.getCommField(commFieldId);
            }
        };

        // 读取 Unit 配置
        for (int i = 1; i <= 50; ++i) {
            String keyUrl = "unit." + i + ".kms.url";
            if (properties.containsKey(keyUrl)) {
                // Forwarding Media Unit
                KurentoForwardingMediaUnit forwardingMediaUnit = new KurentoForwardingMediaUnit(service, portal,
                        properties.getProperty(keyUrl), this.executor);
                this.forwardingMediaUnitList.add(forwardingMediaUnit);

                // Composite Media Unit
                KurentoCompositeMediaUnit compositeMediaUnit = new KurentoCompositeMediaUnit(service, portal,
                        properties.getProperty(keyUrl), this.executor);
                this.compositeMediaUnitList.add(compositeMediaUnit);
            }
        }
    }

    @Override
    public void onPipelineCreated(CommField field) {
        // Nothing
    }

    @Override
    public void onPipelineReleased(CommField field) {
        // Nothing
    }

    public void onTick(long now) {
        for (AbstractForwardingMediaUnit mediaUnit : this.forwardingMediaUnitList) {
            mediaUnit.onTick(now);
        }

        for (AbstractCompositeMediaUnit mediaUnit : this.compositeMediaUnitList) {
            mediaUnit.onTick(now);
        }
    }
}
