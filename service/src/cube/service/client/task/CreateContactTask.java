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
 * 创建联系人任务。
 */
public class CreateContactTask extends ClientTask {

    public CreateContactTask(ClientCellet cellet, TalkContext talkContext, ActionDialect actionDialect) {
        super(cellet, talkContext, actionDialect);
    }

    @Override
    public void run() {
        String domain = actionDialect.getParamAsString("domain");
        Long contactId = actionDialect.getParamAsLong("id");
        String name = actionDialect.getParamAsString("name");
        JSONObject context = actionDialect.containsParam("context")
                ? actionDialect.getParamAsJson("context") : null;

        // 新建联系人
        Contact contact = ContactManager.getInstance().newContact(contactId, domain, name, context);

        ActionDialect result = new ActionDialect(ClientAction.CreateContact.name);
        copyNotifier(result);
        result.addParam("contact", contact.toJSON());

        cellet.speak(talkContext, result);
    }
}
