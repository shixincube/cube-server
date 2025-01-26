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
 * 删除文件。
 * 该操作不可回滚。
 */
public class DeleteFileTask extends ClientTask {

    public DeleteFileTask(ClientCellet cellet, TalkContext talkContext, ActionDialect actionDialect) {
        super(cellet, talkContext, actionDialect);
    }

    @Override
    public void run() {
        String domain = actionDialect.getParamAsString("domain");
        String fileCode = actionDialect.getParamAsString("fileCode");

        JSONObject notification = new JSONObject();
        notification.put("action", FileStorageAction.DeleteFile.name);
        notification.put("domain", domain);
        notification.put("fileCode", fileCode);

        ActionDialect response = new ActionDialect(ClientAction.DeleteFile.name);
        copyNotifier(response);

        // 获取文件信息
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
