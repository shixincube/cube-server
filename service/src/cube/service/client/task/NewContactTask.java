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
 * 获取联系人任务。
 */
public class NewContactTask extends ClientTask {

    public NewContactTask(ClientCellet cellet, TalkContext talkContext, ActionDialect actionDialect) {
        super(cellet, talkContext, actionDialect);
    }

    @Override
    public void run() {
        JSONObject contactJson = actionDialect.getParamAsJson("contact");
        Contact contact = new Contact(contactJson);

        // 新建联系人
        contact = ContactManager.getInstance().newContact(contact);

        ActionDialect result = new ActionDialect(ClientAction.NewContact.name);
        copyNotifier(result);
        result.addParam("contact", contact.toJSON());

        cellet.speak(talkContext, result);
    }
}
