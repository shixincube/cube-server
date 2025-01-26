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
 * 推送消息任务。
 */
public class PushMessageTask extends ClientTask {

    public PushMessageTask(ClientCellet cellet, TalkContext talkContext, ActionDialect actionDialect) {
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

        JSONObject message = actionDialect.getParamAsJson("message");
        JSONObject pretender = actionDialect.getParamAsJson("pretender");
        JSONObject device = actionDialect.getParamAsJson("device");

        JSONObject notification = new JSONObject();
        notification.put("action", MessagingAction.Push.name);
        notification.put("message", message);
        notification.put("pretender", pretender);
        notification.put("device", device);

        // 使用 notify 通知模块
        JSONObject result = (JSONObject) module.notify(notification);
        if (null != result) {
            response.addParam("result", result);
        }

        cellet.speak(talkContext, response);
    }
}
