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
 * 标记消息已读任务。
 */
public class MarkReadTask extends ClientTask {

    public MarkReadTask(ClientCellet cellet, TalkContext talkContext, ActionDialect actionDialect) {
        super(cellet, talkContext, actionDialect);
    }

    @Override
    public void run() {
        ActionDialect response = new ActionDialect(ClientAction.MarkReadMessages.name);
        copyNotifier(response);

        AbstractModule module = this.getMessagingModule();
        if (null == module) {
            cellet.speak(talkContext, response);
            return;
        }

        String domain = actionDialect.getParamAsString("domain");
        JSONObject data = actionDialect.getParamAsJson("data");

        JSONObject notification = new JSONObject();
        notification.put("action", MessagingAction.Read.name);
        notification.put("domain", domain);
        notification.put("to", data.getLong("to"));
        notification.put("from", data.getLong("from"));
        notification.put("list", data.getJSONArray("list"));

        // 使用 notify 通知模块
        JSONObject result = (JSONObject) module.notify(notification);
        if (null != result) {
            response.addParam("result", result);
        }

        cellet.speak(talkContext, response);
    }
}
