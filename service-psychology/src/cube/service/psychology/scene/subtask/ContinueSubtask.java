/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.psychology.scene.subtask;

import cube.aigc.ModelConfig;
import cube.aigc.listener.GenerateTextListener;
import cube.aigc.psychology.Resource;
import cube.aigc.psychology.composition.ConversationContext;
import cube.aigc.psychology.composition.ConversationRelation;
import cube.aigc.psychology.composition.Subtask;
import cube.aigc.spi.AIGCHost;
import cube.common.entity.AIGCChannel;
import cube.common.entity.ComplexContext;
import cube.common.entity.GeneratingRecord;
import cube.common.state.AIGCStateCode;
import cube.service.psychology.scene.SceneManager;

public class ContinueSubtask extends ConversationSubtask {

    public ContinueSubtask(AIGCHost host, AIGCChannel channel, String query,
                           ComplexContext context, ConversationRelation relation, ConversationContext convCtx,
                           GenerateTextListener listener) {
        super(Subtask.Yes, host, channel, query, context, relation, convCtx, listener);
    }

    @Override
    public AIGCStateCode execute(Subtask roundSubtask) {
        this.host.schedule("psychology-subtask", 0, new Runnable() {
            @Override
            public void run() {
                GeneratingRecord record = new GeneratingRecord(query);
                record.answer = polish(Resource.getInstance().getCorpus(CORPUS, "ANSWER_CONTINUE"));
                listener.onGenerated(channel, record);
                channel.setProcessing(false);

                SceneManager.getInstance().saveHistoryRecord(channel.getCode(), ModelConfig.BAIZE_UNIT,
                        convCtx, record);
            }
        });
        return AIGCStateCode.Ok;
    }
}
