/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.service.psychology.scene.subtask;

import cube.aigc.spi.AIGCHost;
import cell.util.log.Logger;
import cube.aigc.ModelConfig;
import cube.aigc.psychology.composition.ConversationContext;
import cube.aigc.psychology.composition.ConversationRelation;
import cube.aigc.psychology.composition.Subtask;
import cube.common.entity.*;
import cube.common.state.AIGCStateCode;
import cube.aigc.listener.GenerateTextListener;
import cube.service.psychology.scene.SceneManager;

public class StartAppointmentSubtask extends ConversationSubtask {

    public StartAppointmentSubtask(AIGCHost host, AIGCChannel channel, String query,
                                   ComplexContext context, ConversationRelation relation, ConversationContext convCtx,
                                   GenerateTextListener listener) {
        super(Subtask.StartAppointment, host, channel, query, context, relation, convCtx, listener);
    }

    @Override
    public AIGCStateCode execute(Subtask roundSubtask) {
        User user = null;
        try {
            Contact contact = this.host.getContact(this.channel.getAuthToken().getCode());
            user = new User(contact.getContext());
        } catch (Exception e) {
            Logger.e(this.getClass(), "", e);
            return AIGCStateCode.Failure;
        }

        Appointment appointment = new Appointment(user.getId());
        // 激活子任务
        this.convCtx.activateSubtask(Subtask.Appointment);
        this.convCtx.setAppointment(appointment);

        this.host.schedule("psychology-subtask", 0, new Runnable() {
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

                SceneManager.getInstance().saveHistoryRecord(channel.getCode(), ModelConfig.BAIZE_UNIT,
                        convCtx, record);
            }
        });

        return AIGCStateCode.Ok;
    }
}
