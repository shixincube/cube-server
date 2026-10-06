/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.psychology.scene.subtask;

import cube.aigc.ModelConfig;
import cube.aigc.listener.GenerateTextListener;
import cube.aigc.psychology.composition.ConversationContext;
import cube.aigc.psychology.composition.ConversationRelation;
import cube.aigc.psychology.composition.Subtask;
import cube.aigc.spi.AIGCHost;
import cube.common.entity.AIGCChannel;
import cube.common.entity.ComplexContext;
import cube.common.entity.GeneratingRecord;
import cube.common.state.AIGCStateCode;
import cube.service.psychology.scene.GuideFlow;
import cube.service.psychology.scene.Guides;
import cube.service.psychology.scene.SceneManager;

import java.util.List;

public class StartGuideFlowSubtask extends ConversationSubtask {

    public StartGuideFlowSubtask(AIGCHost host, AIGCChannel channel, String query,
                                 ComplexContext context, ConversationRelation relation, ConversationContext convCtx,
                                 GenerateTextListener listener) {
        super(Subtask.StartGuideFlow, host, channel, query, context, relation, convCtx, listener);
    }

    @Override
    public AIGCStateCode execute(Subtask roundSubtask) {
        List<String> words = this.host.segmentWords(this.query);
        GuideFlow guideFlow = Guides.matchGuideFlow(words);
        if (null == guideFlow) {
            this.host.schedule("psychology-subtask", 0, new Runnable() {
                @Override
                public void run() {
                    GeneratingRecord record = new GeneratingRecord(query);
                    record.answer = StartGuideFlowSubtask.this.host.getGuidePrompt("ANSWER_NO_GUIDE_FLOW_DATA");
                    listener.onGenerated(channel, record);
                    channel.setProcessing(false);

                    SceneManager.getInstance().saveHistoryRecord(channel.getCode(), ModelConfig.BAIZE_UNIT,
                            convCtx, record);
                }
            });
            return AIGCStateCode.Ok;
        }

        // 设置问答引导流
        this.convCtx.setGuideFlow(guideFlow);
        // 设置子任务
        this.convCtx.activateSubtask(Subtask.GuideFlow);

        this.host.schedule("psychology-subtask", 0, new Runnable() {
            @Override
            public void run() {
                // 启动
                guideFlow.start(host);

                String answer = String.format(StartGuideFlowSubtask.this.host.getGuidePrompt("FORMAT_ANSWER_START_GUIDE_FLOW"),
                        polish(guideFlow.getInstruction()).trim(),
                        guideFlow.makeQuestion(true));

                ComplexContext complexContext = new ComplexContext();
                complexContext.setSubtask(Subtask.GuideFlow);

                GeneratingRecord record = new GeneratingRecord(query);
                record.answer = answer;
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
