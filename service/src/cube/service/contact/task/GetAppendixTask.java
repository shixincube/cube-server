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
import cube.common.entity.ContactAppendix;
import cube.common.entity.Group;
import cube.common.entity.GroupAppendix;
import cube.common.state.ContactStateCode;
import cube.service.ServiceTask;
import cube.service.contact.ContactManager;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 获取附录任务。
 */
public class GetAppendixTask extends ServiceTask {

    public GetAppendixTask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
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

        if (data.has("contactId")) {
            Long contactId = data.getLong("contactId");
            Contact target = ContactManager.getInstance().getContact(contact.getDomain().getName(), contactId);

            ContactAppendix appendix = ContactManager.getInstance().getAppendix(target);

            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, ContactStateCode.Ok.code, appendix.packJSON(contact)));
        }
        else if (data.has("groupId")) {
            Long groupId = data.getLong("groupId");
            Group group = ContactManager.getInstance().getGroup(groupId, contact.getDomain().getName());
            if (null == group) {
                this.cellet.speak(this.talkContext,
                        this.makeResponse(action, packet, ContactStateCode.NotFindGroup.code, data));
                markResponseTime();
                return;
            }

            // 判断是否是群组成员
            if (!group.hasMember(contact.getId())) {
                this.cellet.speak(this.talkContext,
                        this.makeResponse(action, packet, ContactStateCode.IllegalOperation.code, data));
                markResponseTime();
                return;
            }

            GroupAppendix appendix = ContactManager.getInstance().getAppendix(group);

            // 是否是仅获取 Comm ID 数据
            if (data.has("commId")) {
                JSONObject result = new JSONObject();
                result.put("commId", appendix.getCommId());

                this.cellet.speak(this.talkContext,
                        this.makeResponse(action, packet, ContactStateCode.Ok.code, result));
            }
            else {
                this.cellet.speak(this.talkContext,
                        this.makeResponse(action, packet, ContactStateCode.Ok.code, appendix.packJSON(contact.getId())));
            }
        }
        else {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, ContactStateCode.Failure.code, data));
        }

        markResponseTime();
    }
}
