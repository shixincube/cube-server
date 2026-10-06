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
import cube.aigc.psychology.composition.PaintingFeatureSet;
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

public class ShowCoTSubtask extends ConversationSubtask {

    public ShowCoTSubtask(AIGCHost host, AIGCChannel channel, String query, ComplexContext context,
                          ConversationRelation relation, ConversationContext convCtx,
                          GenerateTextListener listener) {
        super(Subtask.ShowCoT, host, channel, query, context, relation, convCtx, listener);
    }

    @Override
    public AIGCStateCode execute(Subtask roundSubtask) {
        if (null != convCtx.getCurrentReport()) {
            if (convCtx.getCurrentReport().isNull()) {
                this.host.schedule("psychology-subtask", 0, new Runnable() {
                    @Override
                    public void run() {
                        String answer = polish(String.format(
                                Resource.getInstance().getCorpus(CORPUS, "FORMAT_ANSWER_REPORT_IS_NULL"),
                                ReportRenderer.makeReportTitle(convCtx.getCurrentReport())));
                        GeneratingRecord record = new GeneratingRecord(query);
                        record.answer = answer;
                        convCtx.getSubtaskMemory().record(record);
                        listener.onGenerated(channel, record);
                        channel.setProcessing(false);

                        SceneManager.getInstance().saveHistoryRecord(channel.getCode(), ModelConfig.BAIZE_UNIT,
                                convCtx, record);
                    }
                });
            }
            else {
                this.host.schedule("psychology-subtask", 0, new Runnable() {
                    @Override
                    public void run() {
                        PaintingFeatureSet featureSet = PsychologyScene.getInstance().getPaintingFeatureSet(
                                convCtx.getCurrentReport().sn);
                        String answer = null;
                        if (null == featureSet) {
                            answer = polish(String.format(
                                    Resource.getInstance().getCorpus(CORPUS, "FORMAT_ANSWER_REPORT_IS_NULL"),
                                    ReportRenderer.makeReportTitle(convCtx.getCurrentReport())));
                        }
                        else {
                            answer = String.format(
                                    Resource.getInstance().getCorpus(CORPUS, "FORMAT_ANSWER_SHOW_COT"),
                                    ReportRenderer.makeReportTitle(convCtx.getCurrentReport()),
                                    ReportRenderer.makePaintingFeature(featureSet),
                                    channel.getAuthToken().getCode(),
                                    Long.toString(convCtx.getCurrentReport().sn));
                        }
                        GeneratingRecord record = new GeneratingRecord(query);
                        record.answer = answer;
                        convCtx.getSubtaskMemory().record(record);
                        listener.onGenerated(channel, record);
                        channel.setProcessing(false);

                        SceneManager.getInstance().saveHistoryRecord(channel.getCode(), ModelConfig.BAIZE_UNIT,
                                convCtx, record);
                    }
                });
            }
        }
        else {
            this.host.schedule("psychology-subtask", 0, new Runnable() {
                @Override
                public void run() {
                    if (null == convCtx.getReportList()) {
                        List<PaintingReport> list = SceneManager.getInstance().queryReports(convCtx.getAuthToken().getContactId(),
                                0);
                        convCtx.setReportList(list);
                    }
                    String answer = null;
                    if (convCtx.getReportList().isEmpty()) {
                        answer = polish(Resource.getInstance().getCorpus(CORPUS, "ANSWER_NO_REPORTS_DATA"));
                    }
                    else {
                        answer = String.format(
                                Resource.getInstance().getCorpus(CORPUS, "FORMAT_ANSWER_PLEASE_INPUT_REPORT_DESC"),
                                convCtx.getReportList().size());
                    }
                    GeneratingRecord record = new GeneratingRecord(query);
                    record.answer = answer;
                    convCtx.getSubtaskMemory().record(record);
                    listener.onGenerated(channel, record);
                    channel.setProcessing(false);

                    SceneManager.getInstance().saveHistoryRecord(channel.getCode(), ModelConfig.BAIZE_UNIT,
                            convCtx, record);
                }
            });
        }
        return AIGCStateCode.Ok;
    }
}
