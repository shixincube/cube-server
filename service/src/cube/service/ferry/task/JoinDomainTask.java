/*
 * This source file is part of Cube.
 * <p>
 * The MIT License (MIT)
 * <p>
 * Copyright (c) 2020-2022 Cube Team.
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package cube.service.ferry.task;

import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.core.talk.dialect.DialectFactory;
import cube.benchmark.ResponseTime;
import cube.common.Packet;
import cube.common.entity.Contact;
import cube.ferry.DomainMember;
import cube.ferry.FerryStateCode;
import cube.ferry.JoinWay;
import cube.ferry.Role;
import cube.service.ServiceTask;
import cube.service.contact.ContactManager;
import cube.service.ferry.FerryCellet;
import cube.service.ferry.FerryService;
import org.json.JSONObject;

import java.util.List;

/**
 * 加入域任务。
 */
public class JoinDomainTask extends ServiceTask {

    public JoinDomainTask(FerryCellet cellet, TalkContext talkContext, Primitive primitive,
                          ResponseTime responseTime) {
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
                    this.makeResponse(action, packet, FerryStateCode.InvalidToken.code, data));
            markResponseTime();
            return;
        }

        Contact contact = ContactManager.getInstance().getContact(tokenCode);
        if (null == contact) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, FerryStateCode.InvalidToken.code, data));
            markResponseTime();
            return;
        }

        if (!data.has("domain") || !data.has("contactId")) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, FerryStateCode.InvalidParameter.code, data));
            markResponseTime();
            return;
        }

        FerryService service = ((FerryCellet) this.cellet).getFerryService();

        String domain = data.getString("domain");
        if (!service.isOnlineDomain(domain)) {
            // 域对应的服务器不在线
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, FerryStateCode.InvalidDomain.code, data));
            markResponseTime();
            return;
        }

        Long contactId = data.getLong("contactId");
        JoinWay joinWay = JoinWay.parse(data.getInt("way"));

        DomainMember member = null;
        List<DomainMember> list = service.listDomainMember(domain);
        if (list.isEmpty()) {
            // 首次绑定
            member = new DomainMember(domain, contactId, joinWay,
                    System.currentTimeMillis(), Role.Administrator);
        }
        else {
            // 加入
            member = new DomainMember(domain, contactId, joinWay,
                    System.currentTimeMillis(), Role.Member);
        }

        // 添加新成员
        service.addDomainMember(contact, member);

        this.cellet.speak(this.talkContext,
                this.makeResponse(action, packet, FerryStateCode.Ok.code, member.toJSON()));
        markResponseTime();
    }
}