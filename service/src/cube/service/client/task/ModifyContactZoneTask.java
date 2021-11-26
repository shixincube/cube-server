/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 Cube Team.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package cube.service.client.task;

import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cube.common.action.ContactAction;
import cube.common.entity.Contact;
import cube.common.entity.ContactZone;
import cube.common.entity.ContactZoneParticipant;
import cube.service.client.Actions;
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
        ActionDialect response = new ActionDialect(Actions.ModifyContactZone.name);
        copyNotifier(response);

        String domain = actionDialect.getParamAsString("domain");
        Long contactId = actionDialect.getParamAsLong("contactId");

        // 获取联系人
        Contact contact = ContactManager.getInstance().getContact(domain, contactId);

        String zoneName = actionDialect.getParamAsString("zoneName");

        String action = actionDialect.getParamAsString("action");

        ContactZone contactZone = null;

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
        else {
            contactZone = ContactManager.getInstance().getContactZone(contact, zoneName);
        }

        if (null != contactZone) {
            response.addParam("contactZone", contactZone.toJSON());
        }

        cellet.speak(talkContext, response);
    }
}
