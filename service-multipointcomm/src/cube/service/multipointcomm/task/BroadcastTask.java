/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.multipointcomm.task;

import cell.core.cellet.Cellet;
import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.core.talk.dialect.DialectFactory;
import cube.benchmark.ResponseTime;
import cube.common.Packet;
import cube.common.action.MultipointCommAction;
import cube.common.entity.CommFieldEndpoint;
import cube.common.state.MultipointCommStateCode;
import cube.service.ServiceTask;
import cube.service.multipointcomm.MultipointCommService;
import org.json.JSONObject;

/**
 * 请求广播数据任务。
 */
public class BroadcastTask extends ServiceTask {

    public BroadcastTask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
        super(cellet, talkContext, primitive, responseTime);
    }

    @Override
    public void run() {
        ActionDialect action = DialectFactory.getInstance().createActionDialect(this.primitive);
        Packet packet = new Packet(action);

        CommFieldEndpoint source = new CommFieldEndpoint(packet.data.getJSONObject("source"));
        JSONObject data = packet.data.getJSONObject("data");

        MultipointCommService service = (MultipointCommService) this.kernel.getModule(MultipointCommService.NAME);

        MultipointCommStateCode state = service.processBroadcast(source, data);

        this.cellet.speak(this.talkContext,
                this.makeResponse(action, packet, MultipointCommAction.BroadcastAck.name, state.code, data));
        markResponseTime();
    }
}
