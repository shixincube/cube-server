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
import cube.benchmark.ResponseTime;
import cube.common.Packet;
import cube.common.entity.Contact;
import cube.common.entity.ContactSearchResult;
import cube.common.state.ConferenceStateCode;
import cube.service.ServiceTask;
import cube.service.contact.ContactManager;
import org.json.JSONObject;

/**
 * 搜索联系人及群组任务。
 */
public class SearchTask extends ServiceTask {

    public SearchTask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
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
                    this.makeResponse(action, packet, ConferenceStateCode.InvalidParameter.code, data));
            markResponseTime();
            return;
        }

        Contact contact = ContactManager.getInstance().getContact(tokenCode);
        if (null == contact) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, ConferenceStateCode.NoSignIn.code, data));
            markResponseTime();
            return;
        }

        // 关键字
        String keyword = null;

        // 按照 ID 检索
        String contactId = null;

        if (data.has("keyword")) {
            keyword = data.getString("keyword");
        }
        else if (data.has("contactId")) {
            contactId = data.getString("contactId");
        }
        else {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, ConferenceStateCode.InvalidParameter.code, data));
            markResponseTime();
            return;
        }

        // 搜索
        ContactSearchResult result = null;

        if (null != keyword) {
            result = ContactManager.getInstance().searchWithFuzzyRule(contact.getDomain().getName(), keyword);
        }
        else {
            result = ContactManager.getInstance().searchWithContactId(contact.getDomain().getName(),
                    Long.parseLong(contactId));
        }

        this.cellet.speak(this.talkContext,
                this.makeResponse(action, packet, ConferenceStateCode.Ok.code, result.toCompactJSON()));
        markResponseTime();
    }
}
