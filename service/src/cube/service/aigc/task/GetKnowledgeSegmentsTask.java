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
import cube.common.entity.KnowledgeSegment;
import cube.common.state.AIGCStateCode;
import cube.service.ServiceTask;
import cube.service.aigc.AIGCCellet;
import cube.service.aigc.AIGCService;
import cube.service.aigc.knowledge.KnowledgeBase;
import cube.service.aigc.knowledge.KnowledgeFramework;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 获取知识文档分段数据任务。
 */
public class GetKnowledgeSegmentsTask extends ServiceTask {

    public GetKnowledgeSegmentsTask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
        super(cellet, talkContext, primitive, responseTime);
    }

    @Override
    public void run() {
        ActionDialect dialect = new ActionDialect(this.primitive);
        Packet packet = new Packet(dialect);

        String token = this.getTokenCode(dialect);
        if (null == token) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.NoToken.code, new JSONObject()));
            markResponseTime();
            return;
        }

        String baseName = KnowledgeFramework.DefaultName;
        if (packet.data.has("base")) {
            baseName = packet.data.getString("base");
        }

        long docId = 0;
        int start = 0;
        int end = 9;
        List<KnowledgeSegment> segments = new ArrayList<>();
        try {
            docId = packet.data.getLong("docId");
            if (packet.data.has("start")) {
                start = packet.data.getInt("start");
            }
            if (packet.data.has("end")) {
                end = packet.data.getInt("end");
            }
        } catch (Exception e){
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.IllegalOperation.code, new JSONObject()));
            markResponseTime();
            return;
        }

        AIGCService service = ((AIGCCellet) this.cellet).getService();

        KnowledgeBase base = service.getKnowledgeBase(token, baseName);
        if (null == base) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.Failure.code, new JSONObject()));
            markResponseTime();
            return;
        }

        int total = base.numKnowledgeSegments(docId);
        List<KnowledgeSegment> list = base.getKnowledgeSegments(docId, start, end);
        JSONArray array = new JSONArray();
        for (KnowledgeSegment segment : list) {
            array.put(segment.toJSON());
        }

        JSONObject responseData = new JSONObject();
        responseData.put("total", total);
        responseData.put("list", array);
        this.cellet.speak(this.talkContext,
                this.makeResponse(dialect, packet, AIGCStateCode.Ok.code, responseData));
        markResponseTime();
    }
}
