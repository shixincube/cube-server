/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.client.task;

import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cube.common.action.ClientAction;
import cube.common.action.MessagingAction;
import cube.core.AbstractModule;
import cube.service.client.ClientCellet;
import org.json.JSONObject;

/**
 * 查询消息任务。
 */
public class QueryMessagesTask extends ClientTask {

    /**
     * 构造函数。
     *
     * @param cellet
     * @param talkContext
     * @param actionDialect
     */
    public QueryMessagesTask(ClientCellet cellet, TalkContext talkContext, ActionDialect actionDialect) {
        super(cellet, talkContext, actionDialect);
    }

    @Override
    public void run() {
        ActionDialect response = new ActionDialect(ClientAction.PushMessage.name);
        copyNotifier(response);

        AbstractModule module = this.getMessagingModule();
        if (null == module) {
            cellet.speak(talkContext, response);
            return;
        }

        long beginning = actionDialect.getParamAsLong("beginning");
        long ending = actionDialect.getParamAsLong("ending");
        String domain = actionDialect.getParamAsString("domain");
        Long contactId = actionDialect.containsParam("contactId")
                ? actionDialect.getParamAsLong("contactId") : null;
        Long groupId = actionDialect.containsParam("groupId")
                ? actionDialect.getParamAsLong("groupId") : null;

        JSONObject notification = new JSONObject();
        notification.put("action", MessagingAction.Pull.name);
        notification.put("beginning", beginning);
        notification.put("ending", ending);

        notification.put("domain", domain);

        if (null != contactId) {
            notification.put("contactId", contactId.longValue());
        }
        else if (null != groupId) {
            notification.put("groupId", groupId.longValue());
        }

        // 使用 notify 通知模块
        JSONObject result = (JSONObject) module.notify(notification);
        if (null != result) {
            response.addParam("result", result);
        }

        cellet.speak(talkContext, response);
    }
}
