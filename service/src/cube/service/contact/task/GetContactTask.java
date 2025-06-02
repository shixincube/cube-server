/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.contact.task;

import cell.core.cellet.Cellet;
import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.core.talk.dialect.DialectFactory;
import cell.util.log.Logger;
import cube.auth.AuthToken;
import cube.benchmark.ResponseTime;
import cube.common.Packet;
import cube.common.entity.Contact;
import cube.common.entity.ContactSearchResult;
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

        String tokenCode = this.getTokenCode(action);
        AuthToken authToken = ContactManager.getInstance().getAuthService().getToken(tokenCode);
        if (null == authToken) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, ContactStateCode.IllegalOperation.code, packet.data));
            markResponseTime();
            return;
        }

        JSONObject data = packet.data;

        Long id = null;
        String domain = null;
        String code = null;
        String name = null;
        try {
            if (data.has("id")) {
                id = data.getLong("id");
                domain = data.getString("domain");
            }
            else if (data.has("code")) {
                code = data.getString("code");
            }
            else {
                name = data.getString("name");
            }
        } catch (Exception e) {
            Logger.w(this.getClass(), "#run", e);
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, ContactStateCode.InvalidParameter.code, data));
            markResponseTime();
            return;
        }

        Contact contact = null;
        try {
            if (null != id && null != domain) {
                contact = ContactManager.getInstance().getContact(domain, id);
            }
            else if (null != code){
                contact = ContactManager.getInstance().getContact(code);
            }
            else {
                ContactSearchResult result = ContactManager.getInstance().searchWithContactName(authToken.getDomain(), name);
                if (!result.getContactList().isEmpty()) {
                    contact = result.getContactList().get(0);
                }
            }
        } catch (Exception e) {
            Logger.w(this.getClass(), "#run", e);
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, ContactStateCode.Failure.code, data));
            markResponseTime();
            return;
        }

        if (null == contact) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, ContactStateCode.NotFindContact.code, data));
            markResponseTime();
            return;
        }

        this.cellet.speak(this.talkContext,
                this.makeResponse(action, packet, ContactStateCode.Ok.code, contact.toJSON()));
        markResponseTime();
    }
}
