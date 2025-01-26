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
import cube.common.entity.KnowledgeDoc;
import cube.common.state.AIGCStateCode;
import cube.service.ServiceTask;
import cube.service.aigc.AIGCCellet;
import cube.service.aigc.AIGCService;
import cube.service.aigc.knowledge.KnowledgeBase;
import cube.service.aigc.knowledge.KnowledgeFramework;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * 获取知识库文档列表任务。
 */
public class ListKnowledgeDocsTask extends ServiceTask {

    public ListKnowledgeDocsTask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
        super(cellet, talkContext, primitive, responseTime);
    }

    @Override
    public void run() {
        ActionDialect dialect = new ActionDialect(this.primitive);
        Packet packet = new Packet(dialect);

        String tokenCode = this.getTokenCode(dialect);
        if (null == tokenCode) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.InvalidParameter.code, new JSONObject()));
            markResponseTime();
            return;
        }

        String baseName = KnowledgeFramework.DefaultName;
        if (packet.data.has("base")) {
            baseName = packet.data.getString("base");
        }

        AIGCService service = ((AIGCCellet) this.cellet).getService();
        KnowledgeBase base = service.getKnowledgeBase(tokenCode, baseName);
        if (null == base) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.InconsistentToken.code, new JSONObject()));
            markResponseTime();
            return;
        }

        List<KnowledgeDoc> docList = base.listKnowledgeDocs();
        if (null == docList) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.Failure.code, new JSONObject()));
            markResponseTime();
            return;
        }

        JSONObject responsePayload = new JSONObject();

        int size = 10;
        if (docList.size() > size) {
            int page = packet.data.has("page") ? packet.data.getInt("page") : 0;
            JSONArray array = new JSONArray();
            for (int i = page * size; i < docList.size(); ++i) {
                array.put(docList.get(i).toJSON());
                if (array.length() >= size) {
                    break;
                }
            }

            responsePayload.put("page", page);
            responsePayload.put("total", docList.size());
            responsePayload.put("list", array);
        }
        else {
            JSONArray array = new JSONArray();
            for (KnowledgeDoc doc : docList) {
                array.put(doc.toJSON());
            }

            responsePayload.put("total", docList.size());
            responsePayload.put("list", array);
        }

        this.cellet.speak(this.talkContext,
                this.makeResponse(dialect, packet, AIGCStateCode.Ok.code, responsePayload));
        markResponseTime();
    }
}
