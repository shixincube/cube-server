/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.psychology.scene.subtask;

import cube.service.psychology.scene.ReportRenderer;
import cube.aigc.spi.AIGCHost;
import cube.aigc.ModelConfig;
import cube.aigc.complex.widget.ListTile;
import cube.aigc.complex.widget.ListView;
import cube.aigc.complex.widget.PromptAction;
import cube.aigc.psychology.PaintingReport;
import cube.aigc.psychology.Resource;
import cube.aigc.psychology.composition.ConversationContext;
import cube.aigc.psychology.composition.ConversationRelation;
import cube.aigc.psychology.composition.Subtask;
import cube.common.entity.AIGCChannel;
import cube.common.entity.ComplexContext;
import cube.common.entity.GeneratingRecord;
import cube.common.entity.WidgetResource;
import cube.common.state.AIGCStateCode;
import cube.aigc.listener.GenerateTextListener;
import cube.service.psychology.scene.PsychologyScene;
import cube.service.psychology.scene.SceneManager;

import java.util.List;

public class QueryReportSubtask extends ConversationSubtask {

    private final boolean useWidget = true;

    public QueryReportSubtask(AIGCHost host, AIGCChannel channel, String query, ComplexContext context,
                              ConversationRelation relation, ConversationContext convCtx,
                              GenerateTextListener listener) {
        super(Subtask.QueryReport, host, channel, query, context, relation, convCtx, listener);
    }

    @Override
    public AIGCStateCode execute(Subtask roundSubtask) {
        final List<PaintingReport> list = PsychologyScene.getInstance().getPaintingReports(
                convCtx.getAuthToken().getContactId(), 0, 10);
        final int total = PsychologyScene.getInstance().numPaintingReports(convCtx.getAuthToken().getContactId(), 0);

        convCtx.setReportList(list);
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
                    String answer = null;
                    ComplexContext complexContext = null;
                    if (useWidget) {
                        ListView listView = new ListView();
                        for (PaintingReport paintingReport : list) {
                            ListTile tile = new ListTile(ReportRenderer.makeReportThemeName(paintingReport));
                            tile.subtitle = ReportRenderer.makeReportDate(paintingReport);
                            tile.onTap = new PromptAction(
                                    String.format(
                                            Resource.getInstance().getCorpus(CORPUS, "FORMAT_ANSWER_SELECT_REPORT_LOCATION"),
                                            listView.numItems() + 1));
                            listView.addItem(tile);
                        }

                        complexContext = new ComplexContext();
                        complexContext.addResource(new WidgetResource(listView));
                        answer = String.format(
                                Resource.getInstance().getCorpus(CORPUS, "FORMAT_ANSWER_QUERY_REPORT_RESULT_SUMMARY"),
                                total, list.size());
                    }
                    else {
                        answer = infer(String.format(Resource.getInstance().getCorpus(CORPUS,
                                "FORMAT_PROMPT_QUERY_REPORT_RESULT"),
                                total, list.size(), ReportRenderer.makeReportList(list), query));
                        if (null == answer) {
                            answer = String.format(Resource.getInstance().getCorpus(CORPUS,
                                    "FORMAT_ANSWER_QUERY_REPORT_RESULT"),
                                    total, list.size(), ReportRenderer.makeReportList(list));
                        }
                    }

                    GeneratingRecord record = new GeneratingRecord(query);
                    record.context = complexContext;
                    record.answer = answer;
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
