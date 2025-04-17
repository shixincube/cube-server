/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.scene.subtask;

import cell.util.log.Logger;
import cube.aigc.ModelConfig;
import cube.aigc.psychology.Attribute;
import cube.aigc.psychology.Resource;
import cube.aigc.psychology.composition.ConversationContext;
import cube.aigc.psychology.composition.ConversationRelation;
import cube.aigc.psychology.composition.Scale;
import cube.aigc.psychology.composition.Subtask;
import cube.common.entity.AIGCChannel;
import cube.common.entity.ComplexContext;
import cube.common.entity.GeneratingRecord;
import cube.common.state.AIGCStateCode;
import cube.service.aigc.AIGCService;
import cube.service.aigc.listener.GenerateTextListener;
import cube.service.aigc.scene.PsychologyScene;
import cube.service.aigc.scene.SceneManager;
import cube.service.tokenizer.keyword.TFIDFAnalyzer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class StartQuestionnaireSubtask extends ConversationSubtask {

    public StartQuestionnaireSubtask(AIGCService service, AIGCChannel channel, String query,
                                     ComplexContext context, ConversationRelation relation, ConversationContext convCtx,
                                     GenerateTextListener listener) {
        super(Subtask.StartQuestionnaire, service, channel, query, context, relation, convCtx, listener);
    }

    @Override
    public AIGCStateCode execute(Subtask roundSubtask) {
        // 尝试匹配量表
        String scaleName = this.extractScaleName();

        // 生成
        Scale scale = PsychologyScene.getInstance().generateScale(channel.getAuthToken().getContactId(), scaleName,
                new Attribute("male", 30, false));
        if (null == scale) {
            Logger.w(this.getClass(), "#execute - Load scale failed: " + channel.getAuthToken().getCode());

            this.service.getExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    GeneratingRecord record = new GeneratingRecord(query);
                    record.answer = Resource.getInstance().getCorpus(CORPUS, "ANSWER_FAILED");
                    listener.onGenerated(channel, record);
                    channel.setProcessing(false);

                    SceneManager.getInstance().saveHistoryRecord(channel.getCode(), ModelConfig.AIXINLI,
                            convCtx, record);
                }
            });
            return AIGCStateCode.Ok;
        }

        // 添加量表
        SceneManager.getInstance().setScale(this.channel.getCode(), scale);
        // 设置问卷子任务
        this.convCtx.setCurrentSubtask(Subtask.Questionnaire);

        this.service.getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                ComplexContext complexContext = new ComplexContext(ComplexContext.Type.Lightweight);
                complexContext.setSubtask(Subtask.StartQuestionnaire);

                GeneratingRecord record = new GeneratingRecord(query);
                record.answer = polish(String.format(
                        Resource.getInstance().getCorpus(CORPUS, "FORMAT_ANSWER_START_QUESTIONNAIRE"),
                        scale.instruction));

                record.answer += "\n\n您可以点击 [现在开始](aixinli://prompt.direct/开始。) 让我们立即开始。";

                record.context = complexContext;
                listener.onGenerated(channel, record);
                channel.setProcessing(false);

                SceneManager.getInstance().saveHistoryRecord(channel.getCode(), ModelConfig.AIXINLI,
                        convCtx, record);
            }
        });
        return AIGCStateCode.Ok;
    }

    private String extractScaleName() {
        List<Scale> scaleList = Resource.getInstance().listScales(channel.getAuthToken().getContactId());
        List<AtomicInteger> hitCountList = new ArrayList<>();
        for (int i = 0; i < scaleList.size(); ++i) {
            hitCountList.add(new AtomicInteger());
        }

        TFIDFAnalyzer analyzer = new TFIDFAnalyzer(this.service.getTokenizer());
        List<String> queryWords = analyzer.analyzeOnlyWords(this.query, 10);

        for (int i = 0; i < scaleList.size(); ++i) {
            Scale scale = scaleList.get(i);
            List<String> nameWords = analyzer.analyzeOnlyWords(scale.displayName, 5);

            for (String word : queryWords) {
                for (String nameWord : nameWords) {
                    if (word.equalsIgnoreCase(nameWord)) {
                        hitCountList.get(i).incrementAndGet();
                    }
                }
            }
        }

        int hitIndex = -1;
        int maxValue = 0;
        for (int i = 0; i < hitCountList.size(); ++i) {
            if (hitCountList.get(i).get() > maxValue) {
                maxValue = hitCountList.get(i).get();
                hitIndex = i;
            }
        }

        if (hitIndex >= 0 && hitCountList.get(hitIndex).get() >= 1) {
            return scaleList.get(hitIndex).name;
        }

        return "HAMA";
    }
}
