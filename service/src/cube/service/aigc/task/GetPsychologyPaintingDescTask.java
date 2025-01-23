/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2020-2025 Ambrose Xu.
 */

package cube.service.aigc.task;

import cell.core.cellet.Cellet;
import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.util.log.Logger;
import cube.aigc.psychology.Painting;
import cube.aigc.psychology.PaintingReport;
import cube.aigc.psychology.algorithm.Attention;
import cube.benchmark.ResponseTime;
import cube.common.Packet;
import cube.common.state.AIGCStateCode;
import cube.service.ServiceTask;
import cube.service.aigc.scene.PsychologyScene;
import org.json.JSONObject;

/**
 * 获取任务。
 */
public class GetPsychologyPaintingDescTask extends ServiceTask {

    public GetPsychologyPaintingDescTask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
        super(cellet, talkContext, primitive, responseTime);
    }

    @Override
    public void run() {
        ActionDialect dialect = new ActionDialect(this.primitive);
        Packet packet = new Packet(dialect);

        String token = this.getTokenCode(dialect);
        if (null == token) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.NoToken.code, new JSONObject()));
            markResponseTime();
            return;
        }

        if (!packet.data.has("sn")) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.InvalidParameter.code, new JSONObject()));
            markResponseTime();
            return;
        }

        try {
            long sn = packet.data.getLong("sn");
            Painting painting = PsychologyScene.getInstance().getPainting(sn);
            if (null == painting) {
                this.cellet.speak(this.talkContext,
                        this.makeResponse(dialect, packet, AIGCStateCode.Failure.code, new JSONObject()));
                markResponseTime();
                return;
            }

            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.Ok.code, painting.toJSON()));
            markResponseTime();
        } catch (Exception e) {
            Logger.e(this.getClass(), "#run", e);

            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.IllegalOperation.code, new JSONObject()));
            markResponseTime();
        }
    }
}
