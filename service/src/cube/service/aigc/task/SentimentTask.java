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
import org.json.JSONObject;

/**
 * 情感分析。
 * @deprecated
 */
public class SentimentTask extends ServiceTask {

    public SentimentTask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
        super(cellet, talkContext, primitive, responseTime);
    }

    @Override
    public void run() {
        ActionDialect dialect = new ActionDialect(this.primitive);
        Packet packet = new Packet(dialect);

        if (!packet.data.has("text")) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.InvalidParameter.code, new JSONObject()));
            markResponseTime();
            return;
        }

        String text = packet.data.getString("text");

        AIGCService service = ((AIGCCellet) this.cellet).getService();

        // 执行 Sentiment Analysis
        /* 废弃 2024-12-13
        boolean success = service.sentimentAnalysis(text, new SentimentAnalysisListener() {
            @Override
            public void onCompleted(SentimentResult result) {
                cellet.speak(talkContext,
                        makeResponse(dialect, packet, AIGCStateCode.Ok.code, result.toJSON()));
                markResponseTime();
            }

            @Override
            public void onFailed() {
                cellet.speak(talkContext,
                        makeResponse(dialect, packet, AIGCStateCode.UnitError.code, new JSONObject()));
                markResponseTime();
            }
        });

        if (!success) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.Failure.code, new JSONObject()));
            markResponseTime();
        }*/
        this.cellet.speak(this.talkContext,
                this.makeResponse(dialect, packet, AIGCStateCode.Failure.code, new JSONObject()));
        markResponseTime();
    }
}
