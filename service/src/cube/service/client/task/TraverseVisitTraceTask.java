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
import cube.common.notice.NoticeData;
import cube.common.state.FileStorageStateCode;
import cube.core.AbstractModule;
import cube.service.client.ClientCellet;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 遍历指定层级访问记录任务。
 */
public class TraverseVisitTraceTask extends ClientTask {

    public TraverseVisitTraceTask(ClientCellet cellet, TalkContext talkContext, ActionDialect actionDialect) {
        super(cellet, talkContext, actionDialect);
    }

    @Override
    public void run() {
        long sn = this.actionDialect.getParamAsLong("sn");
        JSONObject notification = this.actionDialect.getParamAsJson(NoticeData.PARAMETER);

        // 获取文件存储模块
        AbstractModule module = this.getFileStorageModule();
        Object result = module.notify(notification);
        if (null == result) {
            ActionDialect response = new ActionDialect(ClientAction.TraverseVisitTrace.name);
            response.addParam("sn", sn);
            response.addParam("code", FileStorageStateCode.Failure.code);
            this.cellet.speak(talkContext, response);
            return;
        }

        List<VisitTrace> resultList = (List<VisitTrace>) result;

        // 分批返回
        final int batchSize = 10;
        final int total = resultList.size();

        if (0 == total) {
            ActionDialect response = new ActionDialect(ClientAction.TraverseVisitTrace.name);
            response.addParam("sn", sn);
            response.addParam("code", FileStorageStateCode.NotFound.code);
            response.addParam("total", total);
            this.cellet.speak(talkContext, response);
            return;
        }

        List<VisitTrace> batchList = new ArrayList<>();
        for (VisitTrace trace : resultList) {
            // 添加到批量列表
            batchList.add(trace);

            if (batchList.size() == batchSize) {
                JSONArray array = new JSONArray();
                for (VisitTrace vt : batchList) {
                    array.put(vt.toCompactJSON());
                }
                JSONObject data = new JSONObject();
                data.put("list", array);

                ActionDialect response = new ActionDialect(ClientAction.TraverseVisitTrace.name);
                response.addParam("sn", sn);
                response.addParam("code", FileStorageStateCode.Ok.code);
                response.addParam("total", total);  // 总数据量
                response.addParam("size", batchSize);   // 当前批次数据量
                response.addParam("data", data);
                this.cellet.speak(talkContext, response);

                batchList.clear();
            }
        }

        if (!batchList.isEmpty()) {
            // 剩余的数据
            JSONArray array = new JSONArray();
            for (VisitTrace vt : batchList) {
                array.put(vt.toCompactJSON());
            }
            JSONObject data = new JSONObject();
            data.put("list", array);

            ActionDialect response = new ActionDialect(ClientAction.TraverseVisitTrace.name);
            response.addParam("sn", sn);
            response.addParam("code", FileStorageStateCode.Ok.code);
            response.addParam("total", total);  // 总数据量
            response.addParam("size", batchSize);   // 当前批次数据量
            response.addParam("data", data);
            this.cellet.speak(talkContext, response);
        }
    }
}
