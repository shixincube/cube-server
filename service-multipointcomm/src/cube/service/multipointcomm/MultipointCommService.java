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
import cube.service.multipointcomm.signaling.AnswerSignaling;
import cube.service.multipointcomm.signaling.OfferSignaling;
import cube.service.multipointcomm.signaling.Signaling;

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

    private MediaUnitLeader leader;

    public MultipointCommService(MultipointCommServiceCellet cellet) {
        this.cellet = cellet;
        this.commFieldMap = new ConcurrentHashMap<Long, CommField>();
        this.leader = new MediaUnitLeader();
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

    public MultipointCommStateCode applyCall(CommField commField, Contact proposer, Contact target) {
        CommField current = this.commFieldMap.get(commField.getId());
        if (null == current) {
            current = commField;
            this.commFieldMap.put(current.getId(), current);
        }

        if (current.isPrivate()) {
            // 私域
            if (current.hasOutboundCall(proposer)) {
                // 主叫忙
                return MultipointCommStateCode.CallerBusy;
            }

            // 标记 Call
            current.markOutboundCall(proposer, target);
            return MultipointCommStateCode.Ok;
        }

        // 向 MU 请求 Offer

        return MultipointCommStateCode.Ok;
    }

    public MultipointCommStateCode processOffer(OfferSignaling signaling) {
        CommField current = this.commFieldMap.get(signaling.getField().getId());
        if (null == current) {
            return MultipointCommStateCode.NoCommField;
        }

        // 添加主叫终端
        Long endpointId = this.makeCommFieldEndpointId(signaling.getContact(), signaling.getDevice());
        CommFieldEndpoint endpoint = new CommFieldEndpoint(endpointId, signaling.getContact(), signaling.getDevice());
        endpoint.setSessionDescription(signaling.getSessionDescription());
        current.addEndpoint(endpoint);

        // 追踪
        current.traceOffer(this.scheduledExecutor, endpoint, new Runnable() {
            @Override
            public void run() {
                offerTimeout(current, endpoint);
            }
        });

        // 更新信令的 Field
        signaling.setField(current);

        List<Contact> targets = current.getOutboundCallTargets();
        for (Contact target : targets) {
            // 设置被叫
            signaling.setCallee(target);

            // 推送到集群
            ModuleEvent event = new ModuleEvent(MultipointCommService.NAME,
                    MultipointCommAction.Offer.name, signaling.toJSON());

            this.contactsAdapter.publish(target.getUniqueKey(), event.toJSON());
        }

        return MultipointCommStateCode.Ok;
    }

    public MultipointCommStateCode processAnswer(AnswerSignaling signaling) {
        CommField current = this.commFieldMap.get(signaling.getField().getId());
        if (null == current) {
            return MultipointCommStateCode.NoCommField;
        }

        // 添加被叫终端
        Long endpointId = this.makeCommFieldEndpointId(signaling.getContact(), signaling.getDevice());
        CommFieldEndpoint endpoint = new CommFieldEndpoint(endpointId, signaling.getContact(), signaling.getDevice());
        endpoint.setSessionDescription(signaling.getSessionDescription());
        current.addEndpoint(endpoint);

        // 更新信令的 Field
        signaling.setField(current);

        return MultipointCommStateCode.Ok;
    }

    protected void offerTimeout(CommField field, CommFieldEndpoint endpoint) {
        field.removeEndpoint(endpoint);

        Contact contact = ContactManager.getInstance().getOnlineContact(field.getDomain().getName(),
                endpoint.getContact().getId());
        if (null == contact) {
            return;
        }

        Device device = contact.getDevice(endpoint.getDevice());
        if (null == device) {
            return;
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

            if (event.getEventName().equals(MultipointCommAction.Offer.name)) {
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
                        if (pushSignaling(device.getTalkContext(),
                                contact.getId(), contact.getDomain().getName(),
                                signaling)) {
                            Logger.d(this.getClass(), "Push signaling '" + signaling.getName()
                                    + "' to '" + callee.getId() + "'");
                        }
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
