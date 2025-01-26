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
import cube.common.state.MultipointCommStateCode;
import cube.service.ServiceTask;
import cube.service.multipointcomm.MultipointCommService;

/**
 * 获取 CommField 数据任务。
 */
public class GetFieldTask extends ServiceTask {

    public GetFieldTask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
        super(cellet, talkContext, primitive, responseTime);
    }

    @Override
    public void run() {
        ActionDialect action = DialectFactory.getInstance().createActionDialect(this.primitive);
        Packet packet = new Packet(action);

        Long commFieldId = null;

        try {
            commFieldId = packet.data.getLong("commFieldId");
        } catch (Exception e) {
            Logger.w(this.getClass(), "#run", e);
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, MultipointCommStateCode.InvalidParameter.code, packet.data));
            markResponseTime();
            return;
        }

        MultipointCommService service = (MultipointCommService) this.kernel.getModule(MultipointCommService.NAME);

        // 获取场域
        CommField commField = service.getCommField(commFieldId);

        if (null == commField) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, MultipointCommStateCode.NoCommField.code, packet.data));
            markResponseTime();
            return;
        }

        this.cellet.speak(this.talkContext,
                this.makeResponse(action, packet, MultipointCommStateCode.Ok.code, commField.toJSON()));
        markResponseTime();
    }
}
