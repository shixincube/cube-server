/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
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
import cube.ferry.DomainInfo;
import cube.ferry.DomainMember;
import cube.ferry.FerryStateCode;
import cube.service.ServiceTask;
import cube.service.contact.ContactManager;
import cube.service.ferry.FerryCellet;
import cube.service.ferry.FerryService;
import org.json.JSONObject;

/**
 * 退出域任务。
 */
public class QuitDomainTask extends ServiceTask {

    public QuitDomainTask(FerryCellet cellet, TalkContext talkContext, Primitive primitive,
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

        // 查找成员
        DomainMember domainMember = service.getDomainMember(domain, contactId);
        if (null == domainMember) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, FerryStateCode.NotFindMember.code, data));
            markResponseTime();
            return;
        }

        // 转出
        DomainInfo domainInfo = service.transferOutDomainMember(domainMember);

        JSONObject response = new JSONObject();
        response.put("authDomain", service.getAuthDomain(domain).toJSON());
        response.put("domainInfo", domainInfo.toJSON());
        response.put("member", domainMember.toJSON());

        this.cellet.speak(this.talkContext,
                this.makeResponse(action, packet, FerryStateCode.Ok.code, response));
        markResponseTime();
    }
}
