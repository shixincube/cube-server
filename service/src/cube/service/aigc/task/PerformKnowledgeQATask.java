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
import cube.aigc.ModelConfig;
import cube.benchmark.ResponseTime;
import cube.common.Packet;
import cube.common.entity.AIGCChannel;
import cube.common.entity.KnowledgeMatchingSchema;
import cube.common.entity.KnowledgeQAProgress;
import cube.common.entity.KnowledgeQAResult;
import cube.common.state.AIGCStateCode;
import cube.service.ServiceTask;
import cube.service.aigc.AIGCCellet;
import cube.service.aigc.AIGCService;
import cube.service.aigc.knowledge.KnowledgeBase;
import cube.service.aigc.knowledge.KnowledgeFramework;
import cube.service.aigc.listener.KnowledgeQAListener;
import org.json.JSONObject;

/**
 * 执行知识库文档问答任务。
 */
public class PerformKnowledgeQATask extends ServiceTask {

    public PerformKnowledgeQATask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
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

        String unitName = null;
        KnowledgeMatchingSchema matchingSchema = null;
        try {
            unitName = packet.data.has("unit") ? packet.data.getString("unit") : ModelConfig.CHAT_UNIT;
            matchingSchema = new KnowledgeMatchingSchema(packet.data.getJSONObject("matchingSchema"));
        } catch (Exception e) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.InvalidParameter.code, new JSONObject()));
            markResponseTime();
            return;
        }

        AIGCService service = ((AIGCCellet) this.cellet).getService();

        AIGCChannel channel = service.getChannelByToken(tokenCode);
        if (null == channel) {
            channel = service.requestChannel(tokenCode, "Unknown");
            if (null == channel) {
                // 申请频道失败
                this.cellet.speak(this.talkContext,
                        this.makeResponse(dialect, packet, AIGCStateCode.InconsistentToken.code, new JSONObject()));
                markResponseTime();
                return;
            }
        }

        String baseName = KnowledgeFramework.DefaultName;
        if (packet.data.has("base")) {
            baseName = packet.data.getString("base");
        }

        KnowledgeBase base = service.getKnowledgeBase(tokenCode, baseName);
        if (null == base) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.IllegalOperation.code, new JSONObject()));
            markResponseTime();
            return;
        }

        // 执行知识库问答
        KnowledgeQAProgress progress = base.performKnowledgeQA(channel.getCode(), unitName, matchingSchema,
                new KnowledgeQAListener() {
                    @Override
                    public void onCompleted(AIGCChannel channel, KnowledgeQAResult result) {
                        // Nothing
                    }

                    @Override
                    public void onFailed(AIGCChannel channel, AIGCStateCode stateCode) {
                        // Nothing
                    }
                });

        if (null != progress) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.Ok.code, progress.toJSON()));
            markResponseTime();
        }
        else {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.Failure.code, packet.data));
            markResponseTime();
        }
    }
}
