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

import cell.adapter.CelletAdapter;
import cell.adapter.CelletAdapterFactory;
import cell.adapter.CelletAdapterListener;
import cell.core.net.Endpoint;
import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.util.json.JSONException;
import cell.util.json.JSONObject;
import cell.util.log.Logger;
import cube.common.ModuleEvent;
import cube.common.Packet;
import cube.common.action.MultipointCommAction;
import cube.common.entity.CommField;
import cube.common.entity.CommFieldEndpoint;
import cube.common.entity.Contact;
import cube.common.entity.Device;
import cube.common.state.MultipointCommStateCode;
import cube.core.AbstractModule;
import cube.core.Kernel;
import cube.core.Module;
import cube.service.Director;
import cube.service.contact.ContactManager;
import cube.service.multipointcomm.signaling.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
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

    private MultipointCommServiceCellet cellet;

    /**
     * 联系人事件适配器。
     */
    private CelletAdapter contactsAdapter;

    /**
     * 定时器线程池。
     */
    private ScheduledExecutorService scheduledExecutor;

    /**
     * Comm Field 映射。
     */
    private ConcurrentHashMap<Long, CommField> commFieldMap;

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

        this.scheduledExecutor = Executors.newScheduledThreadPool(16);

        // 读取 Media Unit 配置
        Properties properties = this.loadConfig();
        this.mediaUnitLeader.readConfig(properties);

        this.mediaUnitLeader.start(this.getKernel().getNucleus().getTalkService());
    }

    @Override
    public void stop() {
        if (null != this.contactsAdapter) {
            this.contactsAdapter.removeListener(this);
        }

        this.mediaUnitLeader.stop();

        this.scheduledExecutor.shutdown();
    }

    @Override
    public void onTick(Module module, Kernel kernel) {

    }

    private Properties loadConfig() {
        File file = new File("config/multipoint-comm.properties");
        if (!file.exists()) {
            file = new File("multipoint-comm.properties");
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
     * 申请终端进入场域。
     *
     * @param commField
     * @param contact
     * @param device
     * @return
     */
    public MultipointCommStateCode applyEnter(CommField commField, Contact contact, Device device) {
        CommField current = this.commFieldMap.get(commField.getId());
        if (null == current) {
            current = commField;
            this.commFieldMap.put(current.getId(), current);
        }

        if (current.isPrivate()) {
            Long endpointId = this.makeCommFieldEndpointId(contact, device);
            CommFieldEndpoint endpoint = new CommFieldEndpoint(endpointId, contact, device);
            current.addEndpoint(endpoint);
        }
        else {
            // TODO
        }

        return MultipointCommStateCode.Ok;
    }

    /**
     * 申请记录呼叫。
     *
     * @param commField
     * @param proposer
     * @param target
     * @return
     */
    public MultipointCommStateCode applyCall(CommField commField, Contact proposer, Contact target) {
        CommField current = this.commFieldMap.get(commField.getId());
        if (null == current) {
            current = commField;
            this.commFieldMap.put(current.getId(), current);
        }

        if (current.isPrivate()) {
            // 私域
            if (current.isCalling(proposer)) {
                // 主叫忙
                return MultipointCommStateCode.CallerBusy;
            }

            // 为被叫准备 CommField
            CommField calleeField = this.commFieldMap.get(target.getId());
            if (null == calleeField) {
                calleeField = new CommField(target.getId(), current.getDomain().getName(), target);
                this.commFieldMap.put(calleeField.getId(), calleeField);
            }

            if (calleeField.isCalling(target)) {
                if (calleeField.isCaller(proposer) && calleeField.getCallerState() == MultipointCommStateCode.CallBye) {
                    // 主叫状态为 Bye 说明通话结束
                    calleeField.clearCalling();
                }
                else {
                    // 被叫忙
                    return MultipointCommStateCode.CalleeBusy;
                }
            }

            // 标记 Call
            current.markSingleCalling(proposer, target);

            // 标记 Call
            calleeField.markSingleCalling(proposer, target);

            return MultipointCommStateCode.Ok;
        }
        else {
            // TODO
        }

        return MultipointCommStateCode.Ok;
    }

    /**
     * 处理 Offer 信令。
     *
     * @param signaling
     * @param callback
     */
    public void processOffer(OfferSignaling signaling, SignalingCallback callback) {
        CommField current = this.commFieldMap.get(signaling.getField().getId());
        if (null == current) {
            callback.on(MultipointCommStateCode.NoCommField, signaling);
            return;
        }

        // 更新终端
        CommFieldEndpoint endpoint = current.getEndpoint(signaling.getContact(), signaling.getDevice());
        if (null == endpoint) {
            Long endpointId = this.makeCommFieldEndpointId(signaling.getContact(), signaling.getDevice());
            endpoint = new CommFieldEndpoint(endpointId, signaling.getContact(), signaling.getDevice());
            current.addEndpoint(endpoint);
        }
        endpoint.setSessionDescription(signaling.getSessionDescription());
        endpoint.setMediaConstraint(signaling.getMediaConstraint());
        endpoint.setState(MultipointCommStateCode.Calling);

        // 更新信令的 Field
        signaling.setField(current);

        if (current.isPrivate()) {
            if (!current.isCalling(signaling.getContact())) {
                endpoint.setState(MultipointCommStateCode.Ok);
                callback.on(MultipointCommStateCode.CommFieldStateError, signaling);
                return;
            }

            Logger.i(this.getClass(), "Offer: " + current.getCaller().getId() + " -> " + current.getCallee().getId());

            // 主叫
            signaling.setCaller(current.getCaller());

            // 被叫
            signaling.setCallee(current.getCallee());

            // 被叫的域
            CommField calleeField = this.commFieldMap.get(current.getCallee().getId());

            // 推送信令
            OfferSignaling toCallee = new OfferSignaling(calleeField, calleeField.getFounder(), Device.createDevice());
            toCallee.copy(signaling);
            ModuleEvent event = new ModuleEvent(MultipointCommService.NAME,
                    MultipointCommAction.Offer.name, toCallee.toJSON());
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
            Logger.i(this.getClass(), "Offer: " + current.getId() + " - " + signaling.getContact().getId());

            SignalingCallback processCallback = new SignalingCallback() {
                @Override
                public void on(MultipointCommStateCode stateCode, Signaling responseSignaling) {
                    if (null == responseSignaling) {
                        callback.on(MultipointCommStateCode.MediaUnitField, signaling);
                        return;
                    }

                    // 回调
                    callback.on(stateCode, responseSignaling);

                    // 向场域里的其他终端发送事件
                    broadcastEnteredEvent(current, current.getEndpoint(signaling.getContact(), signaling.getDevice()));
                }
            };

            // 由 Leader 派发信令
            this.mediaUnitLeader.dispatch(current, signaling, processCallback);
        }
    }

    /**
     * 处理 Answer 信令。
     *
     * @param signaling
     * @param callback
     */
    public void processAnswer(AnswerSignaling signaling, SignalingCallback callback) {
        CommField current = this.commFieldMap.get(signaling.getField().getId());
        if (null == current) {
            callback.on(MultipointCommStateCode.NoCommField, signaling);
            return;
        }

        // 更新终端
        CommFieldEndpoint endpoint = current.getEndpoint(signaling.getContact(), signaling.getDevice());
        if (null == endpoint) {
            Long endpointId = this.makeCommFieldEndpointId(signaling.getContact(), signaling.getDevice());
            endpoint = new CommFieldEndpoint(endpointId, signaling.getContact(), signaling.getDevice());
            current.addEndpoint(endpoint);
        }
        endpoint.setSessionDescription(signaling.getSessionDescription());
        endpoint.setMediaConstraint(signaling.getMediaConstraint());
        endpoint.setState(MultipointCommStateCode.CallConnected);

        // 更新信令的 Field
        signaling.setField(current);

        if (current.isPrivate()) {
            if (!current.isCalling(signaling.getContact())) {
                // 没有呼叫信息
                endpoint.setState(MultipointCommStateCode.Ok);
                callback.on(MultipointCommStateCode.CommFieldStateError, signaling);
                return;
            }

            Logger.i(this.getClass(), "Answer: " + current.getCallee().getId() + " -> " + current.getCaller().getId());

            current.updateCallerState(MultipointCommStateCode.CallConnected);
            current.updateCalleeState(MultipointCommStateCode.CallConnected);

            Contact caller = current.getCaller();
            // 设置主叫
            signaling.setCaller(caller);
            // 设置被叫
            signaling.setCallee(signaling.getContact());

            // 获取主叫的 Comm Field
            CommField callerField = this.commFieldMap.get(caller.getId());
            callerField.updateCallerState(MultipointCommStateCode.CallConnected);
            callerField.updateCalleeState(MultipointCommStateCode.CallConnected);

            CommFieldEndpoint callerEndpoint = callerField.getEndpoint(caller);
            // 修改主叫状态
            callerEndpoint.setState(MultipointCommStateCode.CallConnected);

            // 向主叫发送 Answer
            AnswerSignaling toCaller = new AnswerSignaling(callerField,
                    callerEndpoint.getContact(), callerEndpoint.getDevice());
            toCaller.copy(signaling);
            ModuleEvent event = new ModuleEvent(MultipointCommService.NAME,
                    MultipointCommAction.Answer.name, toCaller.toJSON());
            this.contactsAdapter.publish(caller.getUniqueKey(), event.toJSON());

            // 向主叫推送 Candidate
            CandidateSignaling candidateSignaling = new CandidateSignaling(callerField,
                    callerEndpoint.getContact(), callerEndpoint.getDevice());
            // 填写被叫的 Candidates
            candidateSignaling.setCandidateList(endpoint.getCandidates());
            event = new ModuleEvent(MultipointCommService.NAME, MultipointCommAction.Candidate.name,
                    candidateSignaling.toJSON());
            this.contactsAdapter.publish(callerEndpoint.getContact().getUniqueKey(), event.toJSON());

            // 向被叫推送 Candidate
            candidateSignaling = new CandidateSignaling(current,
                    endpoint.getContact(), endpoint.getDevice());
            // 填写主叫的 Candidates
            candidateSignaling.setCandidateList(callerEndpoint.getCandidates());
            event = new ModuleEvent(MultipointCommService.NAME, MultipointCommAction.Candidate.name,
                    candidateSignaling.toJSON());
            this.contactsAdapter.publish(endpoint.getContact().getUniqueKey(), event.toJSON());

            callback.on(MultipointCommStateCode.Ok, signaling);
        }
        else {
            this.mediaUnitLeader.dispatch(current, signaling, callback);
        }
    }

    /**
     * 处理 Candidate 信令。
     *
     * @param signaling
     * @return
     */
    public MultipointCommStateCode processCandidate(CandidateSignaling signaling) {
        CommField current = this.commFieldMap.get(signaling.getField().getId());
        if (null == current) {
            return MultipointCommStateCode.NoCommField;
        }

        if (current.isPrivate()) {
            CommFieldEndpoint endpoint = current.getEndpoint(signaling.getContact(), signaling.getDevice());
            if (null == endpoint) {
                return MultipointCommStateCode.NoCommFieldEndpoint;
            }

            if (endpoint.getState() == MultipointCommStateCode.CallConnected) {
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

        return MultipointCommStateCode.Ok;
    }

    /**
     * 处理 Bye 信令。
     *
     * @param signaling
     * @return
     */
    public MultipointCommStateCode processBye(ByeSignaling signaling) {
        CommField current = this.commFieldMap.get(signaling.getField().getId());
        if (null == current) {
            return MultipointCommStateCode.NoCommField;
        }

        if (current.isPrivate()) {
            CommFieldEndpoint endpoint = current.getEndpoint(signaling.getContact(), signaling.getDevice());
            if (null != endpoint) {
                if (endpoint.getState() == MultipointCommStateCode.CallBye) {
                    return MultipointCommStateCode.Ok;
                }

                Logger.i(this.getClass(), "Bye: " + current.getFounder().getId());

                // 更新状态
                endpoint.setState(MultipointCommStateCode.CallBye);
                endpoint.clearCandidates();

                current.updateCallerState(MultipointCommStateCode.CallBye);
                current.updateCalleeState(MultipointCommStateCode.CallBye);

                // 停止追踪
                current.stopTrace(endpoint);

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
                CommField targetField = this.commFieldMap.get(target.getId());
                // 更新状态
                targetField.updateCallerState(MultipointCommStateCode.CallBye);
                targetField.updateCalleeState(MultipointCommStateCode.CallBye);

                CommFieldEndpoint targetEndpoint = targetField.getEndpoint(target);
                if (null != targetEndpoint) {
                    targetEndpoint.setState(MultipointCommStateCode.CallBye);

                    ByeSignaling toTarget = new ByeSignaling(targetField,
                            targetEndpoint.getContact(), targetEndpoint.getDevice());
                    toTarget.copy(signaling);
                    ModuleEvent event = new ModuleEvent(MultipointCommService.NAME, toTarget.getName(), toTarget.toJSON());
                    this.contactsAdapter.publish(target.getUniqueKey(), event.toJSON());
                }

                // 清空
                current.clearAll();
            }
        }
        else {
            // TODO
        }

        return MultipointCommStateCode.Ok;
    }

    /**
     * 处理 Busy 信令。
     *
     * @param signaling
     * @return
     */
    public MultipointCommStateCode processBusy(BusySignaling signaling) {
        CommField current = this.commFieldMap.get(signaling.getField().getId());
        if (null == current) {
            return MultipointCommStateCode.NoCommField;
        }

        if (current.isPrivate()) {
            Contact caller = current.getCaller();
            Contact callee = current.getCallee();
            Contact target = null;

            if (signaling.getContact().equals(caller)) {
                target = callee;
            }
            else {
                target = caller;
            }

            signaling.setCaller(current.getCaller());
            signaling.setCallee(current.getCallee());

            CommFieldEndpoint endpoint = current.getEndpoint(signaling.getContact(), signaling.getDevice());
            if (null != endpoint) {
                // 更新状态
                endpoint.setState(MultipointCommStateCode.CalleeBusy);
                endpoint.clearCandidates();

                // 停止追踪
                current.stopTrace(endpoint);

                current.updateCalleeState(MultipointCommStateCode.CalleeBusy);

                current.clearCalling();
            }

            // 对方的通信场域
            CommField targetField = this.commFieldMap.get(target.getId());
            // 更新状态
            targetField.updateCalleeState(MultipointCommStateCode.CalleeBusy);

            CommFieldEndpoint targetEndpoint = targetField.getEndpoint(target);
            if (null != targetEndpoint) {
                BusySignaling toTarget = new BusySignaling(targetField,
                        targetEndpoint.getContact(), targetEndpoint.getDevice());
                toTarget.copy(signaling);
                ModuleEvent event = new ModuleEvent(MultipointCommService.NAME, toTarget.getName(), toTarget.toJSON());
                this.contactsAdapter.publish(target.getUniqueKey(), event.toJSON());
            }
        }
        else {
            // TODO
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

    private void broadcastEnteredEvent(CommField commField, CommFieldEndpoint endpoint) {
        for (CommFieldEndpoint target : commField.getEndpoints()) {
            CommFieldUpdate update = new CommFieldUpdate(commField, endpoint);
            ModuleEvent event = new ModuleEvent(MultipointCommService.NAME,
                    MultipointCommAction.Entered.name,
                    update.toCompactJSON());
            this.contactsAdapter.publish(target.getContact().getUniqueKey(), event.toJSON());
        }
    }

    private void broadcastLeftEvent(CommField commField, CommFieldEndpoint endpoint) {

    }

    /**
     * 向指定上下文推送信令数据。
     *
     * @param talkContext
     * @param contactId
     * @param domainName
     * @param signaling
     * @return
     */
    private boolean pushSignaling(TalkContext talkContext, Long contactId, String domainName, Signaling signaling) {
        if (null == talkContext) {
            return false;
        }

        JSONObject payload = new JSONObject();
        try {
            payload.put("code", MultipointCommStateCode.Ok.code);
            payload.put("data", signaling.toJSON());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Packet packet = new Packet(signaling.getName(), payload);
        ActionDialect dialect = Director.attachDirector(packet.toDialect(),
                contactId.longValue(), domainName);
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
        return Math.abs(id);
    }

    @Override
    public void onDelivered(String topic, Endpoint endpoint, JSONObject jsonObject) {
        if (MultipointCommService.NAME.equals(ModuleEvent.extractModuleName(jsonObject))) {
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
                        if (pushSignaling(device.getTalkContext(), contact.getId(),
                                contact.getDomain().getName(), signaling)) {
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
                Contact target = signaling.getContact();
                // 获取联系人
                Contact contact = ContactManager.getInstance()
                        .getOnlineContact(target.getDomain().getName(), target.getId());
                if (null != contact) {
                    Device device = contact.getDevice(signaling.getDevice());
                    if (null != device) {
                        if (pushSignaling(device.getTalkContext(), contact.getId(),
                                contact.getDomain().getName(), signaling)) {
                            Logger.i(this.getClass(), "Push signaling '" + signaling.getName()
                                    + "' to '" + target.getId() + "'");
                        }
                    }
                }
            }
            else if (MultipointCommAction.Bye.name.equals(eventName)) {
                // 处理 Bye 信令
                ByeSignaling signaling = new ByeSignaling(event.getData());
                Contact target = signaling.getContact();
                // 获取联系人
                Contact contact = ContactManager.getInstance()
                        .getOnlineContact(target.getDomain().getName(), target.getId());
                if (null != contact) {
                    Device device = contact.getDevice(signaling.getDevice());
                    if (null != device) {
                        if (pushSignaling(device.getTalkContext(), contact.getId(),
                                contact.getDomain().getName(), signaling)) {
                            Logger.i(this.getClass(), "Push signaling '" + signaling.getName()
                                    + "' to '" + target.getId() + "'");
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
                        if (pushSignaling(device.getTalkContext(), contact.getId(),
                                contact.getDomain().getName(), signaling)) {
                            Logger.i(this.getClass(), "Push signaling '" + signaling.getName()
                                    + "' to '" + target.getId() + "'");
                        }
                    }
                }
            }
            else if (MultipointCommAction.Candidate.name.equals(eventName)) {
                // 处理 Candidate
                CandidateSignaling signaling = new CandidateSignaling(event.getData());
                Contact contact = ContactManager.getInstance()
                        .getOnlineContact(signaling.getContact().getDomain().getName(), signaling.getContact().getId());
                if (null != contact) {
                    // 发送给指定设备
                    Device device = contact.getDevice(signaling.getDevice());
                    if (null != device && pushSignaling(device.getTalkContext(), contact.getId(),
                            contact.getDomain().getName(), signaling)) {
                        Logger.d(this.getClass(), "Push signaling '" + signaling.getName()
                                + "' to '" + contact.getId() + "'");
                    }
                }
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
