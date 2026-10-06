/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.psychology.scene.subtask;

import cell.util.Utils;
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
import cube.service.psychology.scene.ReportRenderer;
import cube.service.psychology.scene.SceneManager;

public class UnselectReportSubtask extends ConversationSubtask {

    public UnselectReportSubtask(AIGCHost host, AIGCChannel channel, String query, ComplexContext context,
                                 ConversationRelation relation, ConversationContext convCtx,
                                 GenerateTextListener listener) {
        super(Subtask.UnselectReport, host, channel, query, context, relation, convCtx, listener);
    }

    @Override
    public AIGCStateCode execute(Subtask roundSubtask) {
        this.host.schedule("psychology-subtask", 0, new Runnable() {
            @Override
            public void run() {
                ComplexContext context = new ComplexContext();
                context.setSubtask(Subtask.UnselectReport);

                if (null != convCtx.getCurrentReport()) {
                    PaintingReport report = convCtx.getCurrentReport();
                    convCtx.setCurrentReport(null);
                    String answer = String.format(Resource.getInstance().getCorpus(CORPUS,
                            "FORMAT_ANSWER_UNSELECT_REPORT_OK"),
                            ReportRenderer.makeReportTitle(report));
                    GeneratingRecord record = new GeneratingRecord(query);
                    record.answer = answer;
                    record.context = context;
                    convCtx.getSubtaskMemory().record(record);
                    listener.onGenerated(channel, record);
                    channel.setProcessing(false);

                    SceneManager.getInstance().saveHistoryRecord(channel.getCode(), ModelConfig.BAIZE_UNIT,
                            convCtx, record);
                }
                else {
                    GeneratingRecord record = new GeneratingRecord(query);
                    record.answer = Utils.randomUnsigned() % 2 == 0 ?
                            Resource.getInstance().getCorpus(CORPUS, "ANSWER_UNSELECT_REPORT_WARNING") :
                            fastPolish(Resource.getInstance().getCorpus(CORPUS, "ANSWER_UNSELECT_REPORT_WARNING"));
                    record.context = context;
                    convCtx.getSubtaskMemory().record(record);
                    listener.onGenerated(channel, record);
                    channel.setProcessing(false);

                    SceneManager.getInstance().saveHistoryRecord(channel.getCode(), ModelConfig.BAIZE_UNIT,
                            convCtx, record);
                }
            }
        });
        return AIGCStateCode.Ok;
    }
}
