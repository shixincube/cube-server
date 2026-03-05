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
import cube.aigc.psychology.app.CounselingSchedule;
import cube.aigc.psychology.app.Customer;
import cube.auth.AuthToken;
import cube.benchmark.ResponseTime;
import cube.common.Packet;
import cube.common.state.AIGCStateCode;
import cube.service.ServiceTask;
import cube.service.aigc.AIGCCellet;
import cube.service.aigc.AIGCService;
import cube.service.aigc.scene.PsychologyScene;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 查询日程数据任务。
 */
public class AppQueryScheduleTask extends ServiceTask {

    public AppQueryScheduleTask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
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
        AuthToken token = service.getToken(tokenCode);
        if (null == token) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.InconsistentToken.code, new JSONObject()));
            markResponseTime();
            return;
        }

        try {
            long starting = packet.data.has("starting") ?
                    packet.data.getLong("starting") :
                    System.currentTimeMillis() - (365 * 24 * 60 * 60 * 1000L);
            long ending = packet.data.has("ending") ?
                    packet.data.getLong("ending") :
                    System.currentTimeMillis() + (365 * 24 * 60 * 60 * 1000L);

            // 总数
            int total = PsychologyScene.getInstance().getStorage().countSchedules(token.getContactId(), starting, ending);
            // 列表
            List<CounselingSchedule> list = PsychologyScene.getInstance().getStorage().readSchedules(
                    token.getContactId(), starting, ending);

            JSONObject responseJson = new JSONObject();
            JSONArray array = new JSONArray();
            for (CounselingSchedule schedule : list) {
                array.put(schedule.toJSON());
            }
            responseJson.put("list", array);
            responseJson.put("total", total);
            responseJson.put("starting", starting);
            responseJson.put("ending", ending);

            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.Ok.code, responseJson));
            markResponseTime();
        } catch (Exception e) {
            Logger.e(this.getClass(), "", e);
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.InvalidParameter.code, packet.data));
            markResponseTime();
        }
    }
}
