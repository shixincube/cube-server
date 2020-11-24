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

import java.util.List;
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
    }

    @Override
    public void stop() {
        if (null != this.contactsAdapter) {
            this.contactsAdapter.removeListener(this);
        }

        this.scheduledExecutor.shutdown();
    }

    @Override
    public void onTick(Module module, Kernel kernel) {

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
            if (current.isCalling()) {
                // 主叫忙
                return MultipointCommStateCode.CallerBusy;
            }

            // 为被叫准备 CommField
            CommField calleeField = this.commFieldMap.get(target.getId());
            if (null == calleeField) {
                calleeField = new CommField(target.getId(), current.getDomain().getName(), target);
                this.commFieldMap.put(calleeField.getId(), calleeField);
            }

            if (calleeField.isCalling()) {
                if (calleeField.getCallerState() == MultipointCommStateCode.CallBye) {
                    // 主叫状态为 Bye 说明通话结束
                    calleeField.clearCalling();
                }
                else {
                    // 被叫忙
                    return MultipointCommStateCode.CalleeBusy;
                }
            }

            // 标记 Call
            current.markCalling(proposer, target);

            // 标记 Call
            calleeField.markCalling(proposer, target);

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
     * @return
     */
    public MultipointCommStateCode processOffer(OfferSignaling signaling) {
        CommField current = this.commFieldMap.get(signaling.getField().getId());
        if (null == current) {
            return MultipointCommStateCode.NoCommField;
        }

        // 更新终端
        CommFieldEndpoint endpoint = current.getEndpoint(signaling.getContact(), signaling.getDevice());
        if (null == endpoint) {
            Long endpointId = this.makeCommFieldEndpointId(signaling.getContact(), signaling.getDevice());
            endpoint = new CommFieldEndpoint(endpointId, signaling.getContact(), signaling.getDevice());
            current.addEndpoint(endpoint);
        }
        endpoint.setSessionDescription(signaling.getSessionDescription());
        endpoint.setState(MultipointCommStateCode.Calling);

        // 更新信令的 Field
        signaling.setField(current);

        if (current.isPrivate()) {
            if (!current.isCalling()) {
                endpoint.setState(MultipointCommStateCode.Ok);
                return MultipointCommStateCode.CommFieldStateError;
            }

            Logger.i(this.getClass(), "Offer: " + current.getCaller().getId() + " -> " + current.getCallee().getId());

            // 主叫
            signaling.setCaller(current.getCaller());

            // 被叫
            signaling.setCallee(current.getCallee());

            // 推送信令
            ModuleEvent event = new ModuleEvent(MultipointCommService.NAME,
                    MultipointCommAction.Offer.name, signaling.toJSON());
            this.contactsAdapter.publish(current.getCallee().getUniqueKey(), event.toJSON());

            final CommFieldEndpoint offerEndpoint = endpoint;
            // 追踪
            current.traceOffer(this.scheduledExecutor, endpoint, new Runnable() {
                @Override
                public void run() {
                    fireOfferTimeout(current, offerEndpoint);
                }
            });
        }
        else {
            this.mediaUnitLeader.dispatch(current, signaling);
        }

        return MultipointCommStateCode.Ok;
    }

    /**
     * 处理 Answer 信令。
     *
     * @param signaling
     * @return
     */
    public MultipointCommStateCode processAnswer(AnswerSignaling signaling) {
        CommField current = this.commFieldMap.get(signaling.getField().getId());
        if (null == current) {
            return MultipointCommStateCode.NoCommField;
        }

        // 更新终端
        CommFieldEndpoint endpoint = current.getEndpoint(signaling.getContact(), signaling.getDevice());
        if (null == endpoint) {
            Long endpointId = this.makeCommFieldEndpointId(signaling.getContact(), signaling.getDevice());
            endpoint = new CommFieldEndpoint(endpointId, signaling.getContact(), signaling.getDevice());
            current.addEndpoint(endpoint);
        }
        endpoint.setSessionDescription(signaling.getSessionDescription());
        // 更新状态
        endpoint.setState(MultipointCommStateCode.CallConnected);

        // 更新信令的 Field
        signaling.setField(current);

        if (current.isPrivate()) {
            if (!current.isCalling()) {
                // 没有呼叫信息
                endpoint.setState(MultipointCommStateCode.Ok);
                return MultipointCommStateCode.CommFieldStateError;
            }

            Logger.i(this.getClass(), "Answer: " + current.getCallee().getId() + " -> " + current.getCaller().getId());

            current.updateCallerState(MultipointCommStateCode.CallConnected);
            current.updateCalleeState(MultipointCommStateCode.CallConnected);

            Contact caller = current.getCaller();
            // 设置主叫
            signaling.setCaller(caller);
            // 设置被叫
            signaling.setCallee(signaling.getContact());

            // 向主叫发送 Answer
            ModuleEvent event = new ModuleEvent(MultipointCommService.NAME,
                    MultipointCommAction.Answer.name, signaling.toJSON());
            this.contactsAdapter.publish(caller.getUniqueKey(), event.toJSON());

            // 获取主叫的 Comm Field
            CommField callerField = this.commFieldMap.get(caller.getId());
            callerField.updateCallerState(MultipointCommStateCode.CallConnected);
            callerField.updateCalleeState(MultipointCommStateCode.CallConnected);

            CommFieldEndpoint callerEndpoint = callerField.getEndpoint(caller);

            // 修改主叫状态
            callerEndpoint.setState(MultipointCommStateCode.CallConnected);

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
        }
        else {
            this.mediaUnitLeader.dispatch(current, signaling);
        }

        return MultipointCommStateCode.Ok;
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

                Logger.i(this.getClass(), "Bye: " + current.getFounder().getId());

                // 更新状态
                endpoint.setState(MultipointCommStateCode.CallBye);
                endpoint.clearCandidates();

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

                ModuleEvent event = new ModuleEvent(MultipointCommService.NAME, signaling.getName(), signaling.toJSON());
                this.contactsAdapter.publish(target.getUniqueKey(), event.toJSON());

                // 清空
                current.clearAll();

                // 对方的通信场域
                CommField peerField = this.commFieldMap.get(target.getId());
                // 更新状态
                peerField.updateCallerState(MultipointCommStateCode.CallBye);
                peerField.updateCalleeState(MultipointCommStateCode.CallBye);
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
            CommFieldEndpoint endpoint = current.getEndpoint(signaling.getContact(), signaling.getDevice());
            if (null != endpoint) {
                // 更新状态
                endpoint.setState(MultipointCommStateCode.CalleeBusy);
                endpoint.clearCandidates();

                // 停止追踪
                current.stopTrace(endpoint);

                Contact caller = current.getCaller();
                Contact callee = current.getCallee();
                Contact target = null;

                if (signaling.getContact().equals(caller)) {
                    // 向被叫推送 Busy
                    target = callee;
                }
                else {
                    // 向主叫推送 Busy
                    target = caller;
                }

                signaling.setCaller(caller);
                signaling.setCallee(callee);

                ModuleEvent event = new ModuleEvent(MultipointCommService.NAME, signaling.getName(), signaling.toJSON());
                this.contactsAdapter.publish(target.getUniqueKey(), event.toJSON());
            }
        }
        else {
            // TODO
        }

        return MultipointCommStateCode.Ok;
    }

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
                            Logger.d(this.getClass(), "Push signaling '" + signaling.getName()
                                    + "' to '" + callee.getId() + "'");
                        }
                    }
                }
            }
            else if (MultipointCommAction.Answer.name.equals(eventName)) {
                // 处理 Answer 信令
                AnswerSignaling signaling = new AnswerSignaling(event.getData());
                // 主叫
                Contact caller = signaling.getCaller();
                // 获取联系人
                Contact contact = ContactManager.getInstance()
                        .getOnlineContact(caller.getDomain().getName(), caller.getId());
                if (null != contact) {
                    // 向联系人所有设备推送
                    for (Device device : contact.getDeviceList()) {
                        if (pushSignaling(device.getTalkContext(), contact.getId(),
                                contact.getDomain().getName(), signaling)) {
                            Logger.d(this.getClass(), "Push signaling '" + signaling.getName()
                                    + "' to '" + caller.getId() + "'");
                        }
                    }
                }
            }
            else if (MultipointCommAction.Bye.name.equals(eventName)) {
                // 处理 Bye 信令
                ByeSignaling signaling = new ByeSignaling(event.getData());
                Contact source = signaling.getContact();
                Contact target = source.equals(signaling.getCaller()) ? signaling.getCallee() : signaling.getCaller();
                // 获取联系人
                Contact contact = ContactManager.getInstance()
                        .getOnlineContact(target.getDomain().getName(), target.getId());
                if (null != contact) {
                    // 向联系人所有设备推送
                    for (Device device : contact.getDeviceList()) {
                        if (pushSignaling(device.getTalkContext(), contact.getId(),
                                contact.getDomain().getName(), signaling)) {
                            Logger.d(this.getClass(), "Push signaling '" + signaling.getName()
                                    + "' to '" + target.getId() + "'");
                        }
                    }
                }
            }
            else if (MultipointCommAction.Busy.name.equals(eventName)) {
                // 处理 Bye 信令
                BusySignaling signaling = new BusySignaling(event.getData());
                Contact source = signaling.getContact();
                Contact target = source.equals(signaling.getCaller()) ? signaling.getCallee() : signaling.getCaller();
                // 获取联系人
                Contact contact = ContactManager.getInstance()
                        .getOnlineContact(target.getDomain().getName(), target.getId());
                if (null != contact) {
                    // 向联系人所有设备推送
                    for (Device device : contact.getDeviceList()) {
                        if (pushSignaling(device.getTalkContext(), contact.getId(),
                                contact.getDomain().getName(), signaling)) {
                            Logger.d(this.getClass(), "Push signaling '" + signaling.getName()
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