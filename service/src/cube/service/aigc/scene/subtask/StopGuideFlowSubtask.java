/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.scene.subtask;

import cube.aigc.ModelConfig;
import cube.aigc.psychology.Resource;
import cube.aigc.psychology.composition.ConversationContext;
import cube.aigc.psychology.composition.ConversationRelation;
import cube.aigc.psychology.composition.Subtask;
import cube.common.entity.AIGCChannel;
import cube.common.entity.ComplexContext;
import cube.common.entity.GeneratingRecord;
import cube.common.state.AIGCStateCode;
import cube.service.aigc.AIGCService;
import cube.service.aigc.guidance.GuideFlow;
import cube.service.aigc.listener.GenerateTextListener;
import cube.service.aigc.scene.SceneManager;
import cube.util.TimeDuration;
import cube.util.TimeUtils;

public class StopGuideFlowSubtask extends ConversationSubtask {

    public StopGuideFlowSubtask(AIGCService service, AIGCChannel channel, String query,
                                ComplexContext context, ConversationRelation relation, ConversationContext convCtx,
                                GenerateTextListener listener) {
        super(Subtask.StopQuestionnaire, service, channel, query, context, relation, convCtx, listener);
    }

    @Override
    public AIGCStateCode execute(Subtask roundSubtask) {
        final GuideFlow guideFlow = (GuideFlow) convCtx.getGuideFlow();
        if (null == guideFlow) {
            this.service.getExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    GeneratingRecord record = new GeneratingRecord(query);
                    record.answer = Resource.getInstance().getCorpus(CORPUS, "ANSWER_NO_GUIDE_FLOW_DATA");
                    listener.onGenerated(channel, record);
                    channel.setProcessing(false);

                    SceneManager.getInstance().saveHistoryRecord(channel.getCode(), ModelConfig.AIXINLI,
                            convCtx, record);
                }
            });
            return AIGCStateCode.Ok;
        }

        // 取消子任务
        this.convCtx.cancelCurrentSubtask();

        this.service.getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                // 结束
                guideFlow.stop();

                ComplexContext complexContext = new ComplexContext(ComplexContext.Type.Lightweight);
                complexContext.setSubtask(Subtask.StopGuideFlow);

                TimeDuration duration = TimeUtils.calcTimeDuration(
                        guideFlow.getEndTimestamp() - guideFlow.getStartTimestamp());

                GeneratingRecord record = new GeneratingRecord(query);
//                record.answer = polish(String.format(
//                        Resource.getInstance().getCorpus(CORPUS, "FORMAT_ANSWER_GOOD_STOP_QUESTIONNAIRE"),
//                        scaleTrack.scale.getAllChosenAnswers().size(),
//                        duration.toHumanStringDHMS(),
//                        ""
//                ));
                record.context = complexContext;
                listener.onGenerated(channel, record);
                channel.setProcessing(false);

                SceneManager.getInstance().saveHistoryRecord(channel.getCode(), ModelConfig.AIXINLI,
                        convCtx, record);
            }
        });
        return AIGCStateCode.Ok;
    }
}
