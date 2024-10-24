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

package cube.service.conference.task;

import cell.core.cellet.Cellet;
import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.core.talk.dialect.DialectFactory;
import cube.benchmark.ResponseTime;
import cube.common.Packet;
import cube.common.entity.Conference;
import cube.common.entity.Contact;
import cube.common.entity.Invitation;
import cube.common.state.ContactStateCode;
import cube.service.ServiceTask;
import cube.service.conference.ConferenceService;
import cube.service.contact.ContactManager;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 创建会议任务。
 */
public class CreateConferenceTask extends ServiceTask {

    public CreateConferenceTask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
        super(cellet, talkContext, primitive, responseTime);
    }

    @Override
    public void run() {
        ActionDialect action = DialectFactory.getInstance().createActionDialect(this.primitive);
        Packet packet = new Packet(action);

        JSONObject data = packet.data;

        String tokenCode = this.getTokenCode(action);
        if (null == tokenCode) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, ContactStateCode.InvalidParameter.code, data));
            markResponseTime();
            return;
        }

        Contact contact = ContactManager.getInstance().getContact(tokenCode);
        if (null == contact) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, ContactStateCode.NoSignIn.code, data));
            markResponseTime();
            return;
        }

        String subject = null;
        String password = null;
        String summary = null;
        long scheduleTime = 0;
        long expireTime = 0;
        List<Invitation> invitations = new ArrayList<>();

        subject = data.getString("subject");
        if (data.has("password")) {
            password = data.getString("password");
        }
        if (data.has("summary")) {
            summary = data.getString("summary");
        }
        scheduleTime = data.getLong("scheduleTime");
        expireTime = data.getLong("expireTime");

        if (data.has("invitations")) {
            JSONArray array = data.getJSONArray("invitations");
            for (int i = 0; i < array.length(); ++i) {
                invitations.add(new Invitation(array.getJSONObject(i)));
            }
        }

        ConferenceService service = (ConferenceService) this.kernel.getModule(ConferenceService.NAME);
        Conference conference = service.createConference(contact, subject, password, summary, scheduleTime, expireTime, invitations);
        if (null == conference) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, ContactStateCode.Failure.code, data));
            markResponseTime();
            return;
        }

        this.cellet.speak(this.talkContext,
                this.makeResponse(action, packet, ContactStateCode.Ok.code, conference.toCompactJSON()));
        markResponseTime();
    }
}
