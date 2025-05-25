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
import cube.service.aigc.scene.ContentTools;
import cube.service.aigc.scene.SceneManager;

import java.util.List;

public class ShowPersonalitySubtask extends ConversationSubtask {

    public final static String RECENT_ONE = "最近一份";

    public ShowPersonalitySubtask(AIGCService service, AIGCChannel channel, String query, ComplexContext context,
                                  ConversationRelation relation, ConversationContext convCtx,
                                  GenerateTextListener listener) {
        super(Subtask.ShowPainting, service, channel, query, context, relation, convCtx, listener);
    }

    @Override
    public AIGCStateCode execute(Subtask roundSubtask) {
        final PaintingReport report = convCtx.getCurrentReport();
        if (null == report) {
            final List<PaintingReport> list = PsychologyScene.getInstance().getPsychologyReports(
                    convCtx.getAuthToken().getContactId(), 0, 1);
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

                        SceneManager.getInstance().saveHistoryRecord(channel.getCode(), ModelConfig.AIXINLI,
                                convCtx, record);
                    }
                });
                return AIGCStateCode.Ok;
            }
            else {
                this.service.getExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        PaintingReport report = list.get(0);
                        GeneratingRecord record = new GeneratingRecord(query);
                        record.answer = String.format(Resource.getInstance().getCorpus(CORPUS,
                                "FORMAT_ANSWER_SHOW_PERSONALITY"),
                                RECENT_ONE,
                                ContentTools.makeReportTitle(report),
                                ContentTools.makeContent(report, false, 0, true));
                        record.answer += "\n\n" + ContentTools.makePageLink(channel.getHttpsEndpoint(),
                                channel.getAuthToken().getCode(), report, false, true);
                        convCtx.record(record);
                        listener.onGenerated(channel, record);
                        channel.setProcessing(false);

                        SceneManager.getInstance().saveHistoryRecord(channel.getCode(), ModelConfig.AIXINLI,
                                convCtx, record);
                    }
                });
                return AIGCStateCode.Ok;
            }
        }
        else {
            this.service.getExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    GeneratingRecord record = new GeneratingRecord(query);
                    record.answer = String.format(Resource.getInstance().getCorpus(CORPUS,
                            "FORMAT_ANSWER_SHOW_PERSONALITY"),
                            "",
                            ContentTools.makeReportTitle(report),
                            ContentTools.makeContent(report, false, 0, true));
                    record.answer += "\n\n" + ContentTools.makePageLink(channel.getHttpsEndpoint(),
                            channel.getAuthToken().getCode(), report, false, true);
                    convCtx.record(record);
                    listener.onGenerated(channel, record);
                    channel.setProcessing(false);

                    SceneManager.getInstance().saveHistoryRecord(channel.getCode(), ModelConfig.AIXINLI,
                            convCtx, record);
                }
            });
            return AIGCStateCode.Ok;
        }
    }
}
