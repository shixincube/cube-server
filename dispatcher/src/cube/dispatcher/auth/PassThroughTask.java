/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.auth;

import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cube.common.Packet;
import cube.dispatcher.DispatcherTask;
import cube.dispatcher.Performer;
import org.json.JSONObject;

/**
 * 透传数据给服务层。
 */
public class PassThroughTask extends DispatcherTask {

    private boolean waitResponse = true;

    public PassThroughTask(AuthCellet cellet, TalkContext talkContext, Primitive primitive
            , Performer performer) {
        super(cellet, talkContext, primitive, performer);
    }

    public PassThroughTask(AuthCellet cellet, TalkContext talkContext, Primitive primitive
            , Performer performer, boolean sync) {
        super(cellet, talkContext, primitive, performer);
        this.waitResponse = sync;
    }

    @Override
    public void run() {
        if (this.waitResponse) {
            ActionDialect response = this.performer.syncTransmit(this.talkContext, this.cellet.getName(), this.getAction());

            if (null == response) {
                Packet request = this.getRequest();
                // 发生错误
                Packet packet = new Packet(request.sn, request.name, new JSONObject());
                response = this.makeGatewayErrorResponse(packet);
            }
            else {
                response = this.makeResponse(response);
            }

            this.cellet.speak(this.talkContext, response);
        }
        else {
            this.performer.transmit(this.talkContext, this.cellet, this.getAction());
        }

        ((AuthCellet) this.cellet).returnTask(this);
    }
}
