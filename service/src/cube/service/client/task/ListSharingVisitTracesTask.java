/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
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
        JSONObject notifier = copyNotifier(response);

        // 获取文件存储模块
        AbstractModule module = this.getFileStorageModule();
        Object result = module.notify(notification);
        if (null == result) {
            response.addParam("code", FileStorageStateCode.Failure.code);
            this.cellet.speak(talkContext, response);
            return;
        }

        boolean assignSharingCode = notification.has(ListSharingTraces.SHARING_CODE);

        List<VisitTrace> visitTraceList = (List<VisitTrace>) result;

        JSONObject data = new JSONObject();

        if (assignSharingCode) {
            JSONArray array = new JSONArray();
            for (VisitTrace trace : visitTraceList) {
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
        }
        else {
            data.put("total", visitTraceList.size());
            data.put(ListSharingTraces.BEGIN_TIME, notification.getLong(ListSharingTraces.BEGIN_TIME));
            data.put(ListSharingTraces.END_TIME, notification.getLong(ListSharingTraces.END_TIME));

            // 按照时间查询的数据进行异步发送
            this.cellet.getExecutor().execute(() -> {
                for (VisitTrace visitTrace : visitTraceList) {
                    ActionDialect responseData = new ActionDialect(ClientAction.ListSharingTraces.name);
                    responseData.addParam(NoticeData.ASYNC_NOTIFIER, notifier);
                    responseData.addParam("data", visitTrace.toCompactJSON());
                    this.cellet.speak(this.talkContext, responseData);
                }
            });
        }

        response.addParam("code", FileStorageStateCode.Ok.code);
        response.addParam("data", data);

        this.cellet.speak(this.talkContext, response);
    }
}
