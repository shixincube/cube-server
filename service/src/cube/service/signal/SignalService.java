/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 Cube Team.
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

package cube.service.signal;

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
import cube.common.action.SignalAction;
import cube.common.entity.Contact;
import cube.common.entity.Device;
import cube.common.entity.Signal;
import cube.common.state.SignalStateCode;
import cube.core.AbstractModule;
import cube.core.Kernel;
import cube.core.Module;
import cube.plugin.PluginSystem;
import cube.service.Director;
import cube.service.contact.ContactManager;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * 信号服务。
 */
public class SignalService extends AbstractModule implements CelletAdapterListener  {

    public final static String NAME = "Signal";

    /**
     * Cellet 实例。
     */
    private SignalCellet cellet;

    /**
     * 联系人事件队列。
     */
    private CelletAdapter contactsAdapter;

    public SignalService(SignalCellet cellet) {
        super();
        this.cellet = cellet;
    }

    @Override
    public void start() {
        this.contactsAdapter = CelletAdapterFactory.getInstance().getAdapter("Contacts");
        this.contactsAdapter.addListener(this);
    }

    @Override
    public void stop() {
    }

    @Override
    public PluginSystem getPluginSystem() {
        return null;
    }

    @Override
    public void onTick(Module module, Kernel kernel) {

    }

    public boolean emitSignalDirectly(Signal signal) {
        Contact contact = signal.getDestContact();
        if (null == contact) {
            return false;
        }

        ModuleEvent event = new ModuleEvent(SignalService.NAME, SignalAction.Direct.name, signal.toJSON());
        this.contactsAdapter.publish(contact.getUniqueKey(), event.toJSON());

        return true;
    }

    public void emitSignalBroadcast(Signal signal, List<Contact> destination) {
        for (Contact contact : destination) {
            // 设置目标
            signal.setDestContact(contact);
            ModuleEvent event = new ModuleEvent(SignalService.NAME, SignalAction.Broadcast.name, signal.toJSON());
            this.contactsAdapter.publish(contact.getUniqueKey(), event.toJSON());
        }
    }

    private boolean emit(TalkContext talkContext, Contact contact, Signal signal) {
        if (null == talkContext) {
            return false;
        }

        JSONObject payload = new JSONObject();
        try {
            payload.put("code", SignalStateCode.Ok.code);
            payload.put("data", signal.toJSON());
        } catch (JSONException e) {
            Logger.e(this.getClass(), "emit", e);
        }

        Packet packet = new Packet(SignalAction.Receipt.name, payload);
        ActionDialect dialect = Director.attachDirector(packet.toDialect(),
                contact.getId(), contact.getDomain().getName());
        this.cellet.speak(talkContext, dialect);

        return true;
    }

    @Override
    public void onDelivered(String topic, Endpoint endpoint, JSONObject jsonObject) {
        if (NAME.equals(ModuleEvent.extractModuleName(jsonObject))) {

            // 实例化事件
            ModuleEvent event = new ModuleEvent(jsonObject);

            if (SignalAction.Direct.name.equals(event.getEventName()) ||
                    SignalAction.Broadcast.name.equals(event.getEventName())) {
                // 直接向目标投送
                Signal signal = new Signal(event.getData());
                Contact dest = signal.getDestContact();
                Contact contact = ContactManager.getInstance().getOnlineContact(dest.getDomain().getName(), dest.getId());
                if (null != contact) {
                    // 联系人在线
                    for (Device device : contact.getDeviceList()) {
                        TalkContext talkContext = device.getTalkContext();
                        // 发送数据到客户端
                        this.emit(talkContext, contact, signal);
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
    public void onSubscribeFailed(String s, Endpoint endpoint) {
        // Nothing
    }

    @Override
    public void onUnsubscribeFailed(String s, Endpoint endpoint) {
        // Nothing
    }
}
