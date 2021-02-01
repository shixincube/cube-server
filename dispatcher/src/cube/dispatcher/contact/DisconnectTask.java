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

import cell.core.talk.TalkContext;
import cube.common.Packet;
import cube.common.Task;
import cube.common.action.ContactAction;
import cube.common.entity.Contact;
import cube.dispatcher.Performer;

/**
 * 终端设备断开时通知服务单元的任务。
 */
public class DisconnectTask extends Task {

    private Performer performer;

    public DisconnectTask(ContactCellet cellet, TalkContext talkContext, Performer performer) {
        super(cellet, talkContext, null);
        this.performer = performer;
    }

    public void reset(TalkContext talkContext) {
        this.talkContext = talkContext;
    }

    @Override
    public void run() {
        // 查询联系人
        Contact contact = this.performer.queryContact(this.talkContext);

        if (null == contact) {
            ((ContactCellet) this.cellet).returnDisconnectTask(this);
            return;
        }

        // 打包
        Packet packet = new Packet(ContactAction.Disconnect.name, contact.toJSON());

        // 发送
        this.performer.transmit(this.talkContext, this.cellet.getName(), packet.toDialect());

        ((ContactCellet) this.cellet).returnDisconnectTask(this);
    }
}
