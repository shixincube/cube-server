/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.psychology.scene.subtask;

import cube.aigc.spi.AIGCHost;
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
import cube.aigc.listener.GenerateTextListener;
import cube.service.psychology.scene.SceneManager;

public class GuideFlowSubtask extends ConversationSubtask {

    public GuideFlowSubtask(AIGCHost host, AIGCChannel channel, String query,
                            ComplexContext context, ConversationRelation relation, ConversationContext convCtx,
                            GenerateTextListener listener) {
        super(Subtask.GuideFlow, host, channel, query, context, relation, convCtx, listener);
    }

    @Override
    public AIGCStateCode execute(Subtask roundSubtask) {
        final AbstractGuideFlow guideFlow = convCtx.getGuideFlow();
        if (null == guideFlow) {
            this.host.schedule("psychology-subtask", 0, new Runnable() {
                @Override
                public void run() {
                    GeneratingRecord record = new GeneratingRecord(query);
                    record.answer = GuideFlowSubtask.this.host.getGuidePrompt("ANSWER_NO_GUIDE_FLOW_DATA");
                    listener.onGenerated(channel, record);
                    channel.setProcessing(false);

                    SceneManager.getInstance().saveHistoryRecord(channel.getCode(), ModelConfig.BAIZE_UNIT,
                            convCtx, record);
                }
            });
            return AIGCStateCode.Ok;
        }

        if (roundSubtask == Subtask.StopGuideFlow) {
            // 取消子任务
            this.convCtx.deactivateSubtask();

            this.host.schedule("psychology-subtask", 0, new Runnable() {
                @Override
                public void run() {
                    guideFlow.stop();

                    ComplexContext complexContext = new ComplexContext();
                    complexContext.setSubtask(Subtask.StopGuideFlow);

                    String answer = fastPolish((null != guideFlow.getCurrentSection().getInterruption()) ?
                            guideFlow.getCurrentSection().getInterruption() :
                            GuideFlowSubtask.this.host.getGuidePrompt("ANSWER_INTERRUPT_GUIDE_FLOW"));

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
        AIGCStateCode stateCode = guideFlow.input(this.query, candidate);

        if (AIGCStateCode.Ok == stateCode && guideFlow.hasCompleted()) {
            this.host.schedule("psychology-subtask", 0, new Runnable() {
                @Override
                public void run() {
                    Logger.d(this.getClass(), "#execute - Guide flow completed: " + guideFlow.getName());

                    // 取消子任务
                    convCtx.deactivateSubtask();
                    // 停止
                    guideFlow.stop();
                }
            });
        }

        return stateCode;
    }
}
