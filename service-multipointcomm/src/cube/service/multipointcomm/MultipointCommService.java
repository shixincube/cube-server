/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.multipointcomm;

import cell.adapter.CelletAdapter;
import cell.adapter.CelletAdapterFactory;
import cell.adapter.CelletAdapterListener;
import cell.core.net.Endpoint;
import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.util.log.Logger;
import cube.common.ModuleEvent;
import cube.common.Packet;
import cube.common.action.ContactAction;
import cube.common.action.MultipointCommAction;
import cube.common.entity.*;
import cube.common.state.MultipointCommStateCode;
import cube.core.*;
import cube.plugin.PluginSystem;
import cube.service.Director;
import cube.service.contact.ContactManager;
import cube.service.multipointcomm.event.AudioMuted;
import cube.service.multipointcomm.event.CommFieldUpdate;
import cube.service.multipointcomm.event.MicrophoneVolume;
import cube.service.multipointcomm.event.VideoMuted;
import cube.service.multipointcomm.signaling.*;
import cube.util.DummyDevice;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * 多方通讯服务。
 */
public class MultipointCommService extends AbstractModule implements CelletAdapterListener {

    /**
     * 服务单元名。
     */
    public final static String NAME = "MultipointComm";

    /**
     *
     */
    private MultipointCommServiceCellet cellet;

    /**
     * 联系人事件适配器。
     */
    private CelletAdapter contactsAdapter;

    /**
     * 线程池执行器。
     */
    private ExecutorService executor;

    /**
     * 定时器线程池。
     */
    private ScheduledExecutorService scheduledExecutor;

    /**
     * 场域的缓存时长。
     */
    private long fieldLifespan = 12L * 60L * 60L * 1000L;

    /**
     * Comm Field 映射。
     */
    private ConcurrentHashMap<Long, CommField> commFieldMap;

    /**
     * 缓存。
     */
    private Cache cache;

    /**
     * 媒体单元的管理器。
     */
    private MediaUnitLeader mediaUnitLeader;

    public MultipointCommService(MultipointCommServiceCellet cellet) {
        this.cellet = cellet;
        this.commFieldMap = new ConcurrentHashMap<>();
        this.mediaUnitLeader = new MediaUnitLeader();
    }

    @Override
    public void start() {
        this.contactsAdapter = CelletAdapterFactory.getInstance().getAdapter("Contacts");
        this.contactsAdapter.addListener(this);

        this.executor = Executors.newCachedThreadPool();

        this.scheduledExecutor = Executors.newScheduledThreadPool(16);

        this.cache = this.getKernel().getCache("General");

        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                // 读取 Media Unit 配置
                Properties properties = loadConfig();
                mediaUnitLeader.start(MultipointCommService.this, properties);

                started.set(true);
            }
        });
    }

    @Override
    public void stop() {
        if (null != this.contactsAdapter) {
            this.contactsAdapter.removeListener(this);
        }

        if (null != this.scheduledExecutor) {
            this.scheduledExecutor.shutdown();
        }

        if (null != this.executor) {
            this.executor.shutdown();
        }

        this.mediaUnitLeader.stop();

        this.started.set(false);
    }

    @Override
    public PluginSystem getPluginSystem() {
        return null;
    }

    @Override
    public void onTick(cube.core.Module module, Kernel kernel) {
        long now = System.currentTimeMillis();

        ArrayList<CommField> commFields = new ArrayList<>();
        Iterator<CommField> fiter = this.commFieldMap.values().iterator();
        while (fiter.hasNext()) {
            CommField field = fiter.next();
            if ((field.numEndpoints() == 0 && now - field.getTimestamp() > 5000) || now - field.getTimestamp() > this.fieldLifespan) {
                commFields.add(field);
                fiter.remove();
            }
        }

        for (CommField commField : commFields) {
            this.cache.remove(new CacheKey(commField.getId()));

            // 如果域里没有终端，则重置群组的数据
            if (null != commField.getGroup()) {
                // 记录结束时间
                commField.stopTiming();

                Group group = commField.getGroup();
                group = ContactManager.getInstance().getGroup(group.getId(), group.getDomain().getName());
                GroupAppendix appendix = ContactManager.getInstance().getAppendix(group);
                appendix.setCommId(0L);
                ContactManager.getInstance().updateAppendix(group, appendix, true);
            }

            this.mediaUnitLeader.release(commField);

            commField.clearTraces();
        }

        this.mediaUnitLeader.onTick(now);
    }

    private Properties loadConfig() {
        String[] files = new String[]{
                "config/multipoint-comm_dev.properties",
                "multipoint-comm_dev.properties",
                "config/multipoint-comm.properties",
                "multipoint-comm.properties"
        };

        File file = null;
        for (String filename : files) {
            file = new File(filename);
            if (file.exists()) {
                break;
            }
        }

        FileInputStream fis = null;
        Properties properties = new Properties();
        try {
            fis = new FileInputStream(file);
            properties.load(fis);
        } catch (IOException e) {
            Logger.e(this.getClass(), "Can NOT find config file", e);
        } finally {
            if (null != fis) {
                try {
                    fis.close();
                } catch (IOException e) {
                }
            }
        }

        return properties;
    }

    /**
     * 获取指定的场域。
     *
     * @param commFieldId
     * @return
     */
    public CommField getCommField(Long commFieldId) {
        CommField field = this.commFieldMap.get(commFieldId);
        if (null == field) {
            CacheValue value = this.cache.get(new CacheKey(commFieldId));
            if (null == value) {
                return null;
            }
            else {
                JSONObject json = value.get();
                field = new CommField(json);
                this.commFieldMap.put(field.getId(), field);
                return field;
            }
        }
        else {
            return field;
        }
    }

    /**
     * 创建场域。
     *
     * @param commField
     * @return
     */
    public CommField createCommField(CommField commField) {
        this.commFieldMap.put(commField.getId(), commField);
        this.cache.put(new CacheKey(commField.getId()), new CacheValue(commField.toJSON()));
        return commField;
    }

    /**
     * 销毁场域。
     *
     * @param commFieldId
     * @return
     */
    public CommField destroyCommField(Long commFieldId) {
        CommField commField = this.getCommField(commFieldId);
        if (null != commField) {
            // 释放资源
            if (this.mediaUnitLeader.release(commField)) {
                Logger.i(this.getClass(), "Destroy comm field : " + commField.getName());
            }

            this.commFieldMap.remove(commFieldId);
            this.cache.remove(new CacheKey(commFieldId));
        }
        return commField;
    }

    protected void updateCommField(CommField commField, boolean onlyCache) {
        if (!onlyCache) {
            this.commFieldMap.put(commField.getId(), commField);
        }

        this.cache.put(new CacheKey(commField.getId()), new CacheValue(commField.toJSON()));
    }

    /**
     * 申请呼叫。
     *
     * @param commField
     * @param participant
     * @param device
     * @return
     */
    public MultipointCommStateCode applyCall(CommField commField, Contact participant, Device device) {
        CommField current = null;
        if (commField.isPrivate()) {
            // 开始计时
            commField.tryTiming();

            current = this.getCommField(commField.getId());
            if (null == current) {
                current = commField;
                this.updateCommField(current, false);
            }
        }
        else {
            current = this.getCommField(commField.getId());
            if (null == current) {
                return MultipointCommStateCode.NoCommField;
            }
        }

        // 更新终端
        CommFieldEndpoint endpoint = current.getEndpoint(participant, device);
        if (null == endpoint) {
            Long endpointId = this.makeCommFieldEndpointId(participant, device);
            endpoint = new CommFieldEndpoint(endpointId, participant, device);
            // 添加主叫终端
            current.addEndpoint(endpoint);

            this.updateCommField(current, true);
        }

        if (current.isPrivate()) {
            // 私域

            // 目标
            Contact target = commField.getCallee();

            // 判断是否被联系人阻止
            boolean blocked = ContactManager.getInstance().hasBlocked(participant.getDomain().getName(), participant.getId(), target.getId());
            if (blocked) {
                // 主叫端阻止了被叫端
                return MultipointCommStateCode.BeCallerBlocked;
            }

            blocked = ContactManager.getInstance().hasBlocked(participant.getDomain().getName(), target.getId(), participant.getId());
            if (blocked) {
                // 被叫端阻止了主叫端
                return MultipointCommStateCode.BeCalleeBlocked;
            }

            CommFieldEndpoint ep = current.getEndpoint(participant);
            if (null != ep && ep.getState() != CommFieldEndpointState.CallBye) {
                // 主叫忙
                return MultipointCommStateCode.CallerBusy;
            }

            // 为被叫准备 CommField
            CommField calleeField = this.getCommField(target.getId());
            if (null == calleeField) {
                calleeField = new CommField(target.getId(), current.getDomain().getName(),
                        ContactManager.getInstance().getContact(current.getDomain().getName(), target.getId()));
                calleeField.addEndpoint(endpoint);
                this.updateCommField(calleeField, false);
            }

            if (calleeField.isSingleCalling(target)) {
                // 被叫忙
                return MultipointCommStateCode.CalleeBusy;
            }

            // 标记 Call ，复制主被叫信息
            current.markSingleCalling(commField);
            this.updateCommField(current, true);

            // 标记 Call ，复制主被叫信息
            calleeField.markSingleCalling(commField);
            this.updateCommField(calleeField, true);

            Logger.i(this.getClass(), "#applyCall() - " + participant.getId() + " -> " + target.getId());

            return MultipointCommStateCode.Ok;
        }
        else {
            // 分配媒体单元
            MediaConstraint mediaConstraint = current.getMediaConstraint();
            if (mediaConstraint.videoEnabled()) {
                AbstractForwardingMediaUnit mediaUnit = this.mediaUnitLeader.assignForwardingUnit(commField);

                // 准备通道
                mediaUnit.preparePipeline(commField, endpoint);
            }
            else {
                AbstractCompositeMediaUnit mediaUnit = this.mediaUnitLeader.assignCompositeUnit(commField);

                // 准备通道
                mediaUnit.preparePipeline(commField, endpoint);
            }

            return MultipointCommStateCode.Ok;
        }
    }

    /**
     * 申请终端进入场域。
     *
     * @param commField
     * @param participant
     * @param device
     * @return
     */
    public MultipointCommStateCode applyJoin(CommField commField, Contact participant, Device device) {
        CommField current = null;
        if (commField.isPrivate()) {
            current = this.getCommField(commField.getId());
            if (null == current) {
                current = commField;
                this.updateCommField(current, false);
            }
        }
        else {
            current = this.getCommField(commField.getId());
            if (null == current) {
                return MultipointCommStateCode.NoCommField;
            }
        }

        // 更新终端
        CommFieldEndpoint endpoint = current.getEndpoint(participant, device);
        if (null == endpoint) {
            Long endpointId = this.makeCommFieldEndpointId(participant, device);
            endpoint = new CommFieldEndpoint(endpointId, participant, device);
            // 添加主叫终端
            current.addEndpoint(endpoint);

            this.updateCommField(current, true);
        }

        if (current.isPrivate()) {
            // 校验
            // 自己是否是被叫
            if (!current.isCallee(participant)) {
                return MultipointCommStateCode.CommFieldStateError;
            }

            Logger.i(this.getClass(), "#applyJoin() - " + participant.getId() + " (" + device.toString() + ")");
        }
        else {
            // 分配媒体单元
            MediaConstraint mediaConstraint = current.getMediaConstraint();
            if (mediaConstraint.videoEnabled()) {
                AbstractForwardingMediaUnit mediaUnit = this.mediaUnitLeader.assignForwardingUnit(commField);

                // 准备通道
                mediaUnit.preparePipeline(commField, endpoint);
            }
            else {
                AbstractCompositeMediaUnit mediaUnit = this.mediaUnitLeader.assignCompositeUnit(commField);

                // 准备通道
                mediaUnit.preparePipeline(commField, endpoint);
            }

            return MultipointCommStateCode.Ok;
        }

        return MultipointCommStateCode.Ok;
    }

    /**
     * 申请终止呼叫记录。
     *
     * @param commField
     * @param participant
     * @param device
     * @return
     */
    public MultipointCommStateCode applyTerminate(CommField commField, Contact participant, Device device) {
        CommField current = null;
        if (commField.isPrivate()) {
            current = this.getCommField(commField.getId());
            if (null == current) {
                current = commField;
                this.updateCommField(current, false);
            }
        }
        else {
            current = this.getCommField(commField.getId());
            if (null == current) {
                return MultipointCommStateCode.NoCommField;
            }
        }

        if (current.isPrivate()) {
            // 主叫
            Contact caller = current.getCaller();
            if (null == caller) {
                return MultipointCommStateCode.NoCommFieldEndpoint;
            }
            // 被叫
            Contact callee = current.getCallee();
            if (null == callee) {
                return MultipointCommStateCode.NoCommFieldEndpoint;
            }

            if (caller.equals(participant)) {
                // 申请人是主叫，则校验被叫的状态
                // 为被叫准备 CommField
                CommField calleeField = this.getCommField(callee.getId());
                if (null != calleeField) {
                    Contact callerInCallee = calleeField.getCaller();
                    if (callerInCallee.equals(caller)) {
                        // 被叫记录的主叫信息一致
                        calleeField.clearAll();

                        this.updateCommField(calleeField, true);

                        Logger.i(this.getClass(), "#applyTerminate() [caller] " + participant.getId() + " (" + device.toString() + ")");
                    }
                }
            }
            else if (callee.equals(participant)) {
                // 申请人是被叫，则校验主叫的状态
                CommField callerField = this.getCommField(caller.getId());
                if (null != callerField) {
                    Contact calleeInCaller = callerField.getCallee();
                    if (calleeInCaller.equals(callee)) {
                        // 主叫记录的被叫信息一致
                        callerField.clearAll();

                        this.updateCommField(callerField, true);

                        Logger.i(this.getClass(), "#applyTerminate() [callee] " + participant.getId() + " (" + device.toString() + ")");
                    }
                }
            }

            current.clearAll();
            this.updateCommField(current, true);
        }
        else {
            CommFieldEndpoint endpoint = current.getEndpoint(participant, device);
            if (null != endpoint) {
                current.removeEndpoint(endpoint);

                this.updateCommField(current, true);
            }
        }

        return MultipointCommStateCode.Ok;
    }

    /**
     * 处理 Offer 信令。
     *
     * @param signaling
     * @param callback
     */
    public synchronized void processOffer(OfferSignaling signaling, SignalingCallback callback) {
        CommField current = this.getCommField(signaling.getField().getId());
        if (null == current) {
            callback.on(MultipointCommStateCode.NoCommField, signaling);
            return;
        }

        // 更新终端
        CommFieldEndpoint endpoint = current.getEndpoint(signaling.getContact(), signaling.getDevice());
        if (null == endpoint) {
            Long endpointId = this.makeCommFieldEndpointId(signaling.getContact(), signaling.getDevice());
            endpoint = new CommFieldEndpoint(endpointId, signaling.getContact(), signaling.getDevice());
            // 添加主叫终端
            current.addEndpoint(endpoint);
            this.updateCommField(current, true);
        }

        endpoint.setSessionDescription(signaling.getSessionDescription());
        endpoint.setMediaConstraint(signaling.getMediaConstraint());

        // 更新信令的 Field
        signaling.setField(current);

        if (current.isPrivate()) {
            if (!current.isSingleCalling(signaling.getContact())) {
                callback.on(MultipointCommStateCode.CommFieldStateError, signaling);
                return;
            }

            endpoint.setState(CommFieldEndpointState.Calling);

            Logger.i(this.getClass(), "Offer: " + current.getCaller().getId() + " -> " + current.getCallee().getId());

            // 主叫
            signaling.setCaller(current.getCaller());

            // 被叫
            signaling.setCallee(current.getCallee());

            // 被叫的域
            CommField calleeField = this.getCommField(current.getCallee().getId());

            // 推送信令
            OfferSignaling toCallee = new OfferSignaling(signaling.getSN(), calleeField, calleeField.getFounder(),
                    new DummyDevice(null));
            toCallee.copy(signaling);
            // 信令事件
            ModuleEvent event = new ModuleEvent(MultipointCommService.NAME,
                    MultipointCommAction.Offer.name, toCallee.toJSON());
            // 向联系人推送信令
            this.contactsAdapter.publish(current.getCallee().getUniqueKey(), event.toJSON());

            final CommFieldEndpoint offerEndpoint = endpoint;
            // 追踪
            current.startTrace(this.scheduledExecutor, endpoint, new Runnable() {
                @Override
                public void run() {
                    fireOfferTimeout(current, offerEndpoint);
                }
            });

            callback.on(MultipointCommStateCode.Ok, signaling);
        }
        else {
            Logger.i(this.getClass(), "Offer: " + current.getId() + " from " + endpoint.getContact().getId());

            final CommFieldEndpoint currentEndpoint = endpoint;

            SignalingCallback processCallback = new SignalingCallback() {
                @Override
                public void on(MultipointCommStateCode stateCode, Signaling responseSignaling) {
                    if (null == responseSignaling) {
                        callback.on(MultipointCommStateCode.MediaUnitField, signaling);
                        return;
                    }

                    // 尝试开始计时
                    current.tryTiming();

                    // 回调
                    callback.on(stateCode, responseSignaling);
                }
            };

            MediaUnitCallback completeCallback = new MediaUnitCallback() {
                @Override
                public void on(CommField commField, CommFieldEndpoint endpoint) {
                    CommFieldEndpoint target = signaling.getTarget();
                    if (null != target) {
                        // 广播
                        if (target.getContact().getId().longValue() == signaling.getContact().getId().longValue()) {
                            broadcastArrivedEvent(current, currentEndpoint);
                        }
                    }
                }
            };

            // 由 Leader 派发信令
            this.mediaUnitLeader.dispatch(current, currentEndpoint, signaling, processCallback, completeCallback);
        }
    }

    /**
     * 处理 Answer 信令。
     *
     * @param signaling
     * @param callback
     */
    public void processAnswer(AnswerSignaling signaling, SignalingCallback callback) {
        CommField current = this.getCommField(signaling.getField().getId());
        if (null == current) {
            callback.on(MultipointCommStateCode.NoCommField, signaling);
            return;
        }

        // 更新终端
        CommFieldEndpoint endpoint = current.getEndpoint(signaling.getContact(), signaling.getDevice());
        if (null == endpoint) {
            Long endpointId = this.makeCommFieldEndpointId(signaling.getContact(), signaling.getDevice());
            endpoint = new CommFieldEndpoint(endpointId, signaling.getContact(), signaling.getDevice());
            // 添加被叫终端
            current.addEndpoint(endpoint);
        }

        endpoint.setSessionDescription(signaling.getSessionDescription());
        endpoint.setMediaConstraint(signaling.getMediaConstraint());

        // 更新信令的 Field
        signaling.setField(current);

        if (current.isPrivate()) {
            if (!current.isSingleCalling(signaling.getContact())) {
                // 没有呼叫信息
                callback.on(MultipointCommStateCode.CommFieldStateError, signaling);
                return;
            }

            // 停止超时追踪
            current.stopTrace(endpoint);

            // 修改被叫状态
            endpoint.setState(CommFieldEndpointState.CallConnected);

            Logger.i(this.getClass(), "Answer: " + current.getCallee().getId() + " -> " + current.getCaller().getId());

            Contact caller = current.getCaller();
            // 设置主叫
            signaling.setCaller(caller);
            // 设置被叫
            signaling.setCallee(signaling.getContact());

            // 获取主叫的 Comm Field
            CommField callerField = this.commFieldMap.get(caller.getId());
            CommFieldEndpoint callerEndpoint = callerField.getEndpoint(caller);
            // 停止超时追踪
            callerField.stopTrace(callerEndpoint);
            // 修改主叫状态
            callerEndpoint.setState(CommFieldEndpointState.CallConnected);

            // 向主叫发送 Answer
            AnswerSignaling toCaller = new AnswerSignaling(callerField,
                    callerEndpoint.getContact(), callerEndpoint.getDevice());
            toCaller.copy(signaling);
            // 事件
            ModuleEvent event = new ModuleEvent(MultipointCommService.NAME,
                    MultipointCommAction.Answer.name, toCaller.toJSON());
            // 推送给联系人
            this.contactsAdapter.publish(caller.getUniqueKey(), event.toJSON());

            // 向主叫推送 Candidate
            CandidateSignaling candidateSignaling = new CandidateSignaling(callerField,
                    callerEndpoint.getContact(), callerEndpoint.getDevice());
            // 填写被叫的 Candidates
            List<JSONObject> candidates = endpoint.getCandidates();
            if (Logger.isDebugLevel()) {
                Logger.d(this.getClass(), "#processAnswer candidate " + endpoint.getContact().getId()
                        + " -> " + caller.getId() + " : " + candidates.size());
            }
            candidateSignaling.setCandidateList(candidates);
            event = new ModuleEvent(MultipointCommService.NAME, MultipointCommAction.Candidate.name,
                    candidateSignaling.toJSON());
            this.contactsAdapter.publish(callerEndpoint.getContact().getUniqueKey(), event.toJSON());

            // 向被叫推送 Candidate
            candidateSignaling = new CandidateSignaling(current,
                    endpoint.getContact(), endpoint.getDevice());
            // 填写主叫的 Candidates
            candidates = callerEndpoint.getCandidates();
            if (Logger.isDebugLevel()) {
                Logger.d(this.getClass(), "#processAnswer candidate " + caller.getId()
                        + " -> " + endpoint.getContact().getId() + " : " + candidates.size());
            }
            candidateSignaling.setCandidateList(candidates);
            event = new ModuleEvent(MultipointCommService.NAME, MultipointCommAction.Candidate.name,
                    candidateSignaling.toJSON());
            this.contactsAdapter.publish(endpoint.getContact().getUniqueKey(), event.toJSON());

            callback.on(MultipointCommStateCode.Ok, signaling);

            // 设置就绪时间戳
            callerEndpoint.readyTimestamp = endpoint.readyTimestamp = System.currentTimeMillis();
        }
        else {
            Logger.i(this.getClass(), "Answer: " + current.getId() + " - " + signaling.getContact().getId());

//            this.mediaUnitLeader.dispatch(current, endpoint, signaling, callback);
        }
    }

    /**
     * 处理 Candidate 信令。
     *
     * @param signaling
     * @return
     */
    public MultipointCommStateCode processCandidate(CandidateSignaling signaling) {
        CommField current = this.getCommField(signaling.getField().getId());
        if (null == current) {
            return MultipointCommStateCode.NoCommField;
        }

        CommFieldEndpoint endpoint = current.getEndpoint(signaling.getContact(), signaling.getDevice());
        if (null == endpoint) {
            return MultipointCommStateCode.NoCommFieldEndpoint;
        }

        if (current.isPrivate()) {
            if (endpoint.getState() == CommFieldEndpointState.CallConnected) {
                // 已连接状态直接向对端发送 Candidate

                Contact peer = null;
                if (signaling.getContact().equals(current.getCallee())) {
                    // 当前为被叫端，向主叫端发送
                    peer = current.getCaller();
                }
                else {
                    // 当前为主叫端，向被叫端发送
                    peer = current.getCallee();
                }

                Logger.d(this.getClass(), "Transmit candidate: "
                        + signaling.getContact().getId() + " -> " + peer.getId());

                CommField peerField = this.commFieldMap.get(peer.getId());
                CommFieldEndpoint peerEndpoint = peerField.getEndpoint(peer);

                CandidateSignaling candidateSignaling = new CandidateSignaling(peerField,
                        peerEndpoint.getContact(), peerEndpoint.getDevice());
                candidateSignaling.setCandidate(signaling.getCandidate());

                ModuleEvent event = new ModuleEvent(MultipointCommService.NAME, MultipointCommAction.Candidate.name,
                        candidateSignaling.toJSON());
                this.contactsAdapter.publish(peer.getUniqueKey(), event.toJSON());
            }
            else {
                // 非连接状态时进行记录
                endpoint.addCandidate(signaling.getCandidate());

                Logger.d(this.getClass(), "Record candidate: "
                        + signaling.getContact().getId() + " " + signaling.getCandidate().toString());
            }
        }
        else {
            // 将信令转到媒体单元
            this.mediaUnitLeader.dispatch(current, endpoint, signaling, null, null);
        }

        return MultipointCommStateCode.Ok;
    }

    /**
     * 处理 Bye 信令。
     *
     * @param signaling
     * @parma callback
     */
    public void processBye(ByeSignaling signaling, SignalingCallback callback) {
        CommField current = this.getCommField(signaling.getField().getId());
        if (null == current) {
            callback.on(MultipointCommStateCode.NoCommField, signaling);
            return;
        }

        CommFieldEndpoint endpoint = current.getEndpoint(signaling.getContact(), signaling.getDevice());
        if (null == endpoint) {
            Logger.w(this.getClass(), "#processBye() - NoCommFieldEndpoint : "
                    + signaling.getContact().getId() + " - "
                    + signaling.getDevice().toString());

            callback.on(MultipointCommStateCode.NoCommFieldEndpoint, signaling);
            return;
        }

        if (current.isPrivate()) {
            Logger.i(this.getClass(), "Bye (Private): " + current.getFounder().getId());

            if (endpoint.getState() == CommFieldEndpointState.CallBye) {
                callback.on(MultipointCommStateCode.Ok, signaling);

                // 接收方收到 Bye，回复到服务器，删除对应的信息
                current.clearAll();
                current.clearTraces();

                this.updateCommField(current, true);

                return;
            }

            // 更新状态
            endpoint.setState(CommFieldEndpointState.CallBye);
            endpoint.readyTimestamp = 0L;
            endpoint.clearCandidates();

            // 停止追踪
            current.stopTrace(endpoint);

            // 停止计时
            current.stopTiming();

            Contact caller = current.getCaller();
            Contact callee = current.getCallee();
            Contact target = null;

            if (signaling.getContact().equals(caller)) {
                // 主叫发送的 Bye
                // 向被叫推送 Bye
                target = callee;
            }
            else {
                // 被叫发送的 Bye
                // 向主叫推送 Bye
                target = caller;
            }

            signaling.setCaller(caller);
            signaling.setCallee(callee);

            // 对方的通信场域
            CommField targetField = this.getCommField(target.getId());
            // 对方通信场域是否正在和当前场域通信
            if (null != targetField && (targetField.getCaller().equals(caller) || targetField.getCallee().equals(callee))) {
                CommFieldEndpoint targetEndpoint = targetField.getEndpoint(target);
                if (null != targetEndpoint) {
                    // 对方接听过
                    targetEndpoint.setState(CommFieldEndpointState.CallBye);
                    targetEndpoint.readyTimestamp = 0L;
                    targetEndpoint.clearCandidates();

                    targetField.stopTrace(targetEndpoint);

                    // 推送 Bye 信令给对方
                    ByeSignaling toTarget = new ByeSignaling(targetField,
                            targetEndpoint.getContact(), targetEndpoint.getDevice());
                    toTarget.copy(signaling);
                    ModuleEvent event = new ModuleEvent(MultipointCommService.NAME, toTarget.getName(), toTarget.toJSON());
                    this.contactsAdapter.publish(target.getUniqueKey(), event.toJSON());
                }
                else {
                    // 对方未接听
                    // 取消所有通话状态
                    targetField.clearCalling();
                }

                // 更新对方的场域
                this.updateCommField(targetField, true);

                // 清空 Calling 信息
                current.clearAll();
                current.clearTraces();

                this.updateCommField(current, true);

                // 回调，向自己回送 Bye
                callback.on(MultipointCommStateCode.Ok, signaling);
            }
            else {
                // 回调
                callback.on(MultipointCommStateCode.NoPeerEndpoint, signaling);
            }
        }
        else {
            Logger.i(this.getClass(), "Bye: " + signaling.getContact().getId());

            final CommFieldEndpoint currentEndpoint = endpoint;

            SignalingCallback processCallback = new SignalingCallback() {
                @Override
                public void on(MultipointCommStateCode stateCode, Signaling responseSignaling) {
                    if (null == responseSignaling) {
                        callback.on(MultipointCommStateCode.MediaUnitField, signaling);
                        return;
                    }

                    if (null == signaling.getTarget()) {
                        // 更新状态
                        endpoint.setState(CommFieldEndpointState.CallBye);
                        endpoint.clearCandidates();

                        // 移除
                        current.removeEndpoint(endpoint);

                        // 更新缓存
                        updateCommField(current, true);
                    }

                    // 回调
                    callback.on(stateCode, responseSignaling);
                }
            };

            MediaUnitCallback completeCallback = new MediaUnitCallback() {
                @Override
                public void on(CommField commField, CommFieldEndpoint endpoint) {
                    // 如果域里没有终端，则重置群组的数据
                    if (current.numEndpoints() == 0) {
                        // 记录结束时间
                        current.stopTiming();

                        Group group = current.getGroup();
                        if (null != group) {
                            group = ContactManager.getInstance().getGroup(group.getId(), group.getDomain().getName());
                            GroupAppendix appendix = ContactManager.getInstance().getAppendix(group);
                            appendix.setCommId(0L);
                            ContactManager.getInstance().updateAppendix(group, appendix, true);
                        }

                        // 更新缓存
                        updateCommField(current, true);
                    }

                    if (null == signaling.getTarget()) {
                        // 向场域里的其他终端发送事件
                        broadcastLeftEvent(current, currentEndpoint);
                    }
                }
            };

            this.mediaUnitLeader.dispatch(current, currentEndpoint, signaling, processCallback, completeCallback);
        }
    }

    /**
     * 处理 Busy 信令。
     *
     * @param signaling
     * @return
     */
    public MultipointCommStateCode processBusy(BusySignaling signaling) {
        CommField current = this.getCommField(signaling.getField().getId());
        if (null == current) {
            return MultipointCommStateCode.NoCommField;
        }

        if (current.isPrivate()) {
            Contact caller = (null == current.getCaller()) ? signaling.getCaller() : current.getCaller();
            Contact callee = (null == current.getCallee()) ? signaling.getCallee() : current.getCallee();
            Contact target = null;

            if (signaling.getContact().getId().longValue() == caller.getId().longValue()) {
                target = callee;
            }
            else {
                target = caller;
            }

            CommFieldEndpoint endpoint = current.getEndpoint(signaling.getContact(), signaling.getDevice());
            if (null != endpoint) {
                // 更新状态
                endpoint.setState(CommFieldEndpointState.Busy);
                endpoint.clearCandidates();

                // 停止追踪
                current.stopTrace(endpoint);

                current.clearCalling();
            }

            // 对方的通信场域
            if (null != target) {
                CommField targetField = this.getCommField(target.getId());
                if (null != targetField) {
                    CommFieldEndpoint targetEndpoint = targetField.getEndpoint(target);
                    if (null != targetEndpoint) {
                        targetEndpoint.setState(CommFieldEndpointState.Busy);

                        // 被叫忙
                        BusySignaling toTarget = new BusySignaling(targetField,
                                targetEndpoint.getContact(), targetEndpoint.getDevice());
                        toTarget.copy(signaling);
                        ModuleEvent event = new ModuleEvent(MultipointCommService.NAME, toTarget.getName(), toTarget.toJSON());
                        this.contactsAdapter.publish(target.getUniqueKey(), event.toJSON());
                    }
                }
            }
            else {
                Logger.w(MultipointCommService.class, "#processBusy - Can NOT find target peer : " + signaling.getContact().getId());
            }
        }
        else {
            // TODO
        }

        return MultipointCommStateCode.Ok;
    }

    /**
     *
     * @param signaling
     * @param callback
     */
    public void processInvite(InviteSignaling signaling, SignalingCallback callback) {
        CommField commField = this.getCommField(signaling.getField().getId());
        if (null == commField) {
            callback.on(MultipointCommStateCode.NoCommField, signaling);
            return;
        }

        List<Long> idList = signaling.getInvitees();
        if (null == idList) {
            callback.on(MultipointCommStateCode.InvalidParameter, signaling);
            return;
        }

        for (Long contactId : idList) {
            Contact target = ContactManager.getInstance().getContact(commField.getDomain().getName(), contactId);
            InviteSignaling inviteSignaling = new InviteSignaling(commField, signaling.getContact(), signaling.getDevice());
            // 设置被邀请人
            inviteSignaling.setInvitee(target);

            ModuleEvent event = new ModuleEvent(MultipointCommService.NAME, MultipointCommAction.Invite.name,
                    inviteSignaling.toJSON());
            this.contactsAdapter.publish(target.getUniqueKey(), event.toJSON());
        }

        callback.on(MultipointCommStateCode.Ok, signaling);
    }

    /**
     * 处理客户端的广播请求。
     *
     * @param endpoint
     * @param data
     * @return
     */
    public MultipointCommStateCode processBroadcast(CommFieldEndpoint endpoint, JSONObject data) {
        CommField commField = queryCommField(endpoint.getContact(), endpoint.getDevice());
        if (null == commField) {
            return MultipointCommStateCode.NoCommField;
        }

        List<CommFieldEndpoint> list = commField.getEndpoints();
        for (int i = 0; i < list.size(); ++i) {
            CommFieldEndpoint ep = list.get(i);
            if (ep.equals(endpoint)) {

                // 事件判断
                if (Broadcast.isMicrophoneVolume(data)) {
                    ep.microphoneVolume = MicrophoneVolume.getVolume(data);
                }
                else if (Broadcast.isAudioMuted(data)) {
                    ep.audioStreamEnabled = !AudioMuted.isMuted(data);
                }
                else if (Broadcast.isVideoMuted(data)) {
                    ep.videoStreamEnabled = !VideoMuted.isMuted(data);
                }

                // 跳过自己
                continue;
            }

            // 创建广播数据
            Broadcast broadcast = new Broadcast(commField, endpoint, ep, data);

            ModuleEvent event = new ModuleEvent(MultipointCommService.NAME,
                    MultipointCommAction.Broadcast.name, broadcast.toJSON());

            this.contactsAdapter.publish(ep.getContact().getUniqueKey(), event.toJSON());
        }

        return MultipointCommStateCode.Ok;
    }

    /**
     * 触发 Offer 超时。
     *
     * @param field
     * @param endpoint
     */
    protected void fireOfferTimeout(CommField field, CommFieldEndpoint endpoint) {
        if (Logger.isDebugLevel()) {
            Logger.i(this.getClass(), "Comm field offer timeout: " + field.getId());
        }

        if (field.isPrivate()) {
            CommField calleeField = this.commFieldMap.get(field.getCallee().getId());
            calleeField.clearAll();

            field.clearAll();
        }
        else {
            // TODO
        }
    }

    protected void pushSignaling(CommFieldEndpoint endpoint, Signaling signaling) {
        // 设置接收信令的目标
        signaling.setTarget(endpoint);

        ModuleEvent event = new ModuleEvent(MultipointCommService.NAME, signaling.getName(), signaling.toJSON());
        this.contactsAdapter.publish(endpoint.getContact().getUniqueKey(), event.toJSON());
    }

    private void broadcastArrivedEvent(CommField commField, CommFieldEndpoint endpoint) {
        for (CommFieldEndpoint target : commField.getEndpoints()) {
            if (target.equals(endpoint)) {
                continue;
            }

            CommFieldUpdate update = new CommFieldUpdate(commField, endpoint);
            ModuleEvent event = new ModuleEvent(MultipointCommService.NAME,
                    MultipointCommAction.Arrived.name,
                    update.toJSON());
            event.setContext((new TargetContext(target.getContact(), target.getDevice())).toJSON());
            this.contactsAdapter.publish(target.getContact().getUniqueKey(), event.toJSON());
        }
    }

    private void broadcastLeftEvent(CommField commField, CommFieldEndpoint endpoint) {
        for (CommFieldEndpoint target : commField.getEndpoints()) {
            if (target.equals(endpoint)) {
                continue;
            }

            CommFieldUpdate update = new CommFieldUpdate(commField, endpoint);
            ModuleEvent event = new ModuleEvent(MultipointCommService.NAME,
                    MultipointCommAction.Left.name,
                    update.toJSON());
            event.setContext((new TargetContext(target.getContact(), target.getDevice())).toJSON());
            this.contactsAdapter.publish(target.getContact().getUniqueKey(), event.toJSON());
        }
    }

    /**
     * 向指定上下文推送数据。
     *
     * @param talkContext
     * @param contact
     * @param device
     * @param packetName
     * @param data
     * @return
     */
    private boolean pushPacket(TalkContext talkContext, Contact contact, Device device,
                               String packetName, JSONObject data) {
        if (null == talkContext) {
            return false;
        }

        JSONObject payload = new JSONObject();
        try {
            payload.put("code", MultipointCommStateCode.Ok.code);
            payload.put("data", data);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Packet packet = new Packet(packetName, payload);
        ActionDialect dialect = Director.attachDirector(packet.toDialect(), contact, device);
        this.cellet.speak(talkContext, dialect);

        return true;
    }

    /**
     * 生成通讯场域终端 ID 。
     *
     * @param contact
     * @param device
     * @return
     */
    private long makeCommFieldEndpointId(Contact contact, Device device) {
        long id = 0;
        String string = contact.getUniqueKey() + "_" + device.getName() + "_" + device.getPlatform();
        for (int i = 0, len = string.length(); i < len; ++i) {
            int c = string.codePointAt(i);
            id += c * 3 + id * 3;
        }
        return Math.abs(Long.valueOf(id & 0x7fffffffffffffffL).intValue());
    }

    private CommField queryCommField(Contact contact, Device device) {
        CommField field = null;
        Iterator<CommField> iter = this.commFieldMap.values().iterator();
        while (iter.hasNext()) {
            CommField commField = iter.next();
            CommFieldEndpoint endpoint = commField.getEndpoint(contact, device);
            if (null != endpoint) {
                field = commField;
                break;
            }
        }
        return field;
    }

    @Override
    public void onDelivered(String topic, Endpoint endpoint, JSONObject jsonObject) {
        String moduleName = ModuleEvent.extractModuleName(jsonObject);
        if (MultipointCommService.NAME.equals(moduleName)) {
            // 解析事件
            ModuleEvent event = new ModuleEvent(jsonObject);
            // 事件名
            String eventName = event.getEventName();

            if (MultipointCommAction.Offer.name.equals(eventName)) {
                // 处理 Offer 信令
                OfferSignaling signaling = new OfferSignaling(event.getData());
                // 被叫
                Contact callee = signaling.getCallee();
                // 获取联系人
                Contact contact = ContactManager.getInstance()
                        .getOnlineContact(callee.getDomain().getName(), callee.getId());
                if (null != contact) {
                    // 向联系人所有设备推送
                    for (Device device : contact.getDeviceList()) {
                        if (pushPacket(device.getTalkContext(), contact, device,
                                signaling.getName(), signaling.toJSON())) {
                            Logger.i(this.getClass(), "Push signaling '" + signaling.getName()
                                    + "' to '" + callee.getId() + "'");
                        }
                    }
                }
            }
            else if (MultipointCommAction.Answer.name.equals(eventName)) {
                // 处理 Answer 信令
                AnswerSignaling signaling = new AnswerSignaling(event.getData());
                // 目标
                Contact targetContact = null;
                Device targetDevice = null;
                CommFieldEndpoint target = signaling.getTarget();
                if (null == target) {
                    targetContact = signaling.getContact();
                    targetDevice = signaling.getDevice();
                }
                else {
                    targetContact = target.getContact();
                    targetDevice = target.getDevice();
                }

                // 获取联系人
                Contact contact = ContactManager.getInstance()
                        .getOnlineContact(targetContact.getDomain().getName(), targetContact.getId());
                if (null != contact) {
                    Device device = contact.getDevice(targetDevice);
                    if (null != device) {
                        if (pushPacket(device.getTalkContext(), contact, device, signaling.getName(), signaling.toJSON())) {
                            Logger.i(this.getClass(), "Push signaling '" + signaling.getName()
                                    + "' to '" + targetContact.getId() + "'");
                        }
                    }
                }
            }
            else if (MultipointCommAction.Bye.name.equals(eventName)) {
                // 处理 Bye 信令
                ByeSignaling signaling = new ByeSignaling(event.getData());
                // 目标
                Contact targetContact = null;
                Device targetDevice = null;
                CommFieldEndpoint target = signaling.getTarget();
                if (null == target) {
                    targetContact = signaling.getContact();
                    targetDevice = signaling.getDevice();
                }
                else {
                    targetContact = target.getContact();
                    targetDevice = target.getDevice();
                }

                // 获取联系人
                Contact contact = ContactManager.getInstance()
                        .getOnlineContact(targetContact.getDomain().getName(), targetContact.getId());
                if (null != contact) {
                    Device device = contact.getDevice(targetDevice);
                    if (null != device) {
                        if (pushPacket(device.getTalkContext(), contact, device,
                                signaling.getName(), signaling.toJSON())) {
                            Logger.i(this.getClass(), "Push signaling '" + signaling.getName()
                                    + "' to '" + targetContact.getId() + "'");
                        }
                    }
                }
            }
            else if (MultipointCommAction.Busy.name.equals(eventName)) {
                // 处理 Bye 信令
                BusySignaling signaling = new BusySignaling(event.getData());
                Contact target = signaling.getContact();
                // 获取联系人
                Contact contact = ContactManager.getInstance()
                        .getOnlineContact(target.getDomain().getName(), target.getId());
                if (null != contact) {
                    Device device = contact.getDevice(signaling.getDevice());
                    if (null != device) {
                        if (pushPacket(device.getTalkContext(), contact, device,
                                signaling.getName(), signaling.toJSON())) {
                            Logger.i(this.getClass(), "Push signaling '" + signaling.getName()
                                    + "' to '" + target.getId() + "'");
                        }
                    }
                }
            }
            else if (MultipointCommAction.Candidate.name.equals(eventName)) {
                // 处理 Candidate
                CandidateSignaling signaling = new CandidateSignaling(event.getData());
                // 目标
                Contact targetContact = null;
                Device targetDevice = null;
                CommFieldEndpoint target = signaling.getTarget();
                if (null == target) {
                    targetContact = signaling.getContact();
                    targetDevice = signaling.getDevice();
                }
                else {
                    targetContact = target.getContact();
                    targetDevice = target.getDevice();
                }

                Contact contact = ContactManager.getInstance()
                        .getOnlineContact(targetContact.getDomain().getName(), targetContact.getId());
                if (null != contact) {
                    // 发送给指定设备
                    Device device = contact.getDevice(targetDevice);
                    if (null != device && pushPacket(device.getTalkContext(), contact, device,
                            signaling.getName(), signaling.toJSON())) {
                        Logger.d(this.getClass(), "Push signaling '" + signaling.getName()
                                + "' to '" + contact.getId() + "' : " + signaling.numCandidates());
                    }
                    else {
                        Logger.e(this.getClass(), "Push signaling failed: " + signaling.getName()
                                + "' to '" + contact.getId() + "' : " + signaling.numCandidates());
                    }
                }
            }
            else if (MultipointCommAction.Broadcast.name.equals(eventName)) {
                // 收到广播
                Broadcast broadcast = new Broadcast(event.getData());

                CommFieldEndpoint target = broadcast.getTarget();

                Contact contact = ContactManager.getInstance().getOnlineContact(target.getDomain().getName(),
                        target.getContact().getId());
                if (null != contact) {
                    // 发送给指定设备
                    Device device = contact.getDevice(target.getDevice());
                    if (null != device) {
                        pushPacket(device.getTalkContext(), contact, device, MultipointCommAction.Broadcast.name,
                                broadcast.toJSON());
                    }
                }
            }
            else if (MultipointCommAction.Invite.name.equals(eventName)) {
                InviteSignaling signaling = new InviteSignaling(event.getData());
                Contact invitee = signaling.getInvitee();
                // 被邀人
                Contact contact = ContactManager.getInstance()
                        .getOnlineContact(invitee.getDomain().getName(), invitee.getId());
                if (null != contact) {
                    // 向联系人的所有设备推送
                    for (Device device : contact.getDeviceList()) {
                        if (pushPacket(device.getTalkContext(), contact, device,
                                signaling.getName(), signaling.toJSON())) {
                            Logger.d(this.getClass(), "Push signaling '" + signaling.getName()
                                    + "' to '" + contact.getId() + "'");
                        }
                    }
                }
            }
            else if (MultipointCommAction.Arrived.name.equals(eventName) ||
                    MultipointCommAction.Left.name.equals(eventName)) {
                JSONObject context = event.getContext();
                TargetContext target = new TargetContext(context);
                Contact contact = ContactManager.getInstance().getOnlineContact(
                        target.getContact().getDomain().getName(), target.getContact().getId());
                if (null != contact) {
                    Device device = contact.getDevice(target.getDevice());
                    if (null != device) {
                        pushPacket(device.getTalkContext(), contact, device,
                                eventName, event.getData());
                    }
                }
            }
        }
        else if (ContactManager.NAME.equals(moduleName)) {
            // 联系人模块的事件，处理联系人设备断开
            String eventName = ModuleEvent.extractEventName(jsonObject);
            if (ContactAction.Disconnect.name.equals(eventName)) {
                final ModuleEvent event = new ModuleEvent(jsonObject);
                // 异步执行 Bye
                this.executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        Contact contact = new Contact(event.getData());
                        Device device = contact.getDeviceList().get(0);
                        // 查找断开连接终端所在的通讯场
                        CommField commField = queryCommField(contact, device);
                        if (null == commField) {
                            // 没有找到通讯场
                            return;
                        }

                        ByeSignaling bye = SignalingTool.createByeSignaling(contact, device, commField);
                        processBye(bye, new SignalingCallback() {
                            @Override
                            public void on(MultipointCommStateCode stateCode, Signaling signaling) {
                                if (Logger.isDebugLevel()) {
                                    Logger.d(MultipointCommService.class,
                                            "Device disconnect, simulate bye : " + stateCode.code);
                                }
                            }
                        });
                    }
                });
            }
        }
    }

    @Override
    public void onDelivered(String topic, Endpoint endpoint, Primitive primitive) {
        // Nothing
    }

    @Override
    public void onDelivered(List<String> list, Endpoint endpoint, Primitive primitive) {
        // Nothing
    }

    @Override
    public void onDelivered(List<String> list, Endpoint endpoint, JSONObject jsonObject) {
        // Nothing
    }

    @Override
    public void onSubscribeFailed(String topic, Endpoint endpoint) {
        // Nothing
    }

    @Override
    public void onUnsubscribeFailed(String topic, Endpoint endpoint) {
        // Nothing
    }
}
