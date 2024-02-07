/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 Ambrose Xu.
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
 *
 * 参数说明：
 *
 * 对单条消息进行标记。
 * <code>
 * contactId
 * messageId
 * </code>
 *
 * 对消息进行批量标记。
 * <code>
 * contactId
 * messageIdList
 * messageFrom - 该次操作的消息发件人 ID，以便于服务器通知发件人。
 * </code>
 *
 * 对消息进行批量标记。
 * <code>
 * contactId
 * messageIdList
 * messageSource - 该次操作的消息的群组。
 * </code>
 */
public class ReadTask extends ServiceTask {

    public ReadTask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
        super(cellet, talkContext, primitive, responseTime);
    }

    @Override
    public void run() {
        ActionDialect action = DialectFactory.getInstance().createActionDialect(this.primitive);
        Packet packet = new Packet(action);

        JSONObject data = packet.data;

        String tokenCode = this.getTokenCode(action);
        Contact contact = ContactManager.getInstance().getContact(tokenCode);
        // 设备
        Device device = ContactManager.getInstance().getDevice(tokenCode);
        if (null == contact || null == device) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, MessagingStateCode.NoContact.code, data));
            markResponseTime();
            return;
        }

        // 域
        String domain = contact.getDomain().getName();

        Long contactId = null;
        try {
            contactId = data.getLong("contactId");
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

        Long messageId = null;
        JSONArray messageIds = null;
        Long messageFrom = null;
        Long messageSource = null;

        if (data.has("messageId")) {
            messageId = data.getLong("messageId");
        }
        else if (data.has("messageIdList") && data.has("messageFrom")) {
            messageIds = data.getJSONArray("messageIdList");
            messageFrom = data.getLong("messageFrom");
        }
        else if (data.has("messageIdList") && data.has("messageSource")) {
            messageIds = data.getJSONArray("messageIdList");
            messageSource = data.getLong("messageSource");
        }
        else {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, MessagingStateCode.DataStructureError.code, data));
            markResponseTime();
            return;
        }

        MessagingService messagingService = (MessagingService) this.kernel.getModule(MessagingService.NAME);

        if (null != messageId) {
            // 标记消息已读
            Message message = messagingService.markReadMessage(domain, contactId, messageId, device);
            if (null == message) {
                this.cellet.speak(this.talkContext,
                        this.makeResponse(action, packet, MessagingStateCode.Failure.code, data));
                markResponseTime();
                return;
            }

            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, MessagingStateCode.Ok.code, message.toCompactJSON()));
            markResponseTime();
        }
        else if (null != messageFrom) {
            // 标记针对联系人的消息已读
            List<Long> messageIdList = new ArrayList<>(messageIds.length());
            for (int i = 0; i < messageIds.length(); ++i) {
                messageIdList.add(messageIds.getLong(i));
            }

            messagingService.markReadMessagesByContact(domain, contactId, messageFrom, messageIdList);

            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, MessagingStateCode.Ok.code, data));
            markResponseTime();
        }
        else {
            // 标记针对群组的消息已读
            List<Long> messageIdList = new ArrayList<>(messageIds.length());
            for (int i = 0; i < messageIds.length(); ++i) {
                messageIdList.add(messageIds.getLong(i));
            }

            messagingService.markReadMessagesByGroup(domain, contactId, messageSource, messageIdList);

            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, MessagingStateCode.Ok.code, data));
            markResponseTime();
        }
    }
}
