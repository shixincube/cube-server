/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.contact;

import cell.core.talk.TalkContext;
import cube.common.Packet;
import cube.common.action.ContactAction;
import cube.common.entity.Contact;
import cube.dispatcher.DispatcherTask;
import cube.dispatcher.Performer;

/**
 * 终端设备断开时通知服务单元的任务。
 */
public class DisconnectTask extends DispatcherTask {

    public DisconnectTask(ContactCellet cellet, TalkContext talkContext, Performer performer) {
        super(cellet, talkContext, null, performer);
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

        // 移除
        this.performer.removeContact(contact, contact.getDevice(this.talkContext));

        // 打包
        Packet packet = new Packet(ContactAction.Disconnect.name, contact.toJSON());

        // 发送
        this.performer.transmit(this.talkContext, this.cellet.getName(), packet.toDialect());

        ((ContactCellet) this.cellet).returnDisconnectTask(this);
    }
}
