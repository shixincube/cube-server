/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.scene.subtask;

import cell.util.Utils;
import cell.util.log.Logger;
import cube.aigc.psychology.app.Link;
import cube.aigc.psychology.composition.ConversationContext;
import cube.aigc.psychology.composition.ConversationRelation;
import cube.aigc.psychology.composition.Question;
import cube.aigc.psychology.composition.Subtask;
import cube.common.entity.AIGCChannel;
import cube.common.entity.ComplexContext;
import cube.common.entity.GeneratingRecord;
import cube.common.state.AIGCStateCode;
import cube.service.aigc.AIGCService;
import cube.service.aigc.listener.GenerateTextListener;
import cube.service.aigc.scene.SceneManager;
import cube.service.tokenizer.keyword.Keyword;
import cube.service.tokenizer.keyword.TFIDFAnalyzer;

import java.util.List;

public class SuperAdminSubtask extends ConversationSubtask {

    private final static String[] sPromptRandomFillScaleWords = new String[]{ "随机", "填写", "量表" };

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

        @Override
        public String toString() {
            StringBuilder buf = new StringBuilder();
            for (String word : words) {
                buf.append(word);
            }
            return buf.toString();
        }
    }

    private QueryKeyword qkRandomFillScale = new QueryKeyword(sPromptRandomFillScaleWords);

    public SuperAdminSubtask(AIGCService service, AIGCChannel channel, String query, ComplexContext context,
                             ConversationRelation relation,
                             ConversationContext convCtx,
                             GenerateTextListener listener) {
        super(Subtask.SuperAdmin, service, channel, query, context, relation, convCtx, listener);
    }

    public static String makeCommandText() {
        StringBuilder buf = new StringBuilder();

        buf.append("* [");
        for (String word : sPromptRandomFillScaleWords) {
            buf.append(word.trim());
        }
        buf.append("](").append(Link.PromptDirect);
        for (String word : sPromptRandomFillScaleWords) {
            buf.append(word.trim());
        }
        buf.append(")\n\n");

        return buf.toString();
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
                    Logger.d(getClass(), "#execute - " + qkRandomFillScale.toString());

                    SceneManager.ScaleTrack scaleTrack = SceneManager.getInstance().getScaleTrack(channel.getCode());
                    if (null == scaleTrack) {
                        GeneratingRecord record = new GeneratingRecord(query);
                        record.answer = "没有找到可用的量表";
                        listener.onGenerated(channel, record);
                        channel.setProcessing(false);
                        return;
                    }

                    for (Question question : scaleTrack.scale.getQuestions()) {
                        int choiceIndex = Utils.randomInt(0, question.answers.size() - 1);
                        String choice = question.answers.get(choiceIndex).code;
                        question.chooseAnswer(choice);
                    }
                    // 更新游标
                    scaleTrack.questionCursor = scaleTrack.scale.getQuestions().size();

                    GeneratingRecord record = new GeneratingRecord(query);
                    record.answer = "完成对量表\"" + scaleTrack.scale.displayName + "\"的随机回答。\n\n";
                    record.answer += "共回答**" + scaleTrack.scale.getQuestions().size() + "**道题，";
                    if (scaleTrack.scale.isComplete()) {
                        record.answer += "量表已完成答题。";
                    }
                    else {
                        record.answer += "量表未完成答题。";
                    }
                    listener.onGenerated(channel, record);
                    channel.setProcessing(false);
                }
                else {
                    Logger.d(getClass(), "#execute - No query words");

                    GeneratingRecord record = new GeneratingRecord(query);
                    record.answer = "没有匹配的指令：" + query;
                    listener.onGenerated(channel, record);
                    channel.setProcessing(false);
                }
            }
        });
        return AIGCStateCode.Ok;
    }
}
