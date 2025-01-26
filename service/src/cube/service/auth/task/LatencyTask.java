/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.auth.task;

import cell.core.cellet.Cellet;
import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.core.talk.dialect.DialectFactory;
import cell.util.log.Logger;
import cube.benchmark.ResponseTime;
import cube.common.Packet;
import cube.common.state.AuthStateCode;
import cube.service.ServiceTask;
import org.json.JSONObject;

public class LatencyTask extends ServiceTask {

    public LatencyTask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
        super(cellet, talkContext, primitive, responseTime);
    }

    @Override
    public void run() {
        ActionDialect action = DialectFactory.getInstance().createActionDialect(this.primitive);
        Packet packet = new Packet(action);

        long now = System.currentTimeMillis();
        Logger.d(this.getClass(), "Latency : " + (now - packet.data.getLong("launch")) + "ms");

        JSONObject payload = new JSONObject();
        payload.put("launch", packet.data.getLong("launch"));
        payload.put("time", now);

        this.cellet.speak(this.talkContext,
                this.makeResponse(action, packet, AuthStateCode.Ok.code, payload));
        markResponseTime();
    }
}
