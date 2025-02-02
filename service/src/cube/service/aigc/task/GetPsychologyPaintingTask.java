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
import cube.aigc.psychology.Painting;
import cube.auth.AuthToken;
import cube.benchmark.ResponseTime;
import cube.common.Packet;
import cube.common.entity.FileLabel;
import cube.common.state.AIGCStateCode;
import cube.service.ServiceTask;
import cube.service.aigc.AIGCCellet;
import cube.service.aigc.AIGCService;
import cube.service.aigc.scene.PsychologyScene;
import org.json.JSONObject;

/**
 * 获取心理学量表任务。
 */
public class GetPsychologyPaintingTask extends ServiceTask {

    public GetPsychologyPaintingTask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
        super(cellet, talkContext, primitive, responseTime);
    }

    @Override
    public void run() {
        ActionDialect dialect = new ActionDialect(this.primitive);
        Packet packet = new Packet(dialect);

        String token = getTokenCode(dialect);
        if (null == token) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.NoToken.code, new JSONObject()));
            markResponseTime();
            return;
        }

        AIGCService service = ((AIGCCellet) this.cellet).getService();
        AuthToken authToken = service.getToken(token);
        if (null == authToken) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.IllegalOperation.code, new JSONObject()));
            markResponseTime();
            return;
        }

        try {
            long sn = packet.data.has("sn") ? packet.data.getLong("sn") : 0;
            boolean chart = packet.data.has("chart") ? packet.data.getBoolean("chart") : false;
            boolean bbox = packet.data.has("bbox") ? packet.data.getBoolean("bbox") : true;
            boolean vparam = packet.data.has("vparam") ? packet.data.getBoolean("vparam") : false;
            double prob = packet.data.has("prob") ? packet.data.getDouble("prob") : 0.5d;

            if (chart) {
                JSONObject data = PsychologyScene.getInstance().getPaintingInferenceData(authToken, sn);
                if (null != data) {
                    this.cellet.speak(this.talkContext,
                            this.makeResponse(dialect, packet, AIGCStateCode.Ok.code, data));
                    markResponseTime();
                }
                else {
                    this.cellet.speak(this.talkContext,
                            this.makeResponse(dialect, packet, AIGCStateCode.Failure.code, packet.data));
                    markResponseTime();
                }
            }
            else {
                FileLabel fileLabel = PsychologyScene.getInstance().getPredictedPainting(authToken, sn, bbox, vparam, prob);
                if (null != fileLabel) {
                    this.cellet.speak(this.talkContext,
                            this.makeResponse(dialect, packet, AIGCStateCode.Ok.code, fileLabel.toCompactJSON()));
                    markResponseTime();
                }
                else {
                    this.cellet.speak(this.talkContext,
                            this.makeResponse(dialect, packet, AIGCStateCode.Failure.code, packet.data));
                    markResponseTime();
                }
            }
        } catch (Exception e) {
            Logger.w(this.getClass(), "#run", e);
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.InvalidParameter.code, new JSONObject()));
            markResponseTime();
        }
    }
}
