/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.psychology.scene.subtask;

import cube.aigc.ModelConfig;
import cube.aigc.guidance.AbstractGuideFlow;
import cube.aigc.listener.GenerateTextListener;
import cube.aigc.psychology.composition.ConversationContext;
import cube.aigc.psychology.composition.ConversationRelation;
import cube.aigc.psychology.composition.Subtask;
import cube.aigc.spi.AIGCHost;
import cube.common.entity.AIGCChannel;
import cube.common.entity.ComplexContext;
import cube.common.entity.GeneratingRecord;
import cube.common.state.AIGCStateCode;
import cube.service.psychology.scene.SceneManager;

public class StopGuideFlowSubtask extends ConversationSubtask {

    public StopGuideFlowSubtask(AIGCHost host, AIGCChannel channel, String query,
                                ComplexContext context, ConversationRelation relation, ConversationContext convCtx,
                                GenerateTextListener listener) {
        super(Subtask.StopQuestionnaire, host, channel, query, context, relation, convCtx, listener);
    }

    @Override
    public AIGCStateCode execute(Subtask roundSubtask) {
        final AbstractGuideFlow guideFlow = convCtx.getGuideFlow();
        if (null == guideFlow) {
            this.host.schedule("psychology-subtask", 0, new Runnable() {
                @Override
                public void run() {
                    GeneratingRecord record = new GeneratingRecord(query);
                    record.answer = StopGuideFlowSubtask.this.host.getGuidePrompt("ANSWER_NO_GUIDE_FLOW_DATA");
                    listener.onGenerated(channel, record);
                    channel.setProcessing(false);

                    SceneManager.getInstance().saveHistoryRecord(channel.getCode(), ModelConfig.BAIZE_UNIT,
                            convCtx, record);
                }
            });
            return AIGCStateCode.Ok;
        }

        // 取消子任务
        this.convCtx.deactivateSubtask();

        this.host.schedule("psychology-subtask", 0, new Runnable() {
            @Override
            public void run() {
                // 结束
                guideFlow.stop();

//                GuideFlow impl = (GuideFlow) guideFlow;

                ComplexContext complexContext = new ComplexContext();
                complexContext.setSubtask(Subtask.StopGuideFlow);

//                TimeDuration duration = TimeUtils.calcTimeDuration(
//                        impl.getEndTimestamp() - impl.getStartTimestamp());

                GeneratingRecord record = new GeneratingRecord(query);
                record.answer = polish(StopGuideFlowSubtask.this.host.getGuidePrompt("FORMAT_ANSWER_STOP_GUIDE_FLOW")).trim();
                record.context = complexContext;
                listener.onGenerated(channel, record);
                channel.setProcessing(false);

                SceneManager.getInstance().saveHistoryRecord(channel.getCode(), ModelConfig.BAIZE_UNIT,
                        convCtx, record);
            }
        });
        return AIGCStateCode.Ok;
    }
}
