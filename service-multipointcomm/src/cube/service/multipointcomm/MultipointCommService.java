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
import cell.util.json.JSONObject;
import cube.common.ModuleEvent;
import cube.common.action.MultipointCommAction;
import cube.common.entity.CommField;
import cube.common.entity.CommFieldEndpoint;
import cube.common.entity.Contact;
import cube.common.entity.Device;
import cube.common.state.MultipointCommStateCode;
import cube.core.AbstractModule;
import cube.core.Kernel;
import cube.core.Module;
import cube.service.contact.ContactManager;
import cube.service.multipointcomm.signaling.OfferSignaling;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

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
    }

    @Override
    public void stop() {
        if (null != this.contactsAdapter) {
            this.contactsAdapter.removeListener(this);
        }
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
        CommField current = this.commFieldMap.get(signaling.getField());
        if (null == current) {
            return MultipointCommStateCode.NoCommField;
        }

        // 添加主叫终端
        Long cfeID = this.makeCommFieldEndpointId(signaling.getContact(), signaling.getDevice());
        CommFieldEndpoint cfe = new CommFieldEndpoint(cfeID, signaling.getContact(), signaling.getDevice());
        current.addEndpoint(cfe);

        List<Contact> targets = current.getOutboundCallTargets();
        for (Contact target : targets) {
            ModuleEvent event = new ModuleEvent(MultipointCommService.NAME,
                    MultipointCommAction.Offer.name, signaling.toJSON());
            this.contactsAdapter.publish(target.getUniqueKey(), event.toJSON());
        }

        return MultipointCommStateCode.Failure;
    }

    private void call(CommField commField) {

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
            ModuleEvent event = new ModuleEvent(jsonObject);
            if (event.getEventName().equals(MultipointCommAction.Offer.name)) {
                
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
