/**
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Shixin Cube Team.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package cube.dispatcher;


import cell.core.cellet.Cellet;
import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.core.talk.dialect.DialectFactory;
import cell.util.json.JSONObject;
import cube.common.Packet;
import cube.common.StateCode;
import cube.common.Task;

/**
 * 调度机线程任务抽象。
 */
public abstract class DispatcherTask extends Task {

    protected Performer performer;

    private Packet request;

    private ActionDialect action;

    public DispatcherTask(Cellet cellet, TalkContext talkContext, Primitive primitive, Performer performer) {
        super(cellet, talkContext, primitive);
        this.performer = performer;

        this.action = DialectFactory.getInstance().createActionDialect(this.primitive);
        this.request = new Packet(this.action);
    }

    public void reset(TalkContext talkContext, Primitive primitive) {
        this.talkContext = talkContext;
        this.primitive = primitive;

        this.action = DialectFactory.getInstance().createActionDialect(this.primitive);
        this.request = new Packet(this.action);
    }

    public ActionDialect getAction() {
        return this.action;
    }

    public Packet getRequest() {
        return this.request;
    }

    protected ActionDialect makeResponse(ActionDialect response) {
        response.addParam("state", StateCode.makeState(StateCode.OK, "OK"));
        return response;
    }

    protected ActionDialect makeResponse(JSONObject data, int stateCode, String desc) {
        Packet packet = new Packet(this.request.sn, this.request.name, data);
        ActionDialect response = packet.toDialect();
        response.addParam("state", StateCode.makeState(stateCode, desc));
        return response;
    }

    protected ActionDialect makeGatewayErrorResponse(Packet response) {
        ActionDialect result = response.toDialect();
        result.addParam("state", StateCode.makeState(StateCode.GatewayError, "Gateway error"));
        return result;
    }

    /**
     * 标记状态码。
     *
     * @param response
     * @return
     */
    public static ActionDialect appendState(ActionDialect response) {
        response.addParam("state", StateCode.makeState(StateCode.OK, "OK"));
        return response;
    }
}
