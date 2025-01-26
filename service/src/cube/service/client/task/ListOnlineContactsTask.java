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
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * 获取所有在线用户。
 */
public class ListOnlineContactsTask extends ClientTask {

    public ListOnlineContactsTask(ClientCellet cellet, TalkContext talkContext, ActionDialect actionDialect) {
        super(cellet, talkContext, actionDialect);
    }

    @Override
    public void run() {
        // 从联系人模块获取所有在线联系人
        List<Contact> contactList = ContactManager.getInstance().getAllOnlineContacts();

        JSONArray contacts = new JSONArray();
        for (Contact contact : contactList) {
            contacts.put(contact.toJSON());
        }

        JSONObject data = new JSONObject();
        data.put("contacts", contacts);

        ActionDialect result = new ActionDialect(ClientAction.ListOnlineContacts.name);
        copyNotifier(result);
        result.addParam("data", data);

        cellet.speak(talkContext, result);
    }
}
