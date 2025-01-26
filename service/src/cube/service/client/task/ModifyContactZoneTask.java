/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.client.task;

import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cube.common.action.ClientAction;
import cube.common.action.ContactAction;
import cube.common.entity.Contact;
import cube.common.entity.ContactZone;
import cube.common.entity.ContactZoneParticipant;
import cube.service.client.ClientCellet;
import cube.service.contact.ContactManager;
import org.json.JSONObject;

/**
 * 修改联系人分区数据。
 */
public class ModifyContactZoneTask extends ClientTask {

    public ModifyContactZoneTask(ClientCellet cellet, TalkContext talkContext, ActionDialect actionDialect) {
        super(cellet, talkContext, actionDialect);
    }

    @Override
    public void run() {
        ActionDialect response = new ActionDialect(ClientAction.ModifyContactZone.name);
        copyNotifier(response);

        String domain = actionDialect.getParamAsString("domain");
        Long contactId = actionDialect.getParamAsLong("contactId");

        // 获取联系人
        Contact contact = ContactManager.getInstance().getContact(domain, contactId);

        String zoneName = actionDialect.getParamAsString("zoneName");

        String action = actionDialect.getParamAsString("action");

        ContactZone contactZone = null;
        ContactZoneParticipant zoneParticipant = null;

        if (action.equals(ContactAction.AddParticipantToZone.name)) {
            JSONObject participantJson = actionDialect.getParamAsJson("participant");
            ContactZoneParticipant participant = new ContactZoneParticipant(participantJson);
            contactZone = ContactManager.getInstance().addParticipantToZone(contact, zoneName, participant);
        }
        else if (action.equals(ContactAction.RemoveParticipantFromZone.name)) {
            JSONObject participantJson = actionDialect.getParamAsJson("participant");
            ContactZoneParticipant participant = new ContactZoneParticipant(participantJson);
            contactZone = ContactManager.getInstance().removeParticipantFromZone(contact, zoneName, participant);
        }
        else if (action.equals(ContactAction.ModifyZoneParticipant.name)) {
            JSONObject participantJson = actionDialect.getParamAsJson("participant");
            ContactZoneParticipant participant = new ContactZoneParticipant(participantJson);
            zoneParticipant = ContactManager.getInstance().modifyZoneParticipant(contact, zoneName, participant);
        }
        else {
            contactZone = ContactManager.getInstance().getContactZone(contact, zoneName);
        }

        if (null != contactZone) {
            response.addParam("contactZone", contactZone.toJSON());
        }
        if (null != zoneParticipant) {
            response.addParam("participant", zoneParticipant.toCompactJSON());
        }

        cellet.speak(talkContext, response);
    }
}
