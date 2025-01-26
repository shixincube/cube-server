/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.conference.task;

import cell.core.cellet.Cellet;
import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.core.talk.dialect.DialectFactory;
import cube.benchmark.ResponseTime;
import cube.common.Packet;
import cube.common.entity.Conference;
import cube.common.entity.Contact;
import cube.common.state.ConferenceStateCode;
import cube.service.ServiceTask;
import cube.service.conference.ConferenceService;
import cube.service.contact.ContactManager;
import org.json.JSONObject;

/**
 * 接受会议邀请任务。
 */
public class AcceptInvitationTask extends ServiceTask {

    public AcceptInvitationTask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
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

        // 会议 ID
        Long conferenceId = data.getLong("conferenceId");

        // 接受邀请
        ConferenceService service = (ConferenceService) this.kernel.getModule(ConferenceService.NAME);
        Conference conference = service.acceptInvitation(conferenceId, contact);

        if (null == conference) {
            // 未正确操作
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, ConferenceStateCode.Failure.code, data));
            markResponseTime();
            return;
        }

        this.cellet.speak(this.talkContext,
                this.makeResponse(action, packet, ConferenceStateCode.Ok.code, conference.toCompactJSON()));
        markResponseTime();
    }
}
