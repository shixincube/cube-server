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
import cube.common.entity.ASRResult;
import cube.common.entity.FileLabel;
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

        if (!packet.data.has("domain") || !packet.data.has("fileCode")) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.InvalidParameter.code, new JSONObject()));
            markResponseTime();
            return;
        }

        String domain = packet.data.getString("domain");
        String fileCode = packet.data.getString("fileCode");

        AIGCService service = ((AIGCCellet) this.cellet).getService();

        // 执行 Automatic Speech Recognition
        boolean success = service.automaticSpeechRecognition(domain, fileCode, new AutomaticSpeechRecognitionListener() {
            @Override
            public void onCompleted(FileLabel input, ASRResult result) {
                cellet.speak(talkContext,
                        makeResponse(dialect, packet, AIGCStateCode.Ok.code, result.toJSON()));
                markResponseTime();
            }

            @Override
            public void onFailed(FileLabel source) {
                JSONObject data = new JSONObject();
                data.put("fileCode", source.getFileCode());
                cellet.speak(talkContext,
                        makeResponse(dialect, packet, AIGCStateCode.UnitError.code, data));
                markResponseTime();
            }
        });

        if (!success) {
            JSONObject data = new JSONObject();
            data.put("fileCode", fileCode);
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.Failure.code, data));
            markResponseTime();
        }
    }
}
