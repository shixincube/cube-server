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
import org.json.JSONObject;

/**
 * 更新联系人任务。
 */
public class UpdateContactTask extends ClientTask {

    public UpdateContactTask(ClientCellet cellet, TalkContext talkContext, ActionDialect actionDialect) {
        super(cellet, talkContext, actionDialect);
    }

    @Override
    public void run() {
        String domain = actionDialect.getParamAsString("domain");
        Long contactId = actionDialect.getParamAsLong("id");
        String name = actionDialect.containsParam("name")
                ? actionDialect.getParamAsString("name") : null;
        JSONObject context = actionDialect.containsParam("context")
                ? actionDialect.getParamAsJson("context") : null;

        // 获取联系人
        Contact contact = ContactManager.getInstance().updateContact(domain, contactId, name, context);

        ActionDialect result = new ActionDialect(ClientAction.UpdateContact.name);
        copyNotifier(result);
        if (null != contact) {
            result.addParam("contact", contact.toJSON());
        }

        cellet.speak(talkContext, result);
    }
}
