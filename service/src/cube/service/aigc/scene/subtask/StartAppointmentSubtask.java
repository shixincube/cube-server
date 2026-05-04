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
import cube.common.entity.Appointment;
import cube.common.entity.ComplexContext;
import cube.common.entity.GeneratingRecord;
import cube.common.state.AIGCStateCode;
import cube.service.aigc.AIGCService;
import cube.service.aigc.listener.GenerateTextListener;
import cube.service.aigc.scene.SceneManager;

public class StartAppointmentSubtask extends ConversationSubtask {

    public StartAppointmentSubtask(AIGCService service, AIGCChannel channel, String query,
                                   ComplexContext context, ConversationRelation relation, ConversationContext convCtx,
                                   GenerateTextListener listener) {
        super(Subtask.StartAppointment, service, channel, query, context, relation, convCtx, listener);
    }

    @Override
    public AIGCStateCode execute(Subtask roundSubtask) {
        Appointment appointment = new Appointment();
        // 激活子任务
        this.convCtx.activateSubtask(Subtask.Appointment);
        this.convCtx.setAppointment(appointment);

        this.service.getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                String answer = String.format("%s\n\n%s",
                        appointment.getInstruction(),
                        appointment.makeConversation());

                ComplexContext complexContext = new ComplexContext();
                complexContext.setSubtask(Subtask.Appointment);

                GeneratingRecord record = new GeneratingRecord(query);
                record.answer = answer;
                record.context = complexContext;
                listener.onGenerated(channel, record);
                channel.setProcessing(false);

                // 建立记忆
                convCtx.getSubtaskMemory().record(record);

                SceneManager.getInstance().saveHistoryRecord(channel.getCode(), ModelConfig.AIXINLI,
                        convCtx, record);
            }
        });

        return AIGCStateCode.Ok;
    }
}
