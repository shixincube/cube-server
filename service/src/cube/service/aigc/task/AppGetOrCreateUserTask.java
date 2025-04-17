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
import cube.benchmark.ResponseTime;
import cube.common.Packet;
import cube.common.entity.Device;
import cube.common.entity.User;
import cube.common.state.AIGCStateCode;
import cube.service.ServiceTask;
import cube.service.aigc.AIGCCellet;
import cube.service.aigc.AIGCService;

/**
 * 获取或创建用户。
 */
public class AppGetOrCreateUserTask extends ServiceTask {

    public AppGetOrCreateUserTask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
        super(cellet, talkContext, primitive, responseTime);
    }

    @Override
    public void run() {
        ActionDialect dialect = new ActionDialect(this.primitive);
        Packet packet = new Packet(dialect);

        if (packet.data.has("token")) {
            try {
                String token = packet.data.getString("token");
                AIGCService service = ((AIGCCellet) this.cellet).getService();
                User user = service.getUser(token);
                this.cellet.speak(this.talkContext,
                        this.makeResponse(dialect, packet, AIGCStateCode.Ok.code, user.toJSON()));
                markResponseTime();
            } catch (Exception e) {
                Logger.e(this.getClass(), "#run", e);
                this.cellet.speak(this.talkContext,
                        this.makeResponse(dialect, packet, AIGCStateCode.Failure.code, packet.data));
                markResponseTime();
            }
        }
        else if (packet.data.has("appAgent") && packet.data.has("device")) {
            try {
                String appAgent = packet.data.getString("appAgent");
                Device device = new Device(packet.data.getJSONObject("device"));

                Logger.d(this.getClass(), "#run - appAgent: " + appAgent);

                AIGCService service = ((AIGCCellet) this.cellet).getService();
                User user = service.getOrCreateUser(appAgent, device);

                this.cellet.speak(this.talkContext,
                        this.makeResponse(dialect, packet, AIGCStateCode.Ok.code, user.toJSON()));
                markResponseTime();
            } catch (Exception e) {
                Logger.e(this.getClass(), "#run", e);
                this.cellet.speak(this.talkContext,
                        this.makeResponse(dialect, packet, AIGCStateCode.Failure.code, packet.data));
                markResponseTime();
            }
        }
        else {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.InvalidParameter.code, packet.data));
            markResponseTime();
        }
    }
}
