/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.scene.subtask;

import cube.aigc.ModelConfig;
import cube.aigc.psychology.PaintingReport;
import cube.aigc.psychology.Resource;
import cube.aigc.psychology.composition.ConversationContext;
import cube.aigc.psychology.composition.ConversationRelation;
import cube.aigc.psychology.composition.Subtask;
import cube.common.entity.AIGCChannel;
import cube.common.entity.ComplexContext;
import cube.common.entity.GeneratingRecord;
import cube.common.state.AIGCStateCode;
import cube.service.aigc.AIGCService;
import cube.service.aigc.listener.GenerateTextListener;
import cube.service.aigc.scene.PsychologyScene;
import cube.service.aigc.scene.ReportHelper;
import cube.service.aigc.scene.SceneManager;

import java.util.List;

public class QueryReportSubtask extends ConversationSubtask {

    public QueryReportSubtask(AIGCService service, AIGCChannel channel, String query, ComplexContext context,
                              ConversationRelation relation, ConversationContext convCtx,
                              GenerateTextListener listener) {
        super(Subtask.QueryReport, service, channel, query, context, relation, convCtx, listener);
    }

    @Override
    public AIGCStateCode execute(Subtask roundSubtask) {
        final List<PaintingReport> list = PsychologyScene.getInstance().getPsychologyReports(
                convCtx.getAuthToken().getContactId(), 0, 10);
        final int total = PsychologyScene.getInstance().numPsychologyReports(convCtx.getAuthToken().getContactId(), 0);

        convCtx.setReportList(list);
        if (list.isEmpty()) {
            this.service.getExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    GeneratingRecord record = new GeneratingRecord(query);
                    record.answer = Resource.getInstance().getCorpus(CORPUS,
                            "ANSWER_NO_REPORTS_DATA");
                    convCtx.record(record);
                    listener.onGenerated(channel, record);
                    channel.setProcessing(false);
                }
            });
            return AIGCStateCode.Ok;
        }
        else {
            this.service.getExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    String answer = infer(String.format(Resource.getInstance().getCorpus(CORPUS,
                            "FORMAT_PROMPT_QUERY_REPORT_RESULT"),
                            total, list.size(), ReportHelper.makeReportListMarkdown(channel, list), query));
                    if (null == answer) {
                        answer = String.format(Resource.getInstance().getCorpus(CORPUS,
                                "FORMAT_ANSWER_QUERY_REPORT_RESULT"),
                                total, list.size(), ReportHelper.makeReportListMarkdown(channel, list));
                    }
                    GeneratingRecord record = new GeneratingRecord(query);
                    record.answer = answer;
                    convCtx.record(record);
                    listener.onGenerated(channel, record);
                    channel.setProcessing(false);

                    SceneManager.getInstance().writeRecord(channel.getCode(), ModelConfig.AIXINLI,
                            convCtx, record);
                }
            });
            return AIGCStateCode.Ok;
        }
    }
}
