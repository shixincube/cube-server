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
import cell.util.log.Logger;
import cube.auth.AuthToken;
import cube.benchmark.ResponseTime;
import cube.common.Packet;
import cube.common.entity.VerificationCode;
import cube.common.state.ContactStateCode;
import cube.service.ServiceTask;
import cube.service.contact.ContactManager;
import org.json.JSONObject;

/**
 * 申请验证码。
 */
public class RequestVerificationCodeTask extends ServiceTask {

    public RequestVerificationCodeTask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
        super(cellet, talkContext, primitive, responseTime);
    }

    @Override
    public void run() {
        ActionDialect actionDialect = new ActionDialect(this.primitive);
        Packet packet = new Packet(actionDialect);

        String tokenCode = this.getTokenCode(actionDialect);
        if (null == tokenCode) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(actionDialect, packet, ContactStateCode.InconsistentToken.code, new JSONObject()));
            this.markResponseTime();
            return;
        }

        try {
            AuthToken authToken = ContactManager.getInstance().getAuthService().getToken(tokenCode);
            String dialCode = packet.data.getString("dialCode");
            String isoCode = packet.data.getString("isoCode");
            String phoneNumber = packet.data.getString("phoneNumber");
            VerificationCode verificationCode = ContactManager.getInstance().requestVerificationCode(
                    authToken, dialCode, isoCode, phoneNumber);
            if (null == verificationCode) {
                this.cellet.speak(this.talkContext,
                        this.makeResponse(actionDialect, packet, ContactStateCode.InvalidParameter.code, packet.data));
                this.markResponseTime();
                return;
            }

            this.cellet.speak(this.talkContext,
                    this.makeResponse(actionDialect, packet, ContactStateCode.Ok.code, verificationCode.toCompactJSON()));
            this.markResponseTime();
        } catch (Exception e) {
            Logger.e(this.getClass(), "#run", e);
            this.cellet.speak(this.talkContext,
                    this.makeResponse(actionDialect, packet, ContactStateCode.Failure.code, packet.data));
            this.markResponseTime();
        }
    }
}
