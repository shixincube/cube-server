/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.client.task;

import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cube.common.action.ClientAction;
import cube.common.entity.Contact;
import cube.service.client.ClientCellet;
import cube.service.contact.ContactManager;

/**
 * 获取联系人任务。
 */
public class GetContactTask extends ClientTask {

    public GetContactTask(ClientCellet cellet, TalkContext talkContext, ActionDialect actionDialect) {
        super(cellet, talkContext, actionDialect);
    }

    @Override
    public void run() {
        Contact contact = null;
        if (actionDialect.containsParam("domain") && actionDialect.containsParam("contactId")) {
            String domain = actionDialect.getParamAsString("domain");
            Long contactId = actionDialect.getParamAsLong("contactId");

            // 获取联系人
            contact = ContactManager.getInstance().getContact(domain, contactId);
        }
        else if (actionDialect.containsParam("token")) {
            // 获取联系人
            contact = ContactManager.getInstance().getContact(actionDialect.getParamAsString("token"));
        }

        ActionDialect result = new ActionDialect(ClientAction.GetContact.name);
        copyNotifier(result);

        if (null != contact) {
            result.addParam("contact", contact.toJSON());
        }

        cellet.speak(talkContext, result);
    }
}
