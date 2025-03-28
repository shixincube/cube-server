/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.scene.subtask;

import cell.util.log.Logger;
import cube.aigc.psychology.composition.ConversationContext;
import cube.aigc.psychology.composition.ConversationRelation;
import cube.aigc.psychology.composition.Subtask;
import cube.common.entity.AIGCChannel;
import cube.common.entity.ComplexContext;
import cube.common.state.AIGCStateCode;
import cube.service.aigc.AIGCService;
import cube.service.aigc.listener.GenerateTextListener;
import cube.service.aigc.scene.SceneManager;
import cube.service.tokenizer.keyword.Keyword;
import cube.service.tokenizer.keyword.TFIDFAnalyzer;

import java.util.List;

public class SuperAdminSubtask extends ConversationSubtask {

    private class QueryKeyword {

        private final String[] words;

        private int hitCounts = 0;

        protected QueryKeyword(String[] words) {
            this.words = words;
        }

        protected void hit(List<Keyword> keywordList) {
            for (Keyword keyword : keywordList) {
                for (String word : this.words) {
                    if (word.equalsIgnoreCase(keyword.getWord())) {
                        ++this.hitCounts;
                    }
                }
            }
        }

        protected boolean hasHit() {
            return this.hitCounts == this.words.length;
        }
    }

    private QueryKeyword qkRandomFillScale = new QueryKeyword(new String[]{ "随机", "填写", "量表" });

    public SuperAdminSubtask(AIGCService service, AIGCChannel channel, String query, ComplexContext context,
                             ConversationRelation relation,
                             ConversationContext convCtx,
                             GenerateTextListener listener) {
        super(Subtask.SuperAdmin, service, channel, query, context, relation, convCtx, listener);
    }

    @Override
    public AIGCStateCode execute(Subtask roundSubtask) {
        this.service.getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                TFIDFAnalyzer analyzer = new TFIDFAnalyzer(service.getTokenizer());
                List<Keyword> keywordList = analyzer.analyze(query, 10);

                qkRandomFillScale.hit(keywordList);

                if (qkRandomFillScale.hasHit()) {
                    Logger.d(getClass(), "#execute - " + qkRandomFillScale.getClass().getName());

                    SceneManager.ScaleTrack scaleTrack = SceneManager.getInstance().getScaleTrack(channel.getCode());
                }
                else {
                    Logger.d(getClass(), "#execute - No query words");
                }
            }
        });
        return AIGCStateCode.Ok;
    }
}
