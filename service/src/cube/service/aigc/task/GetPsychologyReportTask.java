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
import cube.aigc.psychology.PsychologyReport;
import cube.benchmark.ResponseTime;
import cube.common.Packet;
import cube.common.state.AIGCStateCode;
import cube.service.ServiceTask;
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

        long sn = 0;
        boolean markdown = false;
        long contactId = 0;
        long startTime = 0;
        long endTime = 0;
        int pageIndex = 0;

        try {
            sn = packet.data.has("sn") ? packet.data.getLong("sn") : 0;
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
            PsychologyReport report = PsychologyScene.getInstance().getPsychologyReport(sn);
            if (null != report) {
                this.cellet.speak(this.talkContext,
                        this.makeResponse(dialect, packet, AIGCStateCode.Ok.code, report.toJSON(markdown)));
            }
            else {
                this.cellet.speak(this.talkContext,
                        this.makeResponse(dialect, packet, AIGCStateCode.Failure.code, packet.data));
            }
        }
        else if (0 != contactId && 0 != startTime && 0 != endTime) {
            int num = PsychologyScene.getInstance().numPsychologyReports(contactId, startTime, endTime);

            List<PsychologyReport> list = PsychologyScene.getInstance().getPsychologyReports(contactId,
                    startTime, endTime, pageIndex);
            JSONArray array = new JSONArray();
            for (PsychologyReport report : list) {
                array.put(report.toJSON(markdown));
            }

            JSONObject responseData = new JSONObject();
            responseData.put("total", num);
            responseData.put("page", pageIndex);
            responseData.put("list", array);
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.Ok.code, responseData));
        }

        markResponseTime();
    }
}
