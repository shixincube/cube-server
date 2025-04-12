/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.task;

import cell.core.cellet.Cellet;
import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cube.auth.AuthToken;
import cube.benchmark.ResponseTime;
import cube.common.Packet;
import cube.common.entity.Contact;
import cube.common.state.AIGCStateCode;
import cube.service.ServiceTask;
import cube.service.aigc.AIGCCellet;
import cube.service.aigc.AIGCService;
import cube.service.contact.ContactManager;
import org.json.JSONObject;

/**
 * 注入或获取指定联系人的令牌任务。
 */
public class AppInjectOrGetTokenTask extends ServiceTask {

    public AppInjectOrGetTokenTask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
        super(cellet, talkContext, primitive, responseTime);
    }

    @Override
    public void run() {
        ActionDialect dialect = new ActionDialect(this.primitive);
        Packet packet = new Packet(dialect);

        if (!packet.data.has("phone")) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.InvalidParameter.code, new JSONObject()));
            markResponseTime();
            return;
        }

        String phoneNumber = packet.data.has("phone") ? packet.data.getString("phone") : null;
        String userName = packet.data.has("name") ? packet.data.getString("name") : null;

        AIGCService service = ((AIGCCellet) this.cellet).getService();

        AuthToken token = service.getOrInjectAuthToken(phoneNumber, userName);
        if (null == token) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.Failure.code, new JSONObject()));
            markResponseTime();
            return;
        }

        Contact contact = ContactManager.getInstance().getContact(token.getDomain(), token.getContactId());

        JSONObject responseData = new JSONObject();
        responseData.put("token", token.toJSON());
        responseData.put("contact", contact.toJSON());

        this.cellet.speak(this.talkContext,
                this.makeResponse(dialect, packet, AIGCStateCode.Ok.code, responseData));
        markResponseTime();
    }
}
