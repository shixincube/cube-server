/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 Cube Team.
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

package cube.service.contact.task;

import cell.core.cellet.Cellet;
import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.core.talk.dialect.DialectFactory;
import cell.util.log.Logger;
import cube.auth.AuthToken;
import cube.benchmark.ResponseTime;
import cube.common.Packet;
import cube.common.entity.Contact;
import cube.common.entity.Device;
import cube.common.state.ContactStateCode;
import cube.service.ServiceTask;
import cube.service.contact.ContactManager;
import cube.util.DummyDevice;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 联系人签入。
 */
public class SignInTask extends ServiceTask {

    public SignInTask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
        super(cellet, talkContext, primitive, responseTime);
    }

    @Override
    public void run() {
        ActionDialect action = DialectFactory.getInstance().createActionDialect(this.primitive);
        Packet packet = new Packet(action);

        if (packet.data.has("code")) {
            Logger.i(this.getClass(), "SignIn with code");

            // 获取令牌码
            String tokenCode = packet.data.getString("code");

            Device device = null;
            if (packet.data.has("device")) {
                device = new Device(packet.data.getJSONObject("device"), this.talkContext);
            }
            else {
                device = new DummyDevice(this.talkContext);
            }

            // 签入联系人
            Contact contact = ContactManager.getInstance().signIn(tokenCode, device);

            // 应答
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, ContactStateCode.Ok.code, contact.toJSON(device)));
        }
        else {
            Logger.i(this.getClass(), "SignIn with self");

            JSONObject contactJson = null;
            JSONObject authTokenJson = null;
            try {
                contactJson = packet.data.getJSONObject("self");
                authTokenJson = packet.data.getJSONObject("token");
            } catch (JSONException e) {
                Logger.w(this.getClass(), "#run", e);
                this.cellet.speak(this.talkContext,
                        this.makeResponse(action, packet, ContactStateCode.InvalidParameter.code, packet.data));
                markResponseTime();
                return;
            }

            // 校验 Token
            AuthToken authToken = new AuthToken(authTokenJson);

            String tokenCode = this.getTokenCode(action);
            if (!tokenCode.equals(authToken.getCode())) {
                this.cellet.speak(this.talkContext,
                        this.makeResponse(action, packet, ContactStateCode.InconsistentToken.code, packet.data));
                markResponseTime();
                return;
            }

            // 创建联系人对象
            Contact contact = new Contact(contactJson, this.talkContext);

            // 活跃设备
            Device activeDevice = contact.getDevice(this.talkContext);
            if (null == activeDevice) {
                this.cellet.speak(this.talkContext,
                        this.makeResponse(action, packet, ContactStateCode.Failure.code, packet.data));
                markResponseTime();
                return;
            }

            // 设置终端的对应关系
            Contact newContact = ContactManager.getInstance().signIn(contact, authToken, activeDevice);
            if (null == newContact) {
                Logger.w(this.getClass(), "SignIn contact error, please chek auth token: " + tokenCode);

                this.cellet.speak(this.talkContext,
                        this.makeResponse(action, packet, ContactStateCode.IllegalOperation.code, packet.data));
                markResponseTime();
                return;
            }

            // 应答
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, ContactStateCode.Ok.code, newContact.toJSON()));
        }

        markResponseTime();
    }
}
