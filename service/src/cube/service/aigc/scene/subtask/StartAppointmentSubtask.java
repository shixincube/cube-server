/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.service.aigc.scene.subtask;

import cube.aigc.psychology.composition.ConversationContext;
import cube.aigc.psychology.composition.ConversationRelation;
import cube.aigc.psychology.composition.Subtask;
import cube.common.entity.AIGCChannel;
import cube.common.entity.ComplexContext;
import cube.common.state.AIGCStateCode;
import cube.service.aigc.AIGCService;
import cube.service.aigc.listener.GenerateTextListener;

public class StartAppointmentSubtask extends ConversationSubtask {

    public StartAppointmentSubtask(AIGCService service, AIGCChannel channel, String query,
                                   ComplexContext context, ConversationRelation relation, ConversationContext convCtx,
                                   GenerateTextListener listener) {
        super(Subtask.StartAppointment, service, channel, query, context, relation, convCtx, listener);
    }

    @Override
    public AIGCStateCode execute(Subtask roundSubtask) {
        // 设置子任务
        this.convCtx.setCurrentSubtask(Subtask.Appointment);

        return null;
    }
}
