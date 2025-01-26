/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.client.task;

import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cube.common.action.ClientAction;
import cube.common.entity.SharingTag;
import cube.common.notice.NoticeData;
import cube.common.state.FileStorageStateCode;
import cube.core.AbstractModule;
import cube.service.client.ClientCellet;
import org.json.JSONObject;

/**
 * 获取分享标签数据。
 */
public class GetSharingTagTask extends ClientTask {

    public GetSharingTagTask(ClientCellet cellet, TalkContext talkContext, ActionDialect actionDialect) {
        super(cellet, talkContext, actionDialect);
    }

    @Override
    public void run() {
        JSONObject notification = this.actionDialect.getParamAsJson(NoticeData.PARAMETER);

        ActionDialect response = new ActionDialect(ClientAction.GetSharingTag.name);
        copyNotifier(response);

        // 获取文件存储模块
        AbstractModule module = this.getFileStorageModule();
        Object result = module.notify(notification);
        if (null == result) {
            response.addParam("code", FileStorageStateCode.Failure.code);
            this.cellet.speak(talkContext, response);
            return;
        }

        SharingTag sharingTag = (SharingTag) result;

        response.addParam("code", FileStorageStateCode.Ok.code);
        response.addParam("data", sharingTag.toCompactJSON());

        this.cellet.speak(this.talkContext, response);
    }
}
