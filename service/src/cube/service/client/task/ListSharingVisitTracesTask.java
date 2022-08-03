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
import cube.common.entity.VisitTrace;
import cube.common.notice.CountSharingVisitTraces;
import cube.common.notice.ListSharingTraces;
import cube.common.notice.NoticeData;
import cube.common.state.FileStorageStateCode;
import cube.core.AbstractModule;
import cube.service.client.ClientCellet;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * 批量获取分享痕迹记录。
 */
public class ListSharingVisitTracesTask extends ClientTask {

    public ListSharingVisitTracesTask(ClientCellet cellet, TalkContext talkContext, ActionDialect actionDialect) {
        super(cellet, talkContext, actionDialect);
    }

    @Override
    public void run() {
        JSONObject notification = this.actionDialect.getParamAsJson(NoticeData.PARAMETER);

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
        data.put(ListSharingTraces.BEGIN, notification.getInt(ListSharingTraces.BEGIN));
        data.put(ListSharingTraces.END, notification.getInt(ListSharingTraces.END));
        data.put(ListSharingTraces.SHARING_CODE, notification.getString(ListSharingTraces.SHARING_CODE));

        // 总数
        CountSharingVisitTraces countSharingVisitTraces = new CountSharingVisitTraces(
                notification.getString(ListSharingTraces.DOMAIN),
                notification.getLong(ListSharingTraces.CONTACT_ID),
                notification.getString(ListSharingTraces.SHARING_CODE));
        result = module.notify(countSharingVisitTraces);
        data.put("total", ((JSONObject)result).getInt(CountSharingVisitTraces.TOTAL));

        response.addParam("code", FileStorageStateCode.Ok.code);
        response.addParam("data", data);

        this.cellet.speak(this.talkContext, response);
    }
}
