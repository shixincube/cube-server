/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.contact.task;

import cell.core.cellet.Cellet;
import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cube.common.Packet;
import cube.common.state.ContactStateCode;
import cube.service.ServiceTask;
import org.json.JSONObject;

/**
 * 服务未就绪。
 */
public class NotReadyTask extends ServiceTask {

    public NotReadyTask(Cellet cellet, TalkContext talkContext, Primitive primitive) {
        super(cellet, talkContext, primitive);
    }

    @Override
    public void run() {
        ActionDialect action = new ActionDialect(this.primitive);
        Packet packet = new Packet(action);

        this.cellet.speak(this.talkContext,
                this.makeResponse(action, packet, ContactStateCode.Failure.code, new JSONObject()));

    }
}
