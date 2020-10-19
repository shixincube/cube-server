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

package cube.service.contact.task;

import cell.core.cellet.Cellet;
import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.core.talk.dialect.DialectFactory;
import cell.util.json.JSONException;
import cell.util.json.JSONObject;
import cube.auth.AuthToken;
import cube.common.Packet;
import cube.common.entity.Contact;
import cube.common.state.ContactStateCode;
import cube.service.ServiceTask;
import cube.service.contact.ContactManager;

/**
 * 联系人签出。
 */
public class SignOutTask extends ServiceTask {

    public SignOutTask(Cellet cellet, TalkContext talkContext, Primitive primitive) {
        super(cellet, talkContext, primitive);
    }

    @Override
    public void run() {
        ActionDialect action = DialectFactory.getInstance().createActionDialect(this.primitive);
        Packet packet = new Packet(action);

        JSONObject selfJson = packet.data;

        // 创建联系人对象
        Contact self = new Contact(selfJson, this.talkContext);

        // 设置终端的对应关系
        Contact newSelf = ContactManager.getInstance().signOut(self, this.getTokenCode(action));
        if (null == newSelf) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, ContactStateCode.IllegalOperation.code, packet.data));
            return;
        }

        // 应答
        this.cellet.speak(this.talkContext,
                this.makeResponse(action, packet, ContactStateCode.Ok.code, newSelf.toJSON()));
    }
}
