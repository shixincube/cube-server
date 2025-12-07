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
import cube.common.entity.AudioStreamSink;
import cube.common.entity.FileLabel;
import cube.common.entity.VoiceDiarization;
import cube.common.state.AIGCStateCode;
import cube.service.ServiceTask;
import cube.service.aigc.AIGCCellet;
import cube.service.aigc.AIGCService;
import cube.service.aigc.listener.AudioStreamAnalysisListener;

/**
 * 分析音频流任务。
 */
public class AnalyseAudioStreamTask extends ServiceTask {

    public AnalyseAudioStreamTask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
        super(cellet, talkContext, primitive, responseTime);
    }

    @Override
    public void run() {
        ActionDialect dialect = new ActionDialect(this.primitive);
        Packet packet = new Packet(dialect);

        String token = this.getTokenCode(dialect);
        if (null == token || !packet.data.has("fileCode") || !packet.data.has("streamName") ||
            !packet.data.has("index")) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.InvalidParameter.code, packet.data));
            markResponseTime();
            return;
        }

        AIGCService service = ((AIGCCellet) this.cellet).getService();
        AuthToken authToken = service.getToken(token);
        String fileCode = packet.data.getString("fileCode");
        String streamName = packet.data.getString("streamName");
        int index = packet.data.getInt("index");

        boolean success = service.analyseAudioStream(authToken, fileCode, streamName, index, new AudioStreamAnalysisListener() {
            @Override
            public void onCompleted(FileLabel source, AudioStreamSink streamSink) {
                cellet.speak(talkContext,
                        makeResponse(dialect, packet, AIGCStateCode.Ok.code, streamSink.toJSON()));
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
    }
}
