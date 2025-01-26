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
import cube.ferry.FerryStateCode;
import cube.service.ServiceTask;
import cube.service.contact.ContactManager;
import cube.service.ferry.FerryCellet;
import cube.service.ferry.FerryService;
import org.json.JSONObject;

/**
 * 连通性检测任务。
 */
public class PingTask extends ServiceTask {

    public PingTask(FerryCellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
        super(cellet, talkContext, primitive, responseTime);
    }

    @Override
    public void run() {
        ActionDialect action = DialectFactory.getInstance().createActionDialect(this.primitive);
        Packet packet = new Packet(action);

        JSONObject data = packet.data;
        if (!data.has("domain")) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, FerryStateCode.InvalidParameter.code, data));
            markResponseTime();
            return;
        }

        boolean touch = false;
        if (data.has("touch")) {
            touch = data.getBoolean("touch");
        }

        String domain = data.getString("domain");
        FerryService service = ((FerryCellet) this.cellet).getFerryService();

        boolean membership = false;
        boolean online = false;
        long duration = 0;

        if (touch) {
            // 获取当前联系人的令牌码
            String tokenCode = this.getTokenCode(action);
            if (null == tokenCode) {
                Logger.w(this.getClass(), "No token parameter");

                this.cellet.speak(this.talkContext,
                        this.makeResponse(action, packet, FerryStateCode.InvalidToken.code, data));
                markResponseTime();
                return;
            }

            // 获取令牌对应的联系人
            Contact contact = ContactManager.getInstance().getContact(tokenCode);
            if (null == contact) {
                this.cellet.speak(this.talkContext,
                        this.makeResponse(action, packet, FerryStateCode.InvalidToken.code, data));
                markResponseTime();
                return;
            }

            FerryService.MembershipAckBundle ackBundle = service.touchFerryHouse(domain, contact, 60 * 1000);
            if (null != ackBundle) {
                // 成员关系
                membership = ackBundle.membership;

                if (ackBundle.membership && ackBundle.end > 0) {
                    online = true;
                    duration = ackBundle.end - ackBundle.start;
                }
            }
        }
        else {
            online = service.isOnlineDomain(domain);
        }

        JSONObject response = new JSONObject();
        response.put("domain", domain);
        response.put("online", online);
        response.put("duration", duration);
        response.put("membership", membership);

        this.cellet.speak(this.talkContext,
                this.makeResponse(action, packet, FerryStateCode.Ok.code, response));
        markResponseTime();
    }
}
