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
import cube.aigc.psychology.PaintingReport;
import cube.aigc.psychology.composition.PaintingFeatureSet;
import cube.aigc.psychology.composition.ReportSection;
import cube.benchmark.ResponseTime;
import cube.common.Packet;
import cube.common.state.AIGCStateCode;
import cube.service.ServiceTask;
import cube.service.aigc.AIGCCellet;
import cube.service.aigc.AIGCService;
import cube.service.aigc.scene.PsychologyHelper;
import cube.service.aigc.scene.PsychologyScene;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * 获取心理学报告指定部分的内容任务。
 */
public class GetPsychologyReportPartTask extends ServiceTask {

    public GetPsychologyReportPartTask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
        super(cellet, talkContext, primitive, responseTime);
    }

    @Override
    public void run() {
        ActionDialect dialect = new ActionDialect(this.primitive);
        Packet packet = new Packet(dialect);

        String token = getTokenCode(dialect);
        if (null == token) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.NoToken.code, new JSONObject()));
            markResponseTime();
            return;
        }

        AIGCService service = ((AIGCCellet) this.cellet).getService();
        if (null == service.getToken(token)) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.IllegalOperation.code, new JSONObject()));
            markResponseTime();
            return;
        }

        long sn = 0;
        boolean content = false;
        boolean section = false;
        boolean thought = false;

        try {
            sn = packet.data.getLong("sn");
            content = packet.data.has("content") && packet.data.getBoolean("content");
            section = packet.data.has("section") && packet.data.getBoolean("section");
            thought = packet.data.has("thought") && packet.data.getBoolean("thought");
        } catch (Exception e) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.InvalidParameter.code, new JSONObject()));
            markResponseTime();
            return;
        }

        PaintingReport report = PsychologyScene.getInstance().getPaintingReport(sn);
        if (null == report) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.Failure.code, new JSONObject()));
            markResponseTime();
            return;
        }

        JSONObject responseData = new JSONObject();
        responseData.put("sn", sn);
        responseData.put("state", report.getState().code);

        if (report.getState().code == AIGCStateCode.Ok.code) {
            if (content) {
                String contentMarkdown = PsychologyHelper.makeContentMarkdown(report);
                responseData.put("content", contentMarkdown);
            }

            if (section) {
                List<ReportSection> list = report.getReportTextList();
                JSONArray array = new JSONArray();
                for (ReportSection rs : list) {
                    array.put(rs.toJSON());
                }
                responseData.put("sections", array);
            }

            if (thought) {
                PaintingFeatureSet featureSet = PsychologyScene.getInstance().getPaintingFeatureSet(sn);
                if (null != featureSet) {
                    responseData.put("thought", PsychologyHelper.makeMarkdown(featureSet));
                }
            }
        }

        this.cellet.speak(this.talkContext,
                this.makeResponse(dialect, packet, AIGCStateCode.Ok.code, responseData));
        markResponseTime();
    }
}
