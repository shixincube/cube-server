/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.contact;

import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cube.common.Packet;
import cube.common.StateCode;
import cube.common.entity.Contact;
import cube.common.entity.Device;
import cube.dispatcher.DispatcherTask;
import cube.dispatcher.Performer;
import org.json.JSONObject;

/**
 * 客户端断线后重新连接。
 */
public class ComebackTask extends DispatcherTask {

    public ComebackTask(ContactCellet cellet, TalkContext talkContext, Primitive primitive, Performer performer) {
        super(cellet, talkContext, primitive, performer);
    }

    @Override
    public void run() {
        String tokenCode = this.getTokenCode(this.getAction());
        if (null == tokenCode) {
            // 无令牌码
            ActionDialect response = this.makeResponse(new JSONObject(), StateCode.NoAuthToken, "No token code");
            this.cellet.speak(this.talkContext, response);
            ((ContactCellet)this.cellet).returnComebackTask(this);
            return;
        }

        Packet packet = this.getRequest();

        // 将当前联系人的设备与会话上下问关联
        Contact contact = new Contact(packet.data, this.talkContext);
        Device device = contact.getDevice(this.talkContext);
        device.setToken(tokenCode);
        this.performer.updateContact(contact, device);

        ActionDialect response = this.performer.syncTransmit(this.talkContext, this.cellet.getName(), this.getAction());
        if (null == response) {
            // 发生错误
            response = this.makeResponse(packet.data, StateCode.GatewayError, "Service is disabled");
        }
        else {
            response = this.makeResponse(response);
        }

        this.cellet.speak(this.talkContext, response);

        ((ContactCellet)this.cellet).returnComebackTask(this);
    }
}
