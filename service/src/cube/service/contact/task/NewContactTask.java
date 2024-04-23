/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 Ambrose Xu.
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
import cell.util.Utils;
import cell.util.log.Logger;
import cube.auth.AuthToken;
import cube.benchmark.ResponseTime;
import cube.common.Packet;
import cube.common.entity.Contact;
import cube.common.state.ContactStateCode;
import cube.service.ServiceTask;
import cube.service.contact.ContactManager;
import org.json.JSONObject;

/**
 * 新建联系人任务。
 */
public class NewContactTask extends ServiceTask {

    public NewContactTask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
        super(cellet, talkContext, primitive, responseTime);
    }

    @Override
    public void run() {
        ActionDialect action = new ActionDialect(this.primitive);
        Packet packet = new Packet(action);

        JSONObject data = packet.data;

        String tokenCode = this.getTokenCode(action);
        if (null == tokenCode) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, ContactStateCode.InvalidParameter.code, data));
            markResponseTime();
            return;
        }

        // 校验 Token
        AuthToken authToken = ContactManager.getInstance().getAuthService().getToken(tokenCode);
        if (null == authToken) {
            // 无效的 token
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, ContactStateCode.NoSignIn.code, data));
            markResponseTime();
            return;
        }

        long newId = 0;
        String newName = null;
        JSONObject newContext = null;
        boolean applyToken = false;
        long tokenDuration = 10L * 365 * 24 * 60 * 60 * 1000;
        try {
            newId = data.getLong("id");

            if (data.has("name")) {
                newName = data.getString("name");
            }
            else {
                newName = "Contact-" + Utils.randomString(8);
            }

            if (data.has("context")) {
                newContext = data.getJSONObject("context");
            }

            if (data.has("applyToken")) {
                applyToken = data.getBoolean("applyToken");
            }

            if (data.has("duration")) {
                tokenDuration = data.getLong("duration");
            }
        } catch (Exception e) {
            Logger.w(this.getClass(), "#run", e);
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, ContactStateCode.InvalidParameter.code, data));
            markResponseTime();
            return;
        }

        // 新建联系人
        Contact newContact = ContactManager.getInstance().newContact(newId, authToken.getDomain(),
                newName, newContext);
        if (null == newContact) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, ContactStateCode.Failure.code, data));
            markResponseTime();
            return;
        }

        AuthToken newToken = null;
        if (applyToken) {
            newToken = ContactManager.getInstance().getAuthService().applyToken(authToken.getDomain(),
                    authToken.getAppKey(), newContact.getId(), tokenDuration);
        }

        JSONObject resultData = new JSONObject();
        resultData.put("contact", newContact.toJSON());
        if (null != newToken) {
            resultData.put("token", newToken.toJSON());
        }

        this.cellet.speak(this.talkContext,
                this.makeResponse(action, packet, ContactStateCode.Ok.code, resultData));
        markResponseTime();
    }
}
