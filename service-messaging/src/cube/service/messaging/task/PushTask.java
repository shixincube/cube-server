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
import cube.benchmark.ResponseTime;
import cube.common.Packet;
import cube.common.entity.Contact;
import cube.common.entity.Device;
import cube.common.entity.Message;
import cube.common.state.MessagingStateCode;
import cube.service.ServiceTask;
import cube.service.contact.ContactManager;
import cube.service.messaging.*;
import org.json.JSONException;

/**
 * 推送消息任务。
 */
public class PushTask extends ServiceTask {

    public PushTask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
        super(cellet, talkContext, primitive, responseTime);
    }

    @Override
    public void run() {
        ActionDialect action = DialectFactory.getInstance().createActionDialect(this.primitive);
        Packet packet = new Packet(action);

        // 创建消息实例
        Message message = null;
        try {
            message = new Message(packet);
        } catch (JSONException e) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, MessagingStateCode.DataStructureError.code, packet.data));
            markResponseTime();
            return;
        }

        if (null == message.getDomain()) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, MessagingStateCode.NoDomain.code, packet.data));
            markResponseTime();
            return;
        }

        String tokenCode = this.getTokenCode(action);
        Device device = ContactManager.getInstance().getDevice(tokenCode);
        if (null == device) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, MessagingStateCode.NoDevice.code, packet.data));
            markResponseTime();
            return;
        }

        // 校验发件人是否与令牌一致
        Contact contact = ContactManager.getInstance().getContact(tokenCode);
        if (null == contact) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, MessagingStateCode.NoContact.code, packet.data));
            markResponseTime();
            return;
        }

        if (contact.getId().longValue() != message.getFrom().longValue()) {
            // 联系人身份信息不一致
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, MessagingStateCode.InvalidParameter.code, packet.data));
            markResponseTime();
            return;
        }

        // 将消息推送到消息中心
        MessagingService messagingService = (MessagingService) this.kernel.getModule(MessagingService.NAME);
        PushResult result = messagingService.pushMessage(message, device);
        Message response = result.message;

        // 进行插件 Hook 处理
        if (result.stateCode == MessagingStateCode.Ok) {
            // Hook
            MessagingHook hook = ((MessagingPluginSystem) messagingService.getPluginSystem()).getSendMessageHook();
            MessagingPluginContext context = new MessagingPluginContext(response, device);
            hook.apply(context);
        }

        // 应答
        this.cellet.speak(this.talkContext
                , this.makeResponse(action, packet, result.stateCode.code, response.toCompactJSON()));
        markResponseTime();
    }
}
