/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.scene.subtask;

import cell.util.log.Logger;
import cube.aigc.ModelConfig;
import cube.aigc.guidance.*;
import cube.aigc.psychology.composition.ConversationContext;
import cube.aigc.psychology.composition.ConversationRelation;
import cube.aigc.psychology.composition.Subtask;
import cube.common.entity.AIGCChannel;
import cube.common.entity.ComplexContext;
import cube.common.entity.GeneratingRecord;
import cube.common.state.AIGCStateCode;
import cube.service.aigc.AIGCService;
import cube.service.aigc.guidance.Prompts;
import cube.service.aigc.listener.GenerateTextListener;
import cube.service.aigc.scene.SceneManager;

public class GuideFlowSubtask extends ConversationSubtask {

    public GuideFlowSubtask(AIGCService service, AIGCChannel channel, String query,
                            ComplexContext context, ConversationRelation relation, ConversationContext convCtx,
                            GenerateTextListener listener) {
        super(Subtask.StartGuideFlow, service, channel, query, context, relation, convCtx, listener);
    }

    @Override
    public AIGCStateCode execute(Subtask roundSubtask) {
        final AbstractGuideFlow guideFlow = convCtx.getGuideFlow();
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

        if (roundSubtask == Subtask.StopGuideFlow) {
            // 清空子任务
            this.convCtx.cancelCurrentSubtask();

            this.service.getExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    guideFlow.stop();

                    ComplexContext complexContext = new ComplexContext(ComplexContext.Type.Lightweight);
                    complexContext.setSubtask(Subtask.StopGuideFlow);

                    String answer = fastPolish((null != guideFlow.getInterruption()) ?
                            guideFlow.getInterruption() :
                            Prompts.getPrompt("ANSWER_INTERRUPT_GUIDE_FLOW"));

                    GeneratingRecord record = new GeneratingRecord(query);
                    record.answer = answer;
                    record.context = complexContext;
                    channel.setProcessing(false);

                    SceneManager.getInstance().saveHistoryRecord(channel.getCode(), ModelConfig.AIXINLI,
                            convCtx, record);
                }
            });
            return AIGCStateCode.Ok;
        }

        guideFlow.setListener(new GuideListener() {
            @Override
            public void onResponse(AbstractGuideFlow guideFlow, GeneratingRecord response) {
                listener.onGenerated(channel, response);
                channel.setProcessing(false);
            }
        });

        Logger.d(this.getClass(), "#execute - The round subtask is " + roundSubtask.name());

        Answer candidate = null;
        Question question = guideFlow.getCurrentQuestion();
        if (null != question.answers) {
            for (Answer answer : question.answers) {
                if (roundSubtask == Subtask.No && answer.code.equalsIgnoreCase("false")) {
                    candidate = answer;
                    break;
                }
                else if (roundSubtask == Subtask.Yes && answer.code.equalsIgnoreCase("true")) {
                    candidate = answer;
                    break;
                }
            }
        }
        else if (null != question.answerGroups) {
            AnswerGroup answerGroup = question.getAnswerGroupByState(AnswerGroup.STATE_ANSWERING);
            for (Answer answer : answerGroup.answers) {
                if (roundSubtask == Subtask.No && answer.code.equalsIgnoreCase("false")) {
                    candidate = answer;
                    break;
                }
                else if (roundSubtask == Subtask.Yes && answer.code.equalsIgnoreCase("true")) {
                    candidate = answer;
                    break;
                }
            }
        }

        // 进行输入处理
        AIGCStateCode stateCode = guideFlow.input(query, candidate);

        if (AIGCStateCode.Ok == stateCode && guideFlow.hasCompleted()) {
            this.service.getExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    Logger.d(this.getClass(), "#execute - Guide flow completed: " + guideFlow.getName());

                    // 取消子任务
                    convCtx.cancelCurrentSubtask();
                    // 停止
                    guideFlow.stop();
                }
            });
        }

        return stateCode;
    }
}
