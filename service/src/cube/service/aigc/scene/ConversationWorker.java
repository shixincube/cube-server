/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.scene;

import cell.util.log.Logger;
import cube.aigc.ModelConfig;
import cube.aigc.psychology.Resource;
import cube.aigc.psychology.composition.ConversationContext;
import cube.aigc.psychology.composition.ConversationRelation;
import cube.aigc.psychology.composition.Subtask;
import cube.common.entity.*;
import cube.common.state.AIGCStateCode;
import cube.service.aigc.AIGCService;
import cube.service.aigc.listener.GenerateTextListener;
import cube.service.aigc.listener.SemanticSearchListener;
import cube.service.aigc.scene.subtask.*;

import java.util.ArrayList;
import java.util.List;

public class ConversationWorker {

    private final static String CORPUS = "conversation";

    private AIGCService service;

    public ConversationWorker(AIGCService service) {
        this.service = service;
    }

    public AIGCStateCode work(String token, String channelCode, List<ConversationRelation> conversationRelationList,
                              String query, GenerateTextListener listener) {
        // 获取频道
        AIGCChannel channel = this.service.getChannel(channelCode);
        if (null == channel) {
            channel = this.service.createChannel(token, channelCode, channelCode);
        }

        // 获取单元
        AIGCUnit unit = this.service.selectUnitByName(ModelConfig.BAIZE_NEXT_UNIT);
        if (null == unit) {
            Logger.w(this.getClass(), "#work - Can NOT find unit \"" + ModelConfig.BAIZE_NEXT_UNIT + "\"");

            unit = this.service.selectUnitByName(ModelConfig.BAIZE_X_UNIT);
            if (null == unit) {
                return AIGCStateCode.UnitError;
            }
        }

        String prompt = PsychologyScene.getInstance().buildPrompt(conversationRelationList, query);
        if (null == prompt) {
            Logger.e(this.getClass(), "#work - Builds prompt failed");
            return AIGCStateCode.NoData;
        }

        // 使用指定模型生成结果
        this.service.generateText(channel, unit, query, prompt, new GeneratingOption(), null, 0,
                null, null, false, true, listener);

        return AIGCStateCode.Ok;
    }

    public AIGCStateCode work(AIGCChannel channel, final String query, ComplexContext context, ConversationRelation relation,
                              GenerateTextListener listener) {
        Logger.d(this.getClass(), "#work - channel code: " + channel.getCode());
        if (channel.isProcessing()) {
            // 频道正在工作
            return AIGCStateCode.Busy;
        }

        // 标记频道正在工作
        channel.setProcessing(true);

        // 获取对话上下文
        ConversationContext cc = SceneManager.getInstance().getConversationContext(channel.getCode());
        if (null == cc) {
            cc = new ConversationContext(relation, channel.getAuthToken());
            SceneManager.getInstance().putConversationContext(channel.getCode(), cc);
        }
        final ConversationContext convCtx = cc;

        // Query
        Subtask roundSubtask = Subtask.None;

        // 获取子任务
        Subtask subtask = convCtx.getCurrentSubtask();
        if (null == subtask) {
            // 匹配子任务
            subtask = this.matchSubtask(query);
        }
        else {
            // 本轮可能的任务，判断是否终止话题
            roundSubtask = this.matchSubtask(query);
            if (roundSubtask == Subtask.EndTopic) {
                convCtx.clearAll();
                this.service.getExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        ComplexContext complexContext = new ComplexContext(ComplexContext.Type.Simplex);
                        complexContext.setSubtask(Subtask.EndTopic);

                        GeneratingRecord record = new GeneratingRecord(query);
                        record.context = complexContext;
                        record.answer = polish(Resource.getInstance().getCorpus(CORPUS, "ANSWER_NEW_TOPIC"));
                        listener.onGenerated(channel, record);
                        channel.setProcessing(false);
                    }
                });
                return AIGCStateCode.Ok;
            }
        }

        if (Subtask.PredictPainting == subtask) {
            Logger.d(this.getClass(), "#work - Subtask - PredictPainting: " +
                    channel.getAuthToken().getCode() + "/" + channel.getCode());
            // 执行子任务
            PredictPaintingSubtask task = new PredictPaintingSubtask(this.service, channel, query, context,
                    relation, convCtx, listener);
            return task.execute(roundSubtask);
        }
        else if (Subtask.QueryReport == subtask) {
            Logger.d(this.getClass(), "#work - Subtask - QueryReport: " +
                    channel.getAuthToken().getCode() + "/" + channel.getCode());

            // 执行子任务
            QueryReportSubtask task = new QueryReportSubtask(this.service, channel, query, context,
                    relation, convCtx, listener);
            return task.execute(roundSubtask);
        }
        else if (Subtask.SelectReport == subtask) {
            Logger.d(this.getClass(), "#work - Subtask - SelectReport: " +
                    channel.getAuthToken().getCode() + "/" + channel.getCode());

            // 执行子任务
            SelectReportSubtask task = new SelectReportSubtask(this.service, channel, query, context,
                    relation, convCtx, listener);
            return task.execute(roundSubtask);
        }
        else if (Subtask.UnselectReport == subtask) {
            Logger.d(this.getClass(), "#work - Subtask - UnselectReport: " +
                    channel.getAuthToken().getCode() + "/" + channel.getCode());

            // 执行子任务
            UnselectReportSubtask task = new UnselectReportSubtask(this.service, channel, query, context,
                    relation, convCtx, listener);
            return task.execute(roundSubtask);
        }
        else if (Subtask.ShowPainting == subtask) {
            Logger.d(this.getClass(), "#work - Subtask - ShowPainting: " +
                    channel.getAuthToken().getCode() + "/" + channel.getCode());

            // 执行子任务
            ShowPaintingSubtask task = new ShowPaintingSubtask(this.service, channel, query, context,
                    relation, convCtx, listener);
            return task.execute(roundSubtask);
        }
        else if (Subtask.ShowIndicator == subtask) {
            Logger.d(this.getClass(), "#work - Subtask - ShowIndicator: " +
                    channel.getAuthToken().getCode() + "/" + channel.getCode());

            // 执行子任务
            ShowIndicatorSubtask task = new ShowIndicatorSubtask(this.service, channel, query, context,
                    relation, convCtx, listener);
            return task.execute(roundSubtask);
        }
        else if (Subtask.ShowPersonality == subtask) {
            Logger.d(this.getClass(), "#work - Subtask - ShowPersonality: " +
                    channel.getAuthToken().getCode() + "/" + channel.getCode());

            // 执行子任务
            ShowPersonalitySubtask task = new ShowPersonalitySubtask(this.service, channel, query, context,
                    relation, convCtx, listener);
            return task.execute(roundSubtask);
        }
        else if (Subtask.ShowCoT == subtask) {
            Logger.d(this.getClass(), "#work - Subtask - ShowCoT: " +
                    channel.getAuthToken().getCode() + "/" + channel.getCode());

            // 执行子任务
            ShowCoTSubtask task = new ShowCoTSubtask(this.service, channel, query, context,
                    relation, convCtx, listener);
            return task.execute(roundSubtask);
        }
        else {
            Logger.d(this.getClass(), "#work - General conversation");
            convCtx.clearCurrentPredict();
        }

        // 获取单元
        AIGCUnit unit = this.service.selectUnitByName(ModelConfig.BAIZE_NEXT_UNIT);
        if (null == unit) {
            Logger.w(this.getClass(), "#work - Can NOT find idle unit \"" + ModelConfig.BAIZE_NEXT_UNIT + "\"");

            unit = this.service.selectUnitByName(ModelConfig.BAIZE_X_UNIT);
            if (null == unit) {
                Logger.w(this.getClass(), "#work - Can NOT find unit \"" + ModelConfig.BAIZE_X_UNIT + "\"");
                channel.setProcessing(false);
                return AIGCStateCode.UnitError;
            }
        }

        String prompt = PsychologyScene.getInstance().buildPrompt(convCtx, query);
        if (null == prompt) {
            Logger.e(this.getClass(), "#work - Builds prompt failed");
            channel.setProcessing(false);
            return AIGCStateCode.NoData;
        }

        this.service.generateText(channel, unit, query, prompt, new GeneratingOption(), null, 0,
                null, null, false, true, new GenerateTextListener() {
                    @Override
                    public void onGenerated(AIGCChannel channel, GeneratingRecord record) {
                        convCtx.record(record);
                        listener.onGenerated(channel, record);
                    }

                    @Override
                    public void onFailed(AIGCChannel channel, AIGCStateCode stateCode) {
                        listener.onFailed(channel, stateCode);
                    }
                });

        return AIGCStateCode.Ok;
    }

    private String polish(String text) {
        AIGCUnit unit = this.service.selectIdleUnitByName(ModelConfig.BAIZE_X_UNIT);
        if (null == unit) {
            unit = this.service.selectIdleUnitByName(ModelConfig.BAIZE_NEXT_UNIT);
            if (null == unit) {
                unit = this.service.selectIdleUnitByName(ModelConfig.BAIZE_UNIT);
                if (null == unit) {
                    Logger.d(this.getClass(), "#polish - Can NOT find unit");
                    return text;
                }
            }
        }

        String prompt = String.format(Resource.getInstance().getCorpus("prompt", "FORMAT_POLISH"), text);
        GeneratingRecord result = this.service.syncGenerateText(unit, prompt, null, null, null);
        if (null == result) {
            return text;
        }
        return result.answer;
    }

    private Subtask matchSubtask(String query) {
        final List<QuestionAnswer> list = new ArrayList<>();

        // 尝试匹配子任务
        boolean success = this.service.semanticSearch(query, new SemanticSearchListener() {
            @Override
            public void onCompleted(String query, List<QuestionAnswer> questionAnswers) {
                list.addAll(questionAnswers);
                synchronized (list) {
                    list.notify();
                }
            }

            @Override
            public void onFailed(String query, AIGCStateCode stateCode) {
                synchronized (list) {
                    list.notify();
                }
            }
        });

        if (success) {
            synchronized (list) {
                try {
                    list.wait(30 * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        for (QuestionAnswer questionAnswer : list) {
            if (questionAnswer.getScore() < 0.8) {
                // 跳过得分低的问答
                continue;
            }
            else if (questionAnswer.getScore() >= 0.95) {
                List<String> answers = questionAnswer.getAnswers();
                for (String answer : answers) {
                    return Subtask.extract(answer);
                }
            }

            List<String> answers = questionAnswer.getAnswers();
            for (String answer : answers) {
                Subtask subtask = Subtask.extract(answer);
                if (Subtask.None != subtask) {
                    return subtask;
                }
            }
        }

        return Subtask.None;
    }
}
