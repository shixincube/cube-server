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
import cube.dispatcher.DispatcherTask;
import cube.dispatcher.Performer;
import org.json.JSONObject;

/**
 * 联系人签出任务。
 */
public class SignOutTask extends DispatcherTask {

    public SignOutTask(ContactCellet cellet, TalkContext talkContext, Primitive primitive, Performer performer) {
        super(cellet, talkContext, primitive, performer);
    }

    @Override
    public void run() {
        String tokenCode = this.getTokenCode(this.getAction());
        if (null == tokenCode) {
            // 无令牌码
            ActionDialect response = this.makeResponse(new JSONObject(), StateCode.NoAuthToken, "No token code");
            this.cellet.speak(this.talkContext, response);
            ((ContactCellet)this.cellet).returnSignOutTask(this);
            return;
        }

        Packet packet = this.getRequest();

        JSONObject contactJson = packet.data;

        // 移除当前上下文关联的联系人及其设备
        Contact contact = new Contact(contactJson, this.talkContext);
        this.performer.removeContact(contact, contact.getDevice(this.talkContext));

        ActionDialect response = this.performer.syncTransmit(this.talkContext, this.cellet.getName(), this.getAction());
        if (null == response) {
            // 发生错误
            response = this.makeResponse(packet.data, StateCode.GatewayError, "Service is disabled");
        }
        else {
            response = this.makeResponse(response);
        }

        this.cellet.speak(this.talkContext, response);

        ((ContactCellet)this.cellet).returnSignOutTask(this);
    }
}
