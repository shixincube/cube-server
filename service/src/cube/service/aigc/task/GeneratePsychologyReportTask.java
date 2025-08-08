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
import cube.aigc.psychology.*;
import cube.aigc.psychology.composition.Usage;
import cube.benchmark.ResponseTime;
import cube.common.Packet;
import cube.common.entity.FileLabel;
import cube.common.state.AIGCStateCode;
import cube.service.ServiceTask;
import cube.service.aigc.AIGCCellet;
import cube.service.aigc.AIGCService;
import cube.service.aigc.scene.PaintingReportListener;
import cube.service.aigc.scene.PsychologyScene;
import cube.service.aigc.scene.ScaleReportListener;
import org.json.JSONObject;

/**
 * 生成心理学报告任务。
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

        if (packet.data.has("fileCode") && packet.data.has("theme") && packet.data.has("attribute")) {
            Attribute attribute = null;
            String fileCode = null;
            String themeName = null;
            int maxIndicators = 10;
            boolean adjust = true;
            final StringBuilder remote = new StringBuilder();

            try {
                attribute = new Attribute(packet.data.getJSONObject("attribute"));
                fileCode = packet.data.getString("fileCode");
                themeName = packet.data.getString("theme");
                maxIndicators = packet.data.has("indicators") ? packet.data.getInt("indicators") : 10;
                adjust = packet.data.has("adjust") ? packet.data.getBoolean("adjust") : true;
                remote.append(packet.data.has("remote") ? packet.data.getString("remote") : "");
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
            PaintingReport report = service.generatePaintingReport(token, attribute, fileCode, theme,
                    maxIndicators, adjust, new PaintingReportListener() {
                        @Override
                        public void onPaintingPredicting(PaintingReport report, FileLabel file) {
                            Logger.d(GeneratePsychologyReportTask.class, "#onPaintingPredicting - " + token);
                        }

                        @Override
                        public void onPaintingPredictCompleted(PaintingReport report, FileLabel file, Painting painting) {
                            Logger.d(GeneratePsychologyReportTask.class, "#onPaintingPredictCompleted - " + token);
                        }

                        @Override
                        public void onPaintingPredictFailed(PaintingReport report) {
                            Logger.d(GeneratePsychologyReportTask.class, "#onPaintingPredictFailed - " + token);
                        }

                        @Override
                        public void onReportEvaluating(PaintingReport report) {
                            Logger.d(GeneratePsychologyReportTask.class, "#onReportEvaluating - " + token);
                        }

                        @Override
                        public void onReportEvaluateCompleted(PaintingReport report) {
                            Logger.d(GeneratePsychologyReportTask.class, "#onReportEvaluateCompleted - " + token);

                            Usage usage = new Usage(service.getToken(token), remote.toString(), report);
                            PsychologyScene.getInstance().recordUsage(usage);
                        }

                        @Override
                        public void onReportEvaluateFailed(PaintingReport report) {
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
        else if (packet.data.has("scaleSn")) {
            long scaleSn = 0;
            final StringBuilder remote = new StringBuilder();

            try {
                scaleSn = packet.data.getLong("scaleSn");
                remote.append(packet.data.has("remote") ? packet.data.getString("remote") : "");
            } catch (Exception e) {
                this.cellet.speak(this.talkContext,
                        this.makeResponse(dialect, packet, AIGCStateCode.InvalidParameter.code, new JSONObject()));
                markResponseTime();
                return;
            }

            AIGCService service = ((AIGCCellet) this.cellet).getService();

            ScaleReport report = service.generateScaleReport(token, scaleSn, new ScaleReportListener() {
                @Override
                public void onReportEvaluating(ScaleReport report) {
                    Logger.d(GeneratePsychologyReportTask.class, "#onReportEvaluating - " + token);
                }

                @Override
                public void onReportEvaluateCompleted(ScaleReport report) {
                    Logger.d(GeneratePsychologyReportTask.class, "#onReportEvaluateCompleted - " + token);

                    try {
                        Usage usage = new Usage(service.getToken(token), remote.toString(), report);
                        PsychologyScene.getInstance().recordUsage(usage);
                    } catch (Exception e) {
                        Logger.e(GeneratePsychologyReportTask.class, "#onReportEvaluateCompleted", e);
                    }
                }

                @Override
                public void onReportEvaluateFailed(ScaleReport report) {
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
        else {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.InvalidParameter.code, new JSONObject()));
            markResponseTime();
        }
    }
}
