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
 * 查找文件任务。
 */
public class FindFileTask extends ClientTask {

    public FindFileTask(ClientCellet cellet, TalkContext talkContext, ActionDialect actionDialect) {
        super(cellet, talkContext, actionDialect);
    }

    @Override
    public void run() {
        String domain = actionDialect.getParamAsString("domain");
        long contactId = actionDialect.getParamAsLong("contactId");
        String fileName = actionDialect.getParamAsString("fileName");
        long fileSize = actionDialect.getParamAsLong("fileSize");
        long lastModified = actionDialect.getParamAsLong("lastModified");

        JSONObject notification = new JSONObject();
        notification.put("action", FileStorageAction.FindFile.name);
        notification.put("domain", domain);
        notification.put("contactId", contactId);
        notification.put("fileName", fileName);
        notification.put("lastModified", lastModified);
        notification.put("fileSize", fileSize);

        ActionDialect response = new ActionDialect(ClientAction.FindFile.name);
        copyNotifier(response);

        // 获取文件存储模块
        AbstractModule module = this.getFileStorageModule();
        Object result = module.notify(notification);
        if (null == result) {
            response.addParam("code", FileStorageStateCode.NotFound.code);
            cellet.speak(talkContext, response);
            return;
        }

        response.addParam("code", FileStorageStateCode.Ok.code);
        response.addParam("fileLabel", (JSONObject) result);
        cellet.speak(talkContext, response);
    }
}
