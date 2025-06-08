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
import cell.util.log.Logger;
import cube.benchmark.ResponseTime;
import cube.common.Packet;
import cube.common.entity.KnowledgeDocument;
import cube.common.entity.ResetKnowledgeProgress;
import cube.common.state.AIGCStateCode;
import cube.service.ServiceTask;
import cube.service.aigc.AIGCCellet;
import cube.service.aigc.AIGCService;
import cube.service.aigc.knowledge.KnowledgeBase;
import cube.service.aigc.knowledge.KnowledgeFramework;
import cube.service.aigc.listener.ResetKnowledgeStoreListener;
import org.json.JSONObject;

import java.util.List;

/**
 * 重置知识库任务。
 */
public class ResetKnowledgeStoreTask extends ServiceTask {

    public ResetKnowledgeStoreTask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
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

        // 是否备份
        boolean backup = packet.data.has("backup") && packet.data.getBoolean("backup");

        ResetKnowledgeProgress progress = base.resetKnowledgeStore(backup, new ResetKnowledgeStoreListener() {
            @Override
            public void onProgress(KnowledgeBase knowledgeBase, ResetKnowledgeProgress progress) {
                Logger.d(ResetKnowledgeStoreTask.class, "#onProgress - " + knowledgeBase.getName() +
                        " - " + knowledgeBase.getAuthToken().getContactId() +
                        " - " + progress.getProgress());
            }

            @Override
            public void onFailed(KnowledgeBase knowledgeBase, ResetKnowledgeProgress progress, AIGCStateCode stateCode) {
                Logger.d(ResetKnowledgeStoreTask.class, "#onFailed - " + knowledgeBase.getName() +
                        " - " + knowledgeBase.getAuthToken().getContactId() +
                        " - " + progress.getProgress() + " - code:" + stateCode.code);
            }

            @Override
            public void onCompleted(KnowledgeBase knowledgeBase, List<KnowledgeDocument> originList, List<KnowledgeDocument> completionList) {
                Logger.d(ResetKnowledgeStoreTask.class, "#onCompleted - " + knowledgeBase.getName() +
                        " - " + knowledgeBase.getAuthToken().getContactId());
            }
        });

        if (null == progress) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.Failure.code, new JSONObject()));
            markResponseTime();
        }
        else {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.Ok.code, progress.toJSON()));
            markResponseTime();
        }
    }
}
