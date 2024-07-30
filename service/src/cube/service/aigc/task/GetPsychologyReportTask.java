/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 Ambrose Xu.
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

package cube.service.aigc.task;

import cell.core.cellet.Cellet;
import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cube.aigc.psychology.PaintingReport;
import cube.aigc.psychology.ScaleReport;
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
        if (null == service.getToken(token)) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.IllegalOperation.code, new JSONObject()));
            markResponseTime();
            return;
        }

        long sn = 0;
        boolean markdown = false;
        boolean texts = false;
        long contactId = 0;
        long startTime = 0;
        long endTime = 0;
        int pageIndex = 0;

        try {
            sn = packet.data.has("sn") ? packet.data.getLong("sn") : 0;
            texts = packet.data.has("texts") && packet.data.getBoolean("texts");
            markdown = packet.data.has("markdown") && packet.data.getBoolean("markdown");
            contactId = packet.data.has("cid") ? packet.data.getLong("cid") : 0;
            startTime = packet.data.has("start") ? packet.data.getLong("start") : 0;
            endTime = packet.data.has("end") ? packet.data.getLong("end") : 0;
            pageIndex = packet.data.has("page") ? packet.data.getInt("page") : 0;
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
                JSONObject reportJson = null;
                if (markdown) {
                    reportJson = report.toMarkdown();
                }
                else {
                    if (texts) {
                        reportJson = report.toTextListJSON();
                    }
                    else {
                        reportJson = report.toCompactJSON();
                        // 所在队列位置
                        reportJson.put("queuePosition", PsychologyScene.getInstance().getGeneratingQueuePosition(sn));
                    }
                }

                this.cellet.speak(this.talkContext,
                        this.makeResponse(dialect, packet, AIGCStateCode.Ok.code, reportJson));
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
        else if (0 != contactId && 0 != startTime && 0 != endTime) {
            int num = PsychologyScene.getInstance().numPsychologyReports(contactId, startTime, endTime);

            List<PaintingReport> list = PsychologyScene.getInstance().getPsychologyReports(contactId,
                    startTime, endTime, pageIndex);
            JSONArray array = new JSONArray();
            for (PaintingReport report : list) {
                array.put(report.toCompactJSON());
            }

            JSONObject responseData = new JSONObject();
            responseData.put("total", num);
            responseData.put("page", pageIndex);
            responseData.put("list", array);
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.Ok.code, responseData));
        }
        else {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.InvalidParameter.code, packet.data));
        }

        markResponseTime();
    }
}
