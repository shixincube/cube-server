/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.client.task;

import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cube.common.action.ClientAction;
import cube.common.action.FileStorageAction;
import cube.common.state.FileStorageStateCode;
import cube.core.AbstractModule;
import cube.service.client.ClientCellet;
import org.json.JSONObject;

/**
 * 放置文件标签。
 */
public class PutFileTask extends ClientTask {

    public PutFileTask(ClientCellet cellet, TalkContext talkContext, ActionDialect actionDialect) {
        super(cellet, talkContext, actionDialect);
    }

    @Override
    public void run() {
        JSONObject fileLabel = actionDialect.getParamAsJson("fileLabel");

        JSONObject notification = new JSONObject();
        notification.put("action", FileStorageAction.PutFile.name);
        notification.put("fileLabel", fileLabel);

        ActionDialect response = new ActionDialect(ClientAction.PutFile.name);
        copyNotifier(response);

        // 获取文件存储模块
        AbstractModule module = this.getFileStorageModule();
        Object result = module.notify(notification);
        if (null == result) {
            response.addParam("code", FileStorageStateCode.Failure.code);
            cellet.speak(talkContext, response);
            return;
        }

        response.addParam("code", FileStorageStateCode.Ok.code);
        response.addParam("fileLabel", (JSONObject) result);
        cellet.speak(talkContext, response);
    }
}
