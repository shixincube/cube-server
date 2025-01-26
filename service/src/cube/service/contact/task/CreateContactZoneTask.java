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
import cube.benchmark.ResponseTime;
import cube.common.Packet;
import cube.common.entity.Contact;
import cube.common.entity.ContactZone;
import cube.common.entity.ContactZoneParticipant;
import cube.common.state.ContactStateCode;
import cube.service.ServiceTask;
import cube.service.contact.ContactManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 添加联系人到分区任务。
 */
public class CreateContactZoneTask extends ServiceTask {

    public CreateContactZoneTask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
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

        String zoneName = null;
        List<ContactZoneParticipant> participantList = null;
        String displayName = null;
        boolean peerMode = false;
        try {
            zoneName = data.getString("name");

            if (data.has("participants")) {
                JSONArray array = data.getJSONArray("participants");
                participantList = new ArrayList<>(array.length());
                for (int i = 0; i < array.length(); ++i) {
                    ContactZoneParticipant participant = new ContactZoneParticipant(array.getJSONObject(i));
                    participantList.add(participant);
                }
            }

            if (data.has("displayName")) {
                displayName = data.getString("displayName");
            }

            if (data.has("peerMode")) {
                peerMode = data.getBoolean("peerMode");
            }
        } catch (JSONException e) {
            Logger.w(this.getClass(), "#run", e);
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, ContactStateCode.InvalidParameter.code, data));
            markResponseTime();
            return;
        }

        // 创建联系人分区
        ContactZone zone = ContactManager.getInstance().createContactZone(contact, zoneName, displayName,
                peerMode, participantList);
        if (null == zone) {
            // 分区已存在
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, ContactStateCode.Failure.code, data));
            markResponseTime();
            return;
        }

        this.cellet.speak(this.talkContext,
                this.makeResponse(action, packet, ContactStateCode.Ok.code, zone.toJSON()));
        markResponseTime();
    }
}
