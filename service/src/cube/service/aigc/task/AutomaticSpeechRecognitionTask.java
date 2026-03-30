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
import cube.common.entity.FileLabel;
import cube.common.entity.SpeechRecognitionInfo;
import cube.common.state.AIGCStateCode;
import cube.service.ServiceTask;
import cube.service.aigc.AIGCCellet;
import cube.service.aigc.AIGCService;
import cube.service.aigc.listener.AutomaticSpeechRecognitionListener;
import org.json.JSONObject;

/**
 * 自动语音识别。
 */
public class AutomaticSpeechRecognitionTask extends ServiceTask {

    public AutomaticSpeechRecognitionTask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
        super(cellet, talkContext, primitive, responseTime);
    }

    @Override
    public void run() {
        ActionDialect dialect = new ActionDialect(this.primitive);
        Packet packet = new Packet(dialect);

        String token = this.getTokenCode(dialect);
        if (null == token) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.InvalidParameter.code, packet.data));
            markResponseTime();
            return;
        }

        AIGCService service = ((AIGCCellet) this.cellet).getService();
        AuthToken authToken = service.getToken(token);

        try {
            String fileCodeOrUrl = packet.data.has("fileCode") ? packet.data.getString("fileCode") :
                    packet.data.getString("fileUrl");

            // 执行 Automatic Speech Recognition
            boolean success = service.automaticSpeechRecognition(authToken, fileCodeOrUrl, new AutomaticSpeechRecognitionListener() {
                @Override
                public void onCompleted(FileLabel source, SpeechRecognitionInfo result) {
                    cellet.speak(talkContext,
                            makeResponse(dialect, packet, AIGCStateCode.Ok.code, result.toJSON()));
                    markResponseTime();
                }

                @Override
                public void onFailed(FileLabel source, AIGCStateCode stateCode) {
                    cellet.speak(talkContext,
                            makeResponse(dialect, packet, stateCode.code, packet.data));
                    markResponseTime();
                }
            });

            if (!success) {
                this.cellet.speak(this.talkContext,
                        this.makeResponse(dialect, packet, AIGCStateCode.Failure.code, packet.data));
                markResponseTime();
            }
        } catch (Exception e) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.InvalidParameter.code, new JSONObject()));
            markResponseTime();
        }
    }
}
