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
import cube.auth.AuthToken;
import cube.benchmark.ResponseTime;
import cube.common.Packet;
import cube.common.entity.ComplexContext;
import cube.common.state.AIGCStateCode;
import cube.service.ServiceTask;
import cube.service.aigc.Explorer;
import org.json.JSONObject;

/**
 * 获取上下文的推理内容。
 */
public class GetContextInferenceTask extends ServiceTask {

    public GetContextInferenceTask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
        super(cellet, talkContext, primitive, responseTime);
    }

    @Override
    public void run() {
        ActionDialect dialect = new ActionDialect(this.primitive);
        Packet packet = new Packet(dialect);

        AuthToken authToken = extractAuthToken(dialect);
        if (null == authToken) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.InvalidParameter.code, new JSONObject()));
            markResponseTime();
            return;
        }

        if (!packet.data.has("id")) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.InvalidParameter.code, new JSONObject()));
            markResponseTime();
            return;
        }

        long contextId = packet.data.getLong("id");

        ComplexContext context = Explorer.getInstance().getComplexContext(contextId);
        if (null == context) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.Failure.code, new JSONObject()));
            markResponseTime();
            return;
        }

        JSONObject responsePayload = context.toJSON();
        this.cellet.speak(this.talkContext,
                this.makeResponse(dialect, packet, AIGCStateCode.Ok.code, responsePayload));
        markResponseTime();

//        if (!context.isInferable() || context.isInferring()) {
//            JSONObject responsePayload = new JSONObject();
//            responsePayload.put("inferable", context.isInferable());
//            responsePayload.put("inferring", context.isInferring());
//            responsePayload.put("inferenceResult", new JSONArray());
//            this.cellet.speak(this.talkContext,
//                    this.makeResponse(dialect, packet, AIGCStateCode.Ok.code, responsePayload));
//            markResponseTime();
//            return;
//        }
//        JSONObject responsePayload = new JSONObject();
//        responsePayload.put("inferable", context.isInferable());
//        responsePayload.put("inferring", context.isInferring());
//        JSONArray inferenceResult = new JSONArray();
//        for (String text : context.getInferenceResult()) {
//            inferenceResult.put(text);
//        }
//        responsePayload.put("inferenceResult", inferenceResult);
    }
}
