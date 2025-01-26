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
import cube.common.entity.Conversation;
import cube.common.state.MessagingStateCode;
import cube.service.ServiceTask;
import cube.service.contact.ContactManager;
import cube.service.messaging.MessagingService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * 标记消息已读任务。
 */
public class UpdateConversationTask extends ServiceTask {

    public UpdateConversationTask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
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

        Conversation conversation = null;
        try {
            conversation = new Conversation(data, contact);
        } catch (Exception e) {
            Logger.w(this.getClass(), "#run", e);
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, MessagingStateCode.DataStructureError.code, data));
            markResponseTime();
            return;
        }

        MessagingService messagingService = (MessagingService) this.kernel.getModule(MessagingService.NAME);
        // 更新会话
        messagingService.updateConversation(conversation);

        this.cellet.speak(this.talkContext,
                this.makeResponse(action, packet, MessagingStateCode.Ok.code, conversation.toJSON()));
        markResponseTime();
    }
}
