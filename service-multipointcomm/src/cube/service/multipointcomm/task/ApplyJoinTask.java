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
import cell.util.log.Logger;
import cube.benchmark.ResponseTime;
import cube.common.Packet;
import cube.common.entity.CommField;
import cube.common.entity.Contact;
import cube.common.entity.Device;
import cube.common.state.MultipointCommStateCode;
import cube.service.ServiceTask;
import cube.service.multipointcomm.MultipointCommService;
import org.json.JSONException;

/**
 * Apply Join 任务。
 */
public class ApplyJoinTask extends ServiceTask {

    public ApplyJoinTask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
        super(cellet, talkContext, primitive, responseTime);
    }

    @Override
    public void run() {
        ActionDialect action = DialectFactory.getInstance().createActionDialect(this.primitive);
        Packet packet = new Packet(action);

        CommField field = null;
        Contact participant = null;
        Device device = null;

        try {
            field = new CommField(packet.data.getJSONObject("field"));
            participant = new Contact(packet.data.getJSONObject("participant"));
            device = new Device(packet.data.getJSONObject("device"));
        } catch (JSONException e) {
            Logger.w(this.getClass(), "#run", e);
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, MultipointCommStateCode.InvalidParameter.code, packet.data));
            markResponseTime();
            return;
        }

        MultipointCommService service = (MultipointCommService) this.kernel.getModule(MultipointCommService.NAME);

        // 申请进入 Comm Field
        MultipointCommStateCode state = service.applyJoin(field, participant, device);
        if (state != MultipointCommStateCode.Ok) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, state.code, packet.data));
            markResponseTime();
            return;
        }

        // 获取场域数据
        CommField current = service.getCommField(field.getId());

        this.cellet.speak(this.talkContext,
                this.makeResponse(action, packet, state.code, current.toJSON()));
        markResponseTime();
    }
}
