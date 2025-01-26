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
import cube.benchmark.ResponseTime;
import cube.common.Packet;
import cube.common.entity.Device;
import cube.service.ServiceTask;
import cube.service.contact.ContactManager;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 设备超时任务。
 */
public class DeviceTimeoutTask extends ServiceTask {

    public DeviceTimeoutTask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
        super(cellet, talkContext, primitive, responseTime);
    }

    @Override
    public void run() {
        ActionDialect action = DialectFactory.getInstance().createActionDialect(this.primitive);

        Packet packet = new Packet(action);

        Long contactId = null;
        String domainName = null;
        Device device = null;
        long failureTime = 0;
        long timeout = 0;

        JSONObject data = packet.data;
        try {
            contactId = data.getLong("id");
            domainName = data.getString("domain");
            JSONObject deviceJson = data.getJSONObject("device");
            failureTime = data.getLong("failureTime");
            timeout = data.getLong("timeout");
            device = new Device(deviceJson);
        } catch (JSONException e) {
            Logger.w(this.getClass(), "#run", e);
            return;
        }

        // 移除联系人的设备
        ContactManager.getInstance().removeContactDevice(contactId, domainName, device);

        markResponseTime();
    }
}
