/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.client.task;

import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cube.common.action.ClientAction;
import cube.common.entity.Group;
import cube.service.client.ClientCellet;
import cube.service.contact.ContactManager;

/**
 * 获取群组任务。
 */
public class GetGroupTask extends ClientTask {

    public GetGroupTask(ClientCellet cellet, TalkContext talkContext, ActionDialect actionDialect) {
        super(cellet, talkContext, actionDialect);
    }

    @Override
    public void run() {
        String domain = actionDialect.getParamAsString("domain");
        Long groupId = actionDialect.getParamAsLong("groupId");

        // 获取联系人
        Group group = ContactManager.getInstance().getGroup(groupId, domain);

        ActionDialect result = new ActionDialect(ClientAction.GetGroup.name);
        copyNotifier(result);

        if (null != group) {
            result.addParam("group", group.toJSON());
        }

        cellet.speak(talkContext, result);
    }
}
