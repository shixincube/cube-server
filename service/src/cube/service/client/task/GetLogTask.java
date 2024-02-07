/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 Ambrose Xu.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
