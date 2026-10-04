/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.service.psychology.scene.subtask;

import cube.aigc.spi.AIGCHost;
import cube.aigc.ModelConfig;
import cube.aigc.psychology.composition.ConversationContext;
import cube.aigc.psychology.composition.ConversationRelation;
import cube.aigc.psychology.composition.Subtask;
import cube.common.entity.AIGCChannel;
import cube.common.entity.ComplexContext;
import cube.common.entity.GeneratingRecord;
import cube.common.state.AIGCStateCode;
import cube.aigc.listener.GenerateTextListener;
import cube.service.psychology.scene.SceneManager;

public class StopAppointmentSubtask extends ConversationSubtask {

    public StopAppointmentSubtask(AIGCHost host, AIGCChannel channel, String query,
                                  ComplexContext context, ConversationRelation relation, ConversationContext convCtx,
                                  GenerateTextListener listener) {
        super(Subtask.StopAppointment, host, channel, query, context, relation, convCtx, listener);
    }

    @Override
    public AIGCStateCode execute(Subtask roundSubtask) {
        // 取消子任务
        this.convCtx.deactivateSubtask();

        this.host.schedule("psychology-subtask", 0, new Runnable() {
            @Override
            public void run() {
                ComplexContext complexContext = new ComplexContext();
                complexContext.setSubtask(Subtask.StopAppointment);

                GeneratingRecord record = new GeneratingRecord(query);
                record.answer = "期待您下次预约我们的专业服务。";
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
