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
import cube.aigc.psychology.PaintingReport;
import cube.aigc.psychology.ScaleReport;
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

import java.util.List;

/**
 * 获取心理学报告任务。
 */
public class GetPsychologyReportTask extends ServiceTask {

    public GetPsychologyReportTask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
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
        AuthToken authToken = service.getToken(token);
        if (null == authToken) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.IllegalOperation.code, new JSONObject()));
            markResponseTime();
            return;
        }

        long sn = 0;
        boolean markdown = false;
        boolean sections = false;

        long contactId = authToken.getContactId();
        int pageIndex = 0;
        int pageSize = 10;
        boolean descending = true;
        int state = -1;
        String type = "painting";

        try {
            sn = packet.data.has("sn") ? packet.data.getLong("sn") : 0;
            sections = packet.data.has("sections") && packet.data.getBoolean("sections");
            markdown = packet.data.has("markdown") && packet.data.getBoolean("markdown");

            pageIndex = packet.data.has("page") ? packet.data.getInt("page") : 0;
            pageSize = packet.data.has("size") ? packet.data.getInt("size") : 10;
            descending = !packet.data.has("desc") || packet.data.getBoolean("desc");
            state = packet.data.has("state") ? packet.data.getInt("state") : -1;
            type = packet.data.has("type") ? packet.data.getString("type") : "painting";
        } catch (Exception e) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.InvalidParameter.code, new JSONObject()));
            markResponseTime();
            return;
        }

        if (0 != sn) {
            // 绘画报告
            PaintingReport report = PsychologyScene.getInstance().getPaintingReport(sn);
            if (null != report) {
                try {
                    JSONObject reportJson = null;
                    if (markdown) {
                        reportJson = report.toMarkdown();
                    }
                    else {
                        if (sections) {
                            reportJson = report.makeReportSectionJSON();
                        }
                        else {
                            reportJson = report.toCompactJSON();
                            // 所在队列位置
                            reportJson.put("queuePosition", PsychologyScene.getInstance().getGeneratingQueuePosition(sn));
                        }
                    }

                    this.cellet.speak(this.talkContext,
                            this.makeResponse(dialect, packet, AIGCStateCode.Ok.code, reportJson));
                } catch (Exception e) {
                    Logger.w(this.getClass(), "#run", e);
                    this.cellet.speak(this.talkContext,
                            this.makeResponse(dialect, packet, AIGCStateCode.Failure.code, packet.data));
                }
            }
            else {
                // 量表报告
                ScaleReport scaleReport = PsychologyScene.getInstance().getScaleReport(sn);
                if (null != scaleReport) {
                    this.cellet.speak(this.talkContext,
                            this.makeResponse(dialect, packet, AIGCStateCode.Ok.code, scaleReport.toJSON()));
                }
                else {
                    this.cellet.speak(this.talkContext,
                            this.makeResponse(dialect, packet, AIGCStateCode.Failure.code, packet.data));
                }
            }
        }
        else if (0 != pageSize) {
            if (type.equalsIgnoreCase("painting")) {
                int num = (state == -1) ? PsychologyScene.getInstance().numPsychologyReports(contactId) :
                        PsychologyScene.getInstance().numPsychologyReports(contactId, state);

                List<PaintingReport> list = (state == -1) ?
                        PsychologyScene.getInstance().getPsychologyReports(contactId, pageIndex, pageSize, descending) :
                        PsychologyScene.getInstance().getPsychologyReportsWithState(contactId, pageIndex, pageSize, descending, state);
                JSONArray array = new JSONArray();
                for (PaintingReport report : list) {
                    array.put(report.toCompactJSON());
                }

                JSONObject responseData = new JSONObject();
                responseData.put("total", num);
                responseData.put("page", pageIndex);
                responseData.put("size", pageSize);
                responseData.put("list", array);
                responseData.put("type", type);
                this.cellet.speak(this.talkContext,
                        this.makeResponse(dialect, packet, AIGCStateCode.Ok.code, responseData));
            }
            else {
                int num = (state == -1) ? PsychologyScene.getInstance().numScaleReports(contactId) :
                        PsychologyScene.getInstance().numScaleReports(contactId, state);

                List<ScaleReport> list = (state == -1) ?
                        PsychologyScene.getInstance().getScaleReports(contactId, descending) :
                        PsychologyScene.getInstance().getScaleReports(contactId, state, descending);
                JSONArray array = new JSONArray();
                for (ScaleReport report : list) {
                    array.put(report.toCompactJSON());
                }

                JSONObject responseData = new JSONObject();
                responseData.put("total", num);
                responseData.put("page", pageIndex);
                responseData.put("size", num);
                responseData.put("list", array);
                responseData.put("type", type);
                this.cellet.speak(this.talkContext,
                        this.makeResponse(dialect, packet, AIGCStateCode.Ok.code, responseData));
            }
        }
        else {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.InvalidParameter.code, packet.data));
        }

        markResponseTime();
    }
}
