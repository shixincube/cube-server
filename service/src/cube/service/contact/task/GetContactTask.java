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

package cube.service.contact.task;

import cell.core.cellet.Cellet;
import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.core.talk.dialect.DialectFactory;
import cell.util.log.Logger;
import cube.benchmark.ResponseTime;
import cube.common.Packet;
import cube.common.entity.Contact;
import cube.common.state.ContactStateCode;
import cube.service.ServiceTask;
import cube.service.contact.ContactManager;
import org.json.JSONObject;

/**
 * 获取联系人信息任务。
 */
public class GetContactTask extends ServiceTask {

    public GetContactTask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
        super(cellet, talkContext, primitive, responseTime);
    }

    @Override
    public void run() {
        ActionDialect action = DialectFactory.getInstance().createActionDialect(this.primitive);
        Packet packet = new Packet(action);

        JSONObject data = packet.data;

        Long id = null;
        String domain = null;
        String code = null;
        try {
            if (data.has("id")) {
                id = data.getLong("id");
                domain = data.getString("domain");
            }
            else {
                code = data.getString("code");
            }
        } catch (Exception e) {
            Logger.w(this.getClass(), "#run", e);
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, ContactStateCode.InvalidParameter.code, data));
            markResponseTime();
            return;
        }

        Contact contact = null;
        if (null != id && null != domain) {
            contact = ContactManager.getInstance().getContact(domain, id);
        }
        else {
            contact = ContactManager.getInstance().getContact(code);
        }

        if (null == contact) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, ContactStateCode.Failure.code, data));
            markResponseTime();
            return;
        }

        this.cellet.speak(this.talkContext,
                this.makeResponse(action, packet, ContactStateCode.Ok.code, contact.toJSON()));
        markResponseTime();
    }
}
