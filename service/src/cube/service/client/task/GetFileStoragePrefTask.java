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
 * 获取文件存储的联系人性能。
 */
public class GetFileStoragePrefTask extends ClientTask {

    public GetFileStoragePrefTask(ClientCellet cellet, TalkContext talkContext, ActionDialect actionDialect) {
        super(cellet, talkContext, actionDialect);
    }

    @Override
    public void run() {
        String domain = this.actionDialect.getParamAsString("domain");
        long contactId = this.actionDialect.getParamAsLong("contactId");

        JSONObject notification = new JSONObject();
        notification.put("action", FileStorageAction.Performance.name);
        notification.put("domain", domain);
        notification.put("contactId", contactId);

        ActionDialect response = new ActionDialect(ClientAction.GetFilePerf.name);
        copyNotifier(response);

        // 获取文件信息
        AbstractModule module = this.getFileStorageModule();
        Object result = module.notify(notification);
        if (null == result) {
            response.addParam("code", FileStorageStateCode.Failure.code);
            this.cellet.speak(this.talkContext, response);
            return;
        }

        response.addParam("code", FileStorageStateCode.Ok.code);
        response.addParam("performance", (JSONObject) result);
        this.cellet.speak(this.talkContext, response);
    }
}
