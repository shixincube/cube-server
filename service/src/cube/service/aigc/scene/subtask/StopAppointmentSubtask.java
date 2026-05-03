/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
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
import cube.service.aigc.guidance.Prompts;
import cube.service.aigc.listener.GenerateTextListener;
import cube.service.aigc.scene.SceneManager;

public class StopAppointmentSubtask extends ConversationSubtask {

    public StopAppointmentSubtask(AIGCService service, AIGCChannel channel, String query,
                                  ComplexContext context, ConversationRelation relation, ConversationContext convCtx,
                                  GenerateTextListener listener) {
        super(Subtask.StopAppointment, service, channel, query, context, relation, convCtx, listener);
    }

    @Override
    public AIGCStateCode execute(Subtask roundSubtask) {
        // 取消子任务
        this.convCtx.deactivateSubtask();

        this.service.getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                ComplexContext complexContext = new ComplexContext();
                complexContext.setSubtask(Subtask.StopAppointment);

                GeneratingRecord record = new GeneratingRecord(query);
                record.answer = "期待您下次预约我们的专业服务。";
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
