/*
 * This source file is part of Cube.
 * <p>
 * The MIT License (MIT)
 * <p>
 * Copyright (c) 2020-2024 Ambrose Xu.
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
import cell.util.log.Logger;
import cube.benchmark.ResponseTime;
import cube.common.Packet;
import cube.common.entity.Contact;
import cube.ferry.*;
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
            Logger.w(this.getClass(), "No token parameter");

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

        String domain = null;
        Long contactId = null;

        if (!data.has("contactId")) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, FerryStateCode.InvalidParameter.code, data));
            markResponseTime();
            return;
        }
        contactId = data.getLong("contactId");

        FerryService service = ((FerryCellet) this.cellet).getFerryService();

        if (data.has("invitationCode")) {
            domain = service.getDomainNameByCode(data.getString("invitationCode"));
        }
        else if (data.has("domain")) {
            domain = data.getString("domain");
        }
        else {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, FerryStateCode.InvalidParameter.code, data));
            markResponseTime();
            return;
        }

        if (null == domain || !service.isOnlineDomain(domain)) {
            // 域对应的服务器不在线
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, FerryStateCode.InvalidDomain.code, data));
            markResponseTime();
            return;
        }

        JoinWay joinWay = null;
        try {
            joinWay = JoinWay.parse(data.getInt("way"));
        } catch (Exception e) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, FerryStateCode.InvalidParameter.code, data));
            markResponseTime();
            return;
        }

        DomainMember member = null;
        List<DomainMember> list = service.listDomainMember(domain, DomainMember.NORMAL);
        if (list.isEmpty()) {
            // 首次绑定
            member = new DomainMember(domain, contactId, joinWay,
                    System.currentTimeMillis(), Role.Administrator, DomainMember.NORMAL);
        }
        else {
            for (DomainMember dm : list) {
                if (dm.getContactId().equals(contactId)) {
                    // 已加入
                    JSONObject response = new JSONObject();
                    response.put("authDomain", service.getAuthDomain(domain).toJSON());
                    response.put("domainInfo", service.getDomainInfo(domain).toJSON());
                    response.put("member", dm.toJSON());
                    this.cellet.speak(this.talkContext,
                            this.makeResponse(action, packet, FerryStateCode.Ok.code, response));
                    markResponseTime();
                    return;
                }
            }

            // 加入
            member = new DomainMember(domain, contactId, joinWay,
                    System.currentTimeMillis(), Role.Member, DomainMember.NORMAL);
        }

        // 转入
        DomainInfo domainInfo = service.transferIntoDomainMember(contact, member, list);

        JSONObject response = new JSONObject();
        response.put("authDomain", service.getAuthDomain(domain).toJSON());
        response.put("domainInfo", domainInfo.toJSON());
        response.put("member", member.toJSON());

        this.cellet.speak(this.talkContext,
                this.makeResponse(action, packet, FerryStateCode.Ok.code, response));
        markResponseTime();
    }
}
