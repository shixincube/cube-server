/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.messaging.task;

import cell.core.cellet.Cellet;
import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.core.talk.dialect.DialectFactory;
import cell.util.log.Logger;
import cube.benchmark.ResponseTime;
import cube.common.Packet;
import cube.common.entity.Contact;
import cube.common.entity.Device;
import cube.common.state.MessagingStateCode;
import cube.service.ServiceTask;
import cube.service.contact.ContactManager;
import cube.service.messaging.MessagingService;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 撤回消息任务。
 */
public class RetractTask extends ServiceTask {

    public RetractTask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
        super(cellet, talkContext, primitive, responseTime);
    }

    @Override
    public void run() {
        ActionDialect action = DialectFactory.getInstance().createActionDialect(this.primitive);
        Packet packet = new Packet(action);

        JSONObject data = packet.data;

        String tokenCode = this.getTokenCode(action);
        Contact contact = ContactManager.getInstance().getContact(tokenCode);
        if (null == contact) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, MessagingStateCode.NoContact.code, data));
            markResponseTime();
            return;
        }

        // 域
        String domain = contact.getDomain().getName();

        // 设备
        Device device = ContactManager.getInstance().getDevice(tokenCode);

        Long contactId = null;
        Long messageId = null;
        try {
            contactId = data.getLong("contactId");
            messageId = data.getLong("messageId");
        } catch (JSONException e) {
            Logger.w(this.getClass(), "#run", e);
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, MessagingStateCode.DataStructureError.code, data));
            markResponseTime();
            return;
        }

        if (!contact.getId().equals(contactId)) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, MessagingStateCode.DataStructureError.code, data));
            markResponseTime();
            return;
        }

        MessagingService messagingService = (MessagingService) this.kernel.getModule(MessagingService.NAME);
        // 撤回消息
        if (!messagingService.retractMessage(domain, contactId, messageId, device)) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, MessagingStateCode.Failure.code, data));
        }
        else {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, MessagingStateCode.Ok.code, data));
        }

        markResponseTime();
    }
}
