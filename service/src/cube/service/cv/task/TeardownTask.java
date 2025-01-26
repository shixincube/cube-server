/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.cv.task;

import cell.core.cellet.Cellet;
import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cube.benchmark.ResponseTime;
import cube.common.Packet;
import cube.common.entity.Contact;
import cube.common.state.CVStateCode;
import cube.service.ServiceTask;
import cube.service.cv.CVCellet;
import cube.service.cv.CVService;

/**
 * 释放 CV 服务。
 */
public class TeardownTask extends ServiceTask {

    public TeardownTask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
        super(cellet, talkContext, primitive, responseTime);
    }

    @Override
    public void run() {
        ActionDialect dialect = new ActionDialect(this.primitive);
        Packet packet = new Packet(dialect);

        Contact contact = new Contact(packet.data.getJSONObject("contact"));

        CVService service = ((CVCellet) this.cellet).getService();
        service.teardown(contact, this.talkContext);

        this.cellet.speak(this.talkContext,
                this.makeResponse(dialect, packet, CVStateCode.Ok.code, contact.toCompactJSON()));
        markResponseTime();
    }
}
