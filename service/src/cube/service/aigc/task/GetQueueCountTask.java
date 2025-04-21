/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.task;

import cell.core.cellet.Cellet;
import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cube.benchmark.ResponseTime;
import cube.common.Packet;
import cube.common.state.AIGCStateCode;
import cube.service.ServiceTask;
import cube.service.aigc.AIGCCellet;
import cube.service.aigc.AIGCService;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 获取队列计数任务。
 */
public class GetQueueCountTask extends ServiceTask {

    public GetQueueCountTask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
        super(cellet, talkContext, primitive, responseTime);
    }

    @Override
    public void run() {
        ActionDialect dialect = new ActionDialect(this.primitive);
        Packet packet = new Packet(dialect);

        String token = getTokenCode(dialect);
        if (null == token) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.InvalidParameter.code, new JSONObject()));
            markResponseTime();
            return;
        }

        AIGCService service = ((AIGCCellet) this.cellet).getService();
        Map<String, AtomicInteger> data = service.getGenerateTextUnitRealtimeCount();

        int total = 0;
        JSONArray array = new JSONArray();
        for (Map.Entry<String, AtomicInteger> entry : data.entrySet()) {
            JSONObject item = new JSONObject();
            item.put("unit", entry.getKey());
            item.put("count", entry.getValue().get());
            total += entry.getValue().get();
            array.put(item);
        }

        JSONObject responseData = new JSONObject();
        responseData.put("list", array);
        responseData.put("total", total);

        this.cellet.speak(this.talkContext,
                this.makeResponse(dialect, packet, AIGCStateCode.Ok.code, responseData));
        markResponseTime();
    }
}
