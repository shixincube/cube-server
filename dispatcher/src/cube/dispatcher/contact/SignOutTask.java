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
