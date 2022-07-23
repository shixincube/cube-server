/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 Cube Team.
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
import cube.common.entity.SharingTag;
import cube.common.entity.VisitTrace;
import cube.common.notice.FileListSharingTags;
import cube.common.notice.FileListTraces;
import cube.common.state.FileStorageStateCode;
import cube.core.AbstractModule;
import cube.service.client.ClientCellet;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * 批量获取分享痕迹记录。
 */
public class ListSharingTracesTask extends ClientTask {

    public ListSharingTracesTask(ClientCellet cellet, TalkContext talkContext, ActionDialect actionDialect) {
        super(cellet, talkContext, actionDialect);
    }

    @Override
    public void run() {
        JSONObject notification = this.actionDialect.getParamAsJson("parameter");

        ActionDialect response = new ActionDialect(ClientAction.ListSharingTraces.name);
        copyNotifier(response);

        // 获取文件存储模块
        AbstractModule module = this.getFileStorageModule();
        Object result = module.notify(notification);
        if (null == result) {
            response.addParam("code", FileStorageStateCode.Failure.code);
            this.cellet.speak(talkContext, response);
            return;
        }

        List<VisitTrace> sharingTagList = (List<VisitTrace>) result;

        JSONObject data = new JSONObject();
        JSONArray array = new JSONArray();
        for (VisitTrace trace : sharingTagList) {
            array.put(trace.toCompactJSON());
        }
        data.put("list", array);
        data.put(FileListTraces.BEGIN, notification.getInt(FileListTraces.BEGIN));
        data.put(FileListTraces.END, notification.getInt(FileListTraces.END));
        data.put(FileListTraces.SHARING_CODE, notification.getBoolean(FileListTraces.SHARING_CODE));

        response.addParam("code", FileStorageStateCode.Ok.code);
        response.addParam("data", data);

        this.cellet.speak(this.talkContext, response);
    }
}
