/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.task;

import cell.core.cellet.Cellet;
import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.util.log.Logger;
import cube.auth.AuthToken;
import cube.benchmark.ResponseTime;
import cube.common.Packet;
import cube.common.entity.Contact;
import cube.common.entity.Device;
import cube.common.entity.User;
import cube.common.state.AIGCStateCode;
import cube.service.ServiceTask;
import cube.service.aigc.AIGCCellet;
import cube.service.aigc.AIGCService;
import cube.service.contact.ContactManager;
import cube.util.DummyDevice;

/**
 * 校验用户信息。
 */
public class AppCheckInUserTask extends ServiceTask {

    public AppCheckInUserTask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
        super(cellet, talkContext, primitive, responseTime);
    }

    @Override
    public void run() {
        ActionDialect dialect = new ActionDialect(this.primitive);
        Packet packet = new Packet(dialect);

        String token = getTokenCode(dialect);
        if (null == token) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.NoToken.code, packet.data));
            markResponseTime();
            return;
        }

        AIGCService service = ((AIGCCellet) this.cellet).getService();
        AuthToken authToken = service.getToken(token);
        if (null == authToken) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.InconsistentToken.code, packet.data));
            markResponseTime();
            return;
        }

        try {
            String userName = packet.data.getString("name");
            String pwdMD5 = packet.data.getString("password");
            Device device = null;
            if (packet.data.has("device")) {
                device = new Device(packet.data.getJSONObject("device"));
            }
            else {
                device = new Device("Unknown", "Unknown");
            }
            if (packet.data.has("address")) {
                device.setAddress(packet.data.getString("address"));
            }
            else {
                Logger.w(this.getClass(), "#run - No address data");
            }

            boolean register = packet.data.getBoolean("register");

            if (userName.length() < 8 || pwdMD5.length() < 8) {
                this.cellet.speak(this.talkContext,
                        this.makeResponse(dialect, packet, AIGCStateCode.NoData.code, packet.data));
                markResponseTime();
                return;
            }

            Contact contact = ContactManager.getInstance().getContact(token);
            // 关联设备信息
            contact.removeDevices();
            contact.addDevice(device);
            User user = service.checkInUser(register, userName, pwdMD5, contact);
            if (null == user) {
                this.cellet.speak(this.talkContext,
                        this.makeResponse(dialect, packet, AIGCStateCode.IllegalOperation.code, packet.data));
                markResponseTime();
                return;
            }

            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.Ok.code, user.toCompactJSON()));
            markResponseTime();
        } catch (Exception e) {
            Logger.e(this.getClass(), "#run", e);
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.Failure.code, packet.data));
            markResponseTime();
        }
    }
}
