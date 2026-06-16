/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2026 Ambrose Xu.
 */

package cube.service.ferry.task;

import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.util.log.Logger;
import cube.benchmark.ResponseTime;
import cube.common.Packet;
import cube.ferry.FerryStateCode;
import cube.ferry.GnosisAgent;
import cube.service.ServiceTask;
import cube.service.ferry.FerryCellet;
import cube.service.ferry.FerryService;
import org.json.JSONObject;

/**
 * 灵知代理任务。
 */
public class GnosisAgentTask extends ServiceTask {

    public GnosisAgentTask(FerryCellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
        super(cellet, talkContext, primitive, responseTime);
    }

    @Override
    public void run() {
        ActionDialect action = new ActionDialect(this.primitive);
        Packet packet = new Packet(action);

        JSONObject data = packet.data;

        String tokenCode = this.getTokenCode(action);
        if (null == tokenCode) {
            Logger.w(this.getClass(), "No token parameter");

            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, FerryStateCode.InvalidToken.code, data));
            markResponseTime();
            return;
        }

        FerryService service = ((FerryCellet) this.cellet).getFerryService();

        GnosisAgent agent = new GnosisAgent(data);
        FerryService.GnosisAgentAckBundle bundle = service.callGnosisAgent(agent);
        if (null == bundle) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, FerryStateCode.NoGnosisAgent.code, data));
            markResponseTime();
            return;
        }

        if (FerryStateCode.Ok.code != bundle.response.getParamAsInt("code")) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, bundle.response.getParamAsInt("code"), data));
            markResponseTime();
            return;
        }

        JSONObject result = bundle.response.getParamAsJson("result");
        this.cellet.speak(this.talkContext,
                this.makeResponse(action, packet, FerryStateCode.Ok.code, result));
        markResponseTime();
    }
}
