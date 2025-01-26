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
import cube.common.entity.Message;
import cube.common.state.MessagingStateCode;
import cube.service.ServiceTask;
import cube.service.contact.ContactManager;
import cube.service.messaging.MessagingService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 标记消息已读任务。
 */
public class GetConversationsTask extends ServiceTask {

    public GetConversationsTask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
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

        int limit = 0;
        try {
            limit = data.getInt("limit");
        } catch (JSONException e) {
            Logger.w(this.getClass(), "#run", e);
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, MessagingStateCode.DataStructureError.code, data));
            markResponseTime();
            return;
        }

        MessagingService messagingService = (MessagingService) this.kernel.getModule(MessagingService.NAME);

        JSONObject response = new JSONObject();
        JSONArray array = new JSONArray();

        // 获取最近清单
        List<Conversation> conversationList = messagingService.getRecentConversations(contact);
        if (null != conversationList) {
            // 返回指定数量的结果
            int num = Math.min(conversationList.size(), limit);

            for (int i = 0; i < num; ++i) {
                Conversation conversation = conversationList.get(i);
                array.put(conversation.toJSON());
            }
        }

        response.put("total", (null != conversationList) ? conversationList.size() : array.length());
        response.put("list", array);

        this.cellet.speak(this.talkContext,
                this.makeResponse(action, packet, MessagingStateCode.Ok.code, response));
        markResponseTime();
    }
}
