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
import cube.common.entity.SpeechEmotion;
import cube.common.state.AIGCStateCode;
import cube.service.ServiceTask;
import cube.service.aigc.AIGCCellet;
import cube.service.aigc.AIGCService;
import cube.service.aigc.listener.SpeechEmotionRecognitionListener;

/**
 * 语音识别情绪。
 */
public class SpeechEmotionRecognitionTask extends ServiceTask {

    public SpeechEmotionRecognitionTask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
        super(cellet, talkContext, primitive, responseTime);
    }

    @Override
    public void run() {
        ActionDialect dialect = new ActionDialect(this.primitive);
        Packet packet = new Packet(dialect);

        String token = getTokenCode(dialect);
        if (null == token || !packet.data.has("fileCode")) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.InvalidParameter.code, packet.data));
            markResponseTime();
            return;
        }

        AIGCService service = ((AIGCCellet) this.cellet).getService();
        AuthToken authToken = service.getToken(token);

        // 执行 Speech Emotion Recognition
        boolean success = service.speechEmotionRecognition(authToken,
                packet.data.getString("fileCode"), new SpeechEmotionRecognitionListener() {
            @Override
            public void onCompleted(FileLabel input, SpeechEmotion result) {
                cellet.speak(talkContext,
                        makeResponse(dialect, packet, AIGCStateCode.Ok.code, result.toJSON()));
                markResponseTime();
            }

            @Override
            public void onFailed(FileLabel input, AIGCStateCode stateCode) {
                cellet.speak(talkContext,
                        makeResponse(dialect, packet, stateCode.code, packet.data));
                markResponseTime();
            }
        });

        if (!success) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.IllegalOperation.code, packet.data));
            markResponseTime();
        }
    }
}
