/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.psychology.scene.subtask;

import cube.aigc.ModelConfig;
import cube.aigc.listener.GenerateTextListener;
import cube.aigc.psychology.PaintingReport;
import cube.aigc.psychology.Resource;
import cube.aigc.psychology.composition.ConversationContext;
import cube.aigc.psychology.composition.ConversationRelation;
import cube.aigc.psychology.composition.Subtask;
import cube.aigc.spi.AIGCHost;
import cube.common.entity.AIGCChannel;
import cube.common.entity.ComplexContext;
import cube.common.entity.GeneratingRecord;
import cube.common.state.AIGCStateCode;
import cube.service.psychology.scene.PsychologyScene;
import cube.service.psychology.scene.ReportRenderer;
import cube.service.psychology.scene.SceneManager;

import java.util.List;

public class ShowPaintingSubtask extends ConversationSubtask {

    public ShowPaintingSubtask(AIGCHost host, AIGCChannel channel, String query, ComplexContext context,
                               ConversationRelation relation, ConversationContext convCtx,
                               GenerateTextListener listener) {
        super(Subtask.ShowPainting, host, channel, query, context, relation, convCtx, listener);
    }

    @Override
    public AIGCStateCode execute(Subtask roundSubtask) {
        final PaintingReport report = convCtx.getCurrentReport();
        if (null == report) {
            final List<PaintingReport> list = PsychologyScene.getInstance().getPaintingReports(
                    convCtx.getAuthToken().getContactId(), 0, 1);
            if (list.isEmpty()) {
                this.host.schedule("psychology-subtask", 0, new Runnable() {
                    @Override
                    public void run() {
                        GeneratingRecord record = new GeneratingRecord(query);
                        record.answer = Resource.getInstance().getCorpus(CORPUS,
                                "ANSWER_NO_REPORTS_DATA");
                        convCtx.getSubtaskMemory().record(record);
                        listener.onGenerated(channel, record);
                        channel.setProcessing(false);

                        SceneManager.getInstance().saveHistoryRecord(channel.getCode(), ModelConfig.BAIZE_UNIT,
                                convCtx, record);
                    }
                });
                return AIGCStateCode.Ok;
            }
            else {
                this.host.schedule("psychology-subtask", 0, new Runnable() {
                    @Override
                    public void run() {
                        PaintingReport report = list.get(0);
                        GeneratingRecord record = new GeneratingRecord(query);
                        record.answer = String.format(Resource.getInstance().getCorpus(CORPUS,
                                "FORMAT_ANSWER_SHOW_PAINTING_RECENT_ONE"),
                                ReportRenderer.makeReportTitle(report),
                                ReportRenderer.makeReportPaintingLink(channel, report));
                        convCtx.getSubtaskMemory().record(record);
                        listener.onGenerated(channel, record);
                        channel.setProcessing(false);

                        SceneManager.getInstance().saveHistoryRecord(channel.getCode(), ModelConfig.BAIZE_UNIT,
                                convCtx, record);
                    }
                });
                return AIGCStateCode.Ok;
            }
        }
        else {
            this.host.schedule("psychology-subtask", 0, new Runnable() {
                @Override
                public void run() {
                    GeneratingRecord record = new GeneratingRecord(query);
                    record.answer = String.format(Resource.getInstance().getCorpus(CORPUS,
                            "FORMAT_ANSWER_SHOW_PAINTING"),
                            ReportRenderer.makeReportTitle(report),
                            ReportRenderer.makeReportPaintingLink(channel, report));
                    convCtx.getSubtaskMemory().record(record);
                    listener.onGenerated(channel, record);
                    channel.setProcessing(false);

                    SceneManager.getInstance().saveHistoryRecord(channel.getCode(), ModelConfig.BAIZE_UNIT,
                            convCtx, record);
                }
            });
            return AIGCStateCode.Ok;
        }
    }
}
