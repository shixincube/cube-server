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
import cell.util.log.Logger;
import cube.aigc.psychology.Attribute;
import cube.aigc.psychology.Painting;
import cube.aigc.psychology.PsychologyReport;
import cube.aigc.psychology.Theme;
import cube.benchmark.ResponseTime;
import cube.common.Packet;
import cube.common.entity.FileLabel;
import cube.common.state.AIGCStateCode;
import cube.service.ServiceTask;
import cube.service.aigc.AIGCCellet;
import cube.service.aigc.AIGCService;
import cube.service.aigc.scene.PsychologySceneListener;
import org.json.JSONObject;

/**
 * 预测心理学绘画任务。
 */
public class GeneratePsychologyReportTask extends ServiceTask {

    public GeneratePsychologyReportTask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
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

        if (!packet.data.has("fileCode") || !packet.data.has("theme") || !packet.data.has("attribute")) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.InvalidParameter.code, new JSONObject()));
            markResponseTime();
            return;
        }

        Attribute attribute = null;
        String fileCode = null;
        String themeName = null;
        int maxBehaviorTexts = 3;
        int maxIndicatorTexts = 3;
//        boolean paragraphInferrable = false;

        try {
            attribute = new Attribute(packet.data.getJSONObject("attribute"));
            fileCode = packet.data.getString("fileCode");
            themeName = packet.data.getString("theme");
            maxBehaviorTexts = packet.data.has("behaviors") ? packet.data.getInt("behaviors") : 5;
            maxIndicatorTexts = packet.data.has("indicators") ? packet.data.getInt("indicators") : 10;
//            paragraphInferrable = packet.data.has("paragraph")
//                    && packet.data.getBoolean("paragraph");
        } catch (Exception e) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.InvalidParameter.code, new JSONObject()));
            markResponseTime();
            return;
        }

        Theme theme = Theme.parse(themeName);
        if (null == theme) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.InvalidParameter.code, new JSONObject()));
            markResponseTime();
            return;
        }

        AIGCService service = ((AIGCCellet) this.cellet).getService();
        PsychologyReport report = service.generatePsychologyReport(token, attribute, fileCode, theme,
                maxBehaviorTexts, maxIndicatorTexts, new PsychologySceneListener() {
            @Override
            public void onPaintingPredict(PsychologyReport report, FileLabel file) {
                Logger.d(GeneratePsychologyReportTask.class, "#onPaintingPredict - " + token);
            }

            @Override
            public void onPaintingPredictCompleted(PsychologyReport report, FileLabel file, Painting painting) {
                Logger.d(GeneratePsychologyReportTask.class, "#onPaintingPredictCompleted - " + token);
            }

            @Override
            public void onPaintingPredictFailed(PsychologyReport report) {
                Logger.d(GeneratePsychologyReportTask.class, "#onPaintingPredictFailed - " + token);
            }

            @Override
            public void onReportEvaluate(PsychologyReport report) {
                Logger.d(GeneratePsychologyReportTask.class, "#onReportEvaluate - " + token);
            }

            @Override
            public void onReportEvaluateCompleted(PsychologyReport report) {
                Logger.d(GeneratePsychologyReportTask.class, "#onReportEvaluateCompleted - " + token);
            }

            @Override
            public void onReportEvaluateFailed(PsychologyReport report) {
                Logger.d(GeneratePsychologyReportTask.class, "#onReportEvaluateFailed - " + token);
            }
        });

        if (null != report) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.Ok.code, report.toJSON()));
        }
        else {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.Failure.code, packet.data));
        }
        markResponseTime();
    }
}
