/**
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 Shixin Cube Team.
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
import cube.common.Packet;
import cube.common.entity.Contact;
import cube.common.entity.Message;
import cube.common.state.MessagingStateCode;
import cube.service.ServiceTask;
import cube.service.contact.ContactManager;
import cube.service.messaging.MessagingService;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 标记消息已读任务。
 */
public class ReadTask extends ServiceTask {

    public ReadTask(Cellet cellet, TalkContext talkContext, Primitive primitive) {
        super(cellet, talkContext, primitive);
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
            return;
        }

        // 域
        String domain = contact.getDomain().getName();

        Long contactId = null;
        Long messageId = null;
        try {
            contactId = data.getLong("contactId");
            messageId = data.getLong("messageId");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (!contact.getId().equals(contactId)) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, MessagingStateCode.DataStructureError.code, data));
            return;
        }

        MessagingService messagingService = (MessagingService) this.kernel.getModule(MessagingService.NAME);
        // 标记消息已读
        Message message = messagingService.markReadMessage(domain, contactId, messageId);
        if (null == message) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, MessagingStateCode.Failure.code, data));
            return;
        }

        this.cellet.speak(this.talkContext,
                this.makeResponse(action, packet, MessagingStateCode.Ok.code, message.toCompactJSON()));
    }
}
