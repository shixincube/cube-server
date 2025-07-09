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
import cube.common.entity.Device;
import cube.common.state.AIGCStateCode;
import cube.service.ServiceTask;
import cube.service.aigc.AIGCCellet;
import cube.service.aigc.AIGCService;
import cube.service.contact.ContactManager;
import org.json.JSONObject;

/**
 * 检查令牌任务。
 */
public class CheckTokenTask extends ServiceTask {

    public CheckTokenTask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
        super(cellet, talkContext, primitive, responseTime);
    }

    @Override
    public void run() {
        ActionDialect dialect = new ActionDialect(this.primitive);
        Packet packet = new Packet(dialect);

        if (!packet.data.has("token") && !packet.data.has("invitation")) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.InvalidParameter.code, new JSONObject()));
            markResponseTime();
            return;
        }

        String tokenCode = packet.data.has("token") ? packet.data.getString("token") : null;
        String invitationCode = packet.data.has("invitation") ? packet.data.getString("invitation") : null;
        JSONObject deviceJson = packet.data.has("device") ? packet.data.getJSONObject("device") : null;

        if (null == deviceJson) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.InvalidParameter.code, new JSONObject()));
            markResponseTime();
            return;
        }

        AIGCService service = ((AIGCCellet) this.cellet).getService();

        if (null == tokenCode) {
            // 用邀请码获取令牌
            tokenCode = service.queryTokenByInvitation(invitationCode);
            if (null == tokenCode) {
                // 无效的邀请码
                this.cellet.speak(this.talkContext,
                        this.makeResponse(dialect, packet, AIGCStateCode.InvalidParameter.code, new JSONObject()));
                markResponseTime();
                return;
            }
        }

        // 校验唯一性
        Device device = new Device(deviceJson);
        Contact contact = ContactManager.getInstance().verifyOnlineUniqueness(tokenCode, device);
        if (null == contact) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.Failure.code, new JSONObject()));
            markResponseTime();
            return;
        }

        AuthToken authToken = service.getToken(tokenCode);
        if (null == authToken) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.InconsistentToken.code, new JSONObject()));
            markResponseTime();
            return;
        }

        JSONObject responseData = new JSONObject();
        responseData.put("token", authToken.toJSON());
        responseData.put("contact", contact.toJSON());

        this.cellet.speak(this.talkContext,
                this.makeResponse(dialect, packet, AIGCStateCode.Ok.code, responseData));
        markResponseTime();
    }
}
