/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.scene.subtask;

import cube.aigc.ModelConfig;
import cube.aigc.psychology.composition.ConversationContext;
import cube.aigc.psychology.composition.ConversationRelation;
import cube.aigc.psychology.composition.Subtask;
import cube.common.entity.AIGCChannel;
import cube.common.entity.ComplexContext;
import cube.common.entity.GeneratingRecord;
import cube.common.state.AIGCStateCode;
import cube.service.aigc.AIGCService;
import cube.service.aigc.guidance.GuideFlow;
import cube.service.aigc.guidance.Guides;
import cube.service.aigc.guidance.Prompts;
import cube.service.aigc.listener.GenerateTextListener;
import cube.service.aigc.scene.SceneManager;

import java.util.List;

public class StartGuideFlowSubtask extends ConversationSubtask {

    public StartGuideFlowSubtask(AIGCService service, AIGCChannel channel, String query,
                                 ComplexContext context, ConversationRelation relation, ConversationContext convCtx,
                                 GenerateTextListener listener) {
        super(Subtask.StartGuideFlow, service, channel, query, context, relation, convCtx, listener);
    }

    @Override
    public AIGCStateCode execute(Subtask roundSubtask) {
        List<String> words = this.service.segmentation(this.query);
        GuideFlow guideFlow = Guides.matchGuideFlow(words);
        if (null == guideFlow) {
            this.service.getExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    GeneratingRecord record = new GeneratingRecord(query);
                    record.answer = Prompts.getPrompt("ANSWER_NO_GUIDE_FLOW_DATA");
                    listener.onGenerated(channel, record);
                    channel.setProcessing(false);

                    SceneManager.getInstance().saveHistoryRecord(channel.getCode(), ModelConfig.AIXINLI,
                            convCtx, record);
                }
            });
            return AIGCStateCode.Ok;
        }

        // 设置问答引导流
        this.convCtx.setGuideFlow(guideFlow);
        // 设置子任务
        this.convCtx.setCurrentSubtask(Subtask.GuideFlow);

        this.service.getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                // 启动
                guideFlow.start(service);

                String answer = String.format(Prompts.getPrompt("FORMAT_ANSWER_STARTS_GUIDE_FLOW"),
                        polish(guideFlow.getInstruction()).trim(),
                        guideFlow.makeQuestion(true));

                ComplexContext complexContext = new ComplexContext();
                complexContext.setSubtask(Subtask.GuideFlow);

                GeneratingRecord record = new GeneratingRecord(query);
                record.answer = answer;
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
