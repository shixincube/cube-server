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
import cube.common.entity.KnowledgeProgress;
import cube.common.state.AIGCStateCode;
import cube.service.ServiceTask;
import cube.service.aigc.AIGCCellet;
import cube.service.aigc.AIGCService;
import cube.service.aigc.knowledge.KnowledgeBase;
import cube.service.aigc.knowledge.KnowledgeFramework;
import cube.service.aigc.listener.KnowledgeProgressListener;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 导入知识库文档任务。
 */
public class ImportKnowledgeDocTask extends ServiceTask {

    public ImportKnowledgeDocTask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
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

        String fileCode = null;
        List<String> fileCodeList = null;

        if (packet.data.has("fileCode")) {
            fileCode = packet.data.getString("fileCode");
        }
        else if (packet.data.has("fileCodeList")) {
            fileCodeList = new ArrayList<>();
            JSONArray array = packet.data.getJSONArray("fileCodeList");
            for (int i = 0; i < array.length(); ++i) {
                fileCodeList.add(array.getString(i));
            }
        }

        if (null == fileCode && null == fileCodeList) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.InvalidParameter.code, new JSONObject()));
            markResponseTime();
            return;
        }

        String baseName = KnowledgeFramework.DefaultName;
        if (packet.data.has("base")) {
            baseName = packet.data.getString("base");
        }

        String splitter = KnowledgeDoc.SPLITTER_AUTO;
        if (packet.data.has("splitter")) {
            splitter = packet.data.getString("splitter");
        }

        AIGCService service = ((AIGCCellet) this.cellet).getService();
        KnowledgeBase base = service.getKnowledgeBase(tokenCode, baseName);
        if (null == base) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.InconsistentToken.code, new JSONObject()));
            markResponseTime();
            return;
        }

        if (null != fileCode) {
            KnowledgeDoc doc = base.importKnowledgeDoc(fileCode, splitter);
            if (null == doc) {
                this.cellet.speak(this.talkContext,
                        this.makeResponse(dialect, packet, AIGCStateCode.Failure.code, new JSONObject()));
                markResponseTime();
                return;
            }

            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.Ok.code, doc.toJSON()));
            markResponseTime();
        }
        else {
            KnowledgeProgress progress = base.batchImportKnowledgeDocuments(fileCodeList, splitter,
                    new KnowledgeProgressListener() {
                @Override
                public void onProgress(KnowledgeBase knowledgeBase, KnowledgeProgress progress) {
                    // Nothing
                }

                @Override
                public void onFailed(KnowledgeBase knowledgeBase, KnowledgeProgress progress) {
                    // Nothing
                }

                @Override
                public void onCompleted(KnowledgeBase knowledgeBase, KnowledgeProgress progress) {
                    // Nothing
                }
            });

            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.Ok.code, progress.toJSON()));
            markResponseTime();
        }
    }
}
