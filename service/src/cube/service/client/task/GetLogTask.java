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
import cube.common.state.AuthStateCode;
import cube.common.state.FileStorageStateCode;
import cube.core.AbstractModule;
import cube.report.LogLine;
import cube.service.client.ClientCellet;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * 获取日志数据。
 */
public class GetLogTask extends ClientTask {

    public GetLogTask(ClientCellet cellet, TalkContext talkContext, ActionDialect actionDialect) {
        super(cellet, talkContext, actionDialect);
    }

    @Override
    public void run() {
        int limit = this.actionDialect.getParamAsInt("limit");

        ActionDialect response = new ActionDialect(ClientAction.GetLog.name);
        copyNotifier(response);

        // 从守护线程获取日志列表
        List<LogLine> logList = this.cellet.getDaemon().getLogRecords(limit);
        JSONArray list = new JSONArray();
        for (LogLine line : logList) {
            list.put(line.toJSON());
        }

        JSONObject data = new JSONObject();
        data.put("limit", limit);
        data.put("logs", list);

        response.addParam("code", AuthStateCode.Ok.code);
        response.addParam("data", data);
        this.cellet.speak(this.talkContext, response);
    }
}
