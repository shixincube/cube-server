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
import cube.aigc.psychology.app.ConsultationSchedule;
import cube.aigc.psychology.consultation.ConsultationScheduleState;
import cube.benchmark.ResponseTime;
import cube.common.Packet;
import cube.common.entity.User;
import cube.common.state.AIGCStateCode;
import cube.service.ServiceTask;
import cube.service.aigc.AIGCCellet;
import cube.service.aigc.AIGCService;
import cube.service.aigc.scene.PsychologyScene;
import org.json.JSONObject;

/**
 * 删除日程数据任务。
 */
public class AppDeleteScheduleTask extends ServiceTask {

    public AppDeleteScheduleTask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
        super(cellet, talkContext, primitive, responseTime);
    }

    @Override
    public void run() {
        ActionDialect dialect = new ActionDialect(this.primitive);
        Packet packet = new Packet(dialect);

        String tokenCode = this.getTokenCode(dialect);
        if (null == tokenCode) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.InvalidParameter.code, new JSONObject()));
            markResponseTime();
            return;
        }

        AIGCService service = ((AIGCCellet) this.cellet).getService();
        User user = service.getUser(tokenCode);
        if (null == user) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.InconsistentToken.code, new JSONObject()));
            markResponseTime();
            return;
        }

        if (!user.isRegistered()) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.IllegalOperation.code, new JSONObject()));
            markResponseTime();
            return;
        }

        try {
            long id = packet.data.getLong("id");
            ConsultationSchedule schedule = PsychologyScene.getInstance().getStorage().readSchedule(user.getContactId(), id);
            if (null == schedule) {
                this.cellet.speak(this.talkContext,
                        this.makeResponse(dialect, packet, AIGCStateCode.NoData.code, packet.data));
                markResponseTime();
                return;
            }

            // 修改状态
            schedule.state = ConsultationScheduleState.Deleted;

            if (PsychologyScene.getInstance().getStorage().writeSchedule(user.getContactId(), schedule)) {
                this.cellet.speak(this.talkContext,
                        this.makeResponse(dialect, packet, AIGCStateCode.Ok.code, schedule.toJSON()));
                markResponseTime();
            }
            else {
                this.cellet.speak(this.talkContext,
                        this.makeResponse(dialect, packet, AIGCStateCode.Failure.code, packet.data));
                markResponseTime();
            }
        } catch (Exception e) {
            Logger.e(this.getClass(), "#run", e);
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.InvalidParameter.code, new JSONObject()));
            markResponseTime();
        }
    }
}
