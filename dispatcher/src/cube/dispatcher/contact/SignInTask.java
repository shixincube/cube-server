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

package cube.dispatcher.contact;

import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.core.talk.dialect.DialectFactory;
import cell.util.json.JSONException;
import cell.util.json.JSONObject;
import cube.common.Packet;
import cube.common.StateCode;
import cube.common.Task;
import cube.common.entity.Contact;
import cube.dispatcher.Performer;

/**
 * 设置自己联系人信息任务。
 */
public class SignInTask extends Task {

    private Performer performer;

    public SignInTask(ContactCellet cellet, TalkContext talkContext, Primitive primitive, Performer performer) {
        super(cellet, talkContext, primitive);
        this.performer = performer;
    }

    protected void reset(TalkContext talkContext, Primitive primitive) {
        this.talkContext = talkContext;
        this.primitive = primitive;
    }

    @Override
    public void run() {
        ActionDialect actionDialect = DialectFactory.getInstance().createActionDialect(this.primitive);
        Packet packet = new Packet(actionDialect);

        JSONObject selfJson = null;
        JSONObject authTokenJson = null;
        try {
            selfJson = packet.data.getJSONObject("self");
            if (packet.data.has("token")) {
                authTokenJson = packet.data.getJSONObject("token");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (null == authTokenJson) {
            // 没有令牌信息
            Packet responsePacket = new Packet(packet.sn, packet.name, this.makeStatePayload(StateCode.InvalidParameter, "No token info"));
            this.cellet.speak(this.talkContext, responsePacket.toDialect());
            return;
        }

        // 将当前联系人与会话上下问关联
        Contact self = new Contact(selfJson, this.talkContext);
        this.performer.addContact(self);

        ActionDialect response = this.performer.syncTransmit(this.talkContext, this.cellet.getName(), actionDialect);
        if (null == response) {
            // 发生错误
            Packet responsePacket = new Packet(packet.sn, packet.name, this.makeGatewayErrorPayload());
            response = responsePacket.toDialect();
        }

        this.cellet.speak(this.talkContext, response);

        ((ContactCellet)this.cellet).returnSignInTask(this);
    }

    private JSONObject makeGatewayErrorPayload() {
        JSONObject payload = new JSONObject();
        try {
            payload.put("state", StateCode.makeState(StateCode.GatewayError, "Gateway error"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return payload;
    }
}
