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
import cube.common.state.AIGCStateCode;
import cube.service.ServiceTask;
import cube.service.aigc.AIGCCellet;
import cube.service.aigc.AIGCService;
import org.json.JSONObject;

/**
 * 语音分析任务。
 */
public class SpeechAnalysisTask extends ServiceTask {

    public SpeechAnalysisTask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
        super(cellet, talkContext, primitive, responseTime);
    }

    @Override
    public void run() {
        ActionDialect dialect = new ActionDialect(this.primitive);
        Packet packet = new Packet(dialect);

        String token = this.getTokenCode(dialect);
        if (null == token || !packet.data.has("fileCode") || !packet.data.has("templateName")) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.InvalidParameter.code, packet.data));
            markResponseTime();
            return;
        }

        AIGCService service = ((AIGCCellet) this.cellet).getService();
        AuthToken authToken = service.getToken(token);
        String fileCode = packet.data.getString("fileCode");
        String templateName = packet.data.getString("templateName");

        // 执行 Speech Analysis
        String result = service.performSpeechAnalysis(authToken, fileCode, templateName);
        if (null != result) {
            JSONObject resultJson = new JSONObject();
            resultJson.put("fileCode", fileCode);
            resultJson.put("templateName", templateName);
            resultJson.put("analysis", result);
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.Ok.code, resultJson));
            markResponseTime();
        }
        else {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.Failure.code, packet.data));
            markResponseTime();
        }
    }
}
