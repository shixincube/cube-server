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
import cell.core.talk.dialect.DialectFactory;
import cell.util.log.Logger;
import cube.common.Packet;
import cube.common.entity.Contact;
import cube.service.ServiceTask;
import cube.service.contact.ContactManager;

/**
 * 联系人设备断开。
 */
public class DisconnectTask extends ServiceTask {

    public DisconnectTask(Cellet cellet, TalkContext talkContext, Primitive primitive) {
        super(cellet, talkContext, primitive);
    }

    @Override
    public void run() {
        ActionDialect action = DialectFactory.getInstance().createActionDialect(this.primitive);
        Packet packet = new Packet(action);

        // 获取联系人
        Contact contact = new Contact(packet.data);

        // 报告设备断开连接
        ContactManager.getInstance().reportDisconnect(contact);

        if (Logger.isDebugLevel()) {
            Logger.d(this.getClass(), "Disconnect : " + contact.getId()
                    + " # " + contact.getDeviceList().get(0).toString());
        }
    }
}
