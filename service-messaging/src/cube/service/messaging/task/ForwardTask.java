/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.messaging.task;

import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.core.talk.dialect.DialectFactory;
import cell.util.log.Logger;
import cube.benchmark.ResponseTime;
import cube.common.Packet;
import cube.common.entity.AbstractContact;
import cube.common.entity.Contact;
import cube.common.entity.Device;
import cube.common.entity.Group;
import cube.common.state.MessagingStateCode;
import cube.service.ServiceTask;
import cube.service.contact.ContactManager;
import cube.service.messaging.MessagingService;
import cube.service.messaging.MessagingServiceCellet;
import cube.service.messaging.PushResult;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 转发消息
 */
public class ForwardTask extends ServiceTask {

    public ForwardTask(MessagingServiceCellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
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
        AbstractContact target = null;
        try {
            contactId = data.getLong("contactId");
            messageId = data.getLong("messageId");
            if (data.has("contact")) {
                target = new Contact(data.getJSONObject("contact"));
            }
            else {
                target = new Group(data.getJSONObject("group"));
            }
        } catch (JSONException e) {
            Logger.w(this.getClass(), "#run", e);
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, MessagingStateCode.DataStructureError.code, data));
            markResponseTime();
            return;
        }

        MessagingService messagingService = (MessagingService) this.kernel.getModule(MessagingService.NAME);
        // 转发消息
        PushResult result = messagingService.forwardMessage(domain, contactId, messageId, device, target);
        if (null == result) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, MessagingStateCode.DataLost.code, data));
            markResponseTime();
            return;
        }

        // 应答
        this.cellet.speak(this.talkContext,
                this.makeResponse(action, packet, result.stateCode.code, result.message.toCompactJSON()));
        markResponseTime();
    }
}
