/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.scene;

import cell.util.log.Logger;
import cube.aigc.ModelConfig;
import cube.aigc.complex.attachment.Attachment;
import cube.aigc.complex.attachment.FileAttachment;
import cube.aigc.psychology.Resource;
import cube.aigc.psychology.composition.ConversationContext;
import cube.aigc.psychology.composition.ConversationRelation;
import cube.aigc.psychology.composition.Scale;
import cube.aigc.psychology.composition.Subtask;
import cube.common.entity.*;
import cube.common.state.AIGCStateCode;
import cube.service.aigc.AIGCService;
import cube.service.aigc.listener.GenerateTextListener;
import cube.service.aigc.listener.SemanticSearchListener;
import cube.service.aigc.scene.subtask.*;
import cube.service.tokenizer.keyword.Keyword;
import cube.service.tokenizer.keyword.TFIDFAnalyzer;
import cube.util.TextUtils;

import java.util.ArrayList;
import java.util.List;

public class ConversationWorker {

    private final static String CORPUS = "conversation";

    private final static String CORPUS_PROMPT = "prompt";

    private final static String JUMP_POLISH = "润色";

    private final static String[] TEST_WORDS = new String[] {
            "评测", "测验", "测试", "评估"
    };

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
                null, null, true, listener);

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
            if (Logger.isDebugLevel()) {
                Logger.d(this.getClass(), "#work - New channel conversation context: " + channel.getAuthToken().getContactId());
            }
            cc = new ConversationContext(relation, channel.getAuthToken());
            SceneManager.getInstance().putConversationContext(channel.getCode(), cc);
        }
        final ConversationContext convCtx = cc;

        // try analysis subtask in query
        final Subtask roundSubtask = this.matchSubtask(query, convCtx);

        if (convCtx.isSuperAdmin()) {
            if (roundSubtask == Subtask.SuperAdmin) {
                // 退出超管模式
                Logger.d(this.getClass(), "#work - Exit super admin model: " +
                        channel.getAuthToken().getCode() + "/" + channel.getCode());
                convCtx.setSuperAdmin(false);
                this.service.getExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        GeneratingRecord record = new GeneratingRecord(query);
                        record.answer = Resource.getInstance().getCorpus(CORPUS, "ANSWER_EXIT_SUPER_ADMIN");
                        listener.onGenerated(channel, record);
                        channel.setProcessing(false);
                    }
                });
                return AIGCStateCode.Ok;
            }
            else {
                SuperAdminSubtask task = new SuperAdminSubtask(this.service, channel, query, context,
                        relation, convCtx, listener);
                return task.execute(roundSubtask);
            }
        }
        else {
            if (roundSubtask == Subtask.SuperAdmin) {
                // 进入超管模式
                Logger.d(this.getClass(), "#work - Enter super admin model: " +
                        channel.getAuthToken().getCode() + "/" + channel.getCode());
                convCtx.setSuperAdmin(true);
                this.service.getExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        GeneratingRecord record = new GeneratingRecord(query);
                        record.answer = Resource.getInstance().getCorpus(CORPUS, "ANSWER_ENTER_SUPER_ADMIN") +
                                SuperAdminSubtask.makeCommandText();
                        listener.onGenerated(channel, record);
                        channel.setProcessing(false);
                    }
                });
                return AIGCStateCode.Ok;
            }
        }

        // 获取子任务
        Subtask subtask = convCtx.getCurrentSubtask();
        if (null == subtask) {
            // 匹配子任务
            subtask = roundSubtask;
        }

        // 本轮可能的任务，判断是否终止话题
        if (roundSubtask == Subtask.EndTopic) {
            if (null != convCtx.getCurrentReport() && subtask != Subtask.PredictPainting) {
                // 已选中报告，取消选中
                subtask = Subtask.UnselectReport;
            }
            else {
                StringBuilder subtaskName = new StringBuilder();
                if (subtask == Subtask.PredictPainting) {
                    subtaskName.append("绘画投射测验");
                }
                else if (subtask == Subtask.StartQuestionnaire || subtask == Subtask.Questionnaire) {
                    subtaskName.append("量表测评");
                }
                else if (subtask == Subtask.GuideFlow) {
                    subtaskName.append("当前主题");
                }
                else {
                    subtaskName.append("当前话题");
                }

                // 取消所有上下文数据
                convCtx.cancelAll();
                this.service.getExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        ComplexContext complexContext = new ComplexContext();
                        complexContext.setSubtask(Subtask.EndTopic);

                        GeneratingRecord record = new GeneratingRecord(query);
                        record.context = complexContext;
                        record.answer = polish(String.format(
                                Resource.getInstance().getCorpus(CORPUS, "FORMAT_ANSWER_NEW_TOPIC"),
                                subtaskName.toString()));
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
        else if (Subtask.Questionnaire == subtask) {
            Logger.d(this.getClass(), "#work - Subtask - Questionnaire: " +
                    channel.getAuthToken().getCode() + "/" + channel.getCode());

            // 执行子任务
            QuestionnaireSubtask task = new QuestionnaireSubtask(this.service, channel, query, context,
                    relation, convCtx, listener);
            return task.execute(roundSubtask);
        }
        else if (Subtask.StartQuestionnaire == subtask) {
            Logger.d(this.getClass(), "#work - Subtask - StartQuestionnaire: " +
                    channel.getAuthToken().getCode() + "/" + channel.getCode());

            // 执行子任务
            StartQuestionnaireSubtask task = new StartQuestionnaireSubtask(this.service, channel, query, context,
                    relation, convCtx, listener);
            return task.execute(roundSubtask);
        }
        else if (Subtask.StopQuestionnaire == subtask) {
            Logger.d(this.getClass(), "#work - Subtask - StopQuestionnaire: " +
                    channel.getAuthToken().getCode() + "/" + channel.getCode());

            // 执行子任务
            StopQuestionnaireSubtask task = new StopQuestionnaireSubtask(this.service, channel, query, context,
                    relation, convCtx, listener);
            return task.execute(roundSubtask);
        }
        else if (Subtask.GuideFlow == subtask) {
            Logger.d(this.getClass(), "#work - Subtask - GuideFlow: " +
                    channel.getAuthToken().getCode() + "/" + channel.getCode());

            // 执行子任务
            GuideFlowSubtask task = new GuideFlowSubtask(this.service, channel, query, context,
                    relation, convCtx, listener);
            return task.execute(roundSubtask);
        }
        else if (Subtask.StartGuideFlow == subtask) {
            Logger.d(this.getClass(), "#work - Subtask - StartGuideFlow: " +
                    channel.getAuthToken().getCode() + "/" + channel.getCode());

            // 执行子任务
            StartGuideFlowSubtask task = new StartGuideFlowSubtask(this.service, channel, query, context,
                    relation, convCtx, listener);
            return task.execute(roundSubtask);
        }
        else if (Subtask.StopGuideFlow == subtask) {
            Logger.d(this.getClass(), "#work - Subtask - StopGuideFlow: " +
                    channel.getAuthToken().getCode() + "/" + channel.getCode());

            // 执行子任务
            StopGuideFlowSubtask task = new StopGuideFlowSubtask(this.service, channel, query, context,
                    relation, convCtx, listener);
            return task.execute(roundSubtask);
        }
        else if (Subtask.Yes == subtask) {
            Logger.d(this.getClass(), "#work - Subtask - Yes/Continue: " +
                    channel.getAuthToken().getCode() + "/" + channel.getCode());

            // 执行子任务
            ContinueSubtask task = new ContinueSubtask(this.service, channel, query, context,
                    relation, convCtx, listener);
            return task.execute(roundSubtask);
        }
        else {
            Logger.d(this.getClass(), "#work - General conversation");
            convCtx.cancelCurrentPredict();
        }

        PromptRevolver prompt = PsychologyScene.getInstance().revolve(convCtx, query);
        if (null == prompt) {
            Logger.e(this.getClass(), "#work - Builds prompt failed");
            channel.setProcessing(false);
            return AIGCStateCode.NoData;
        }

        if (null != prompt.result) {
            (new Thread() {
                @Override
                public void run() {
                    channel.setProcessing(false);
                    // 记录为一般性记录
                    convCtx.recordNormal(prompt.result);
                    listener.onGenerated(channel, prompt.result);
                }
            }).start();
            return AIGCStateCode.Ok;
        }

        // 获取单元
        String unitName = prompt.content.length() > 2000 || prompt.content.contains(JUMP_POLISH) ?
                ModelConfig.BAIZE_X_UNIT : ModelConfig.BAIZE_NEXT_UNIT;
        AIGCUnit unit = this.service.selectIdleUnitByName(unitName);
        if (null == unit) {
            Logger.w(this.getClass(), "#work - Can NOT find idle unit \"" + unitName + "\"");
            unit = this.service.selectUnitByName(ModelConfig.BAIZE_X_UNIT);
            if (null == unit) {
                Logger.w(this.getClass(), "#work - Can NOT find unit \"" + ModelConfig.BAIZE_X_UNIT + "\"");
                channel.setProcessing(false);
                return AIGCStateCode.UnitError;
            }
        }

        // 提取上下文里的文件资源为附录
        List<Attachment> attachments = null;
        if (context.hasResource(ComplexResource.Subject.File)) {
            attachments = new ArrayList<>();
            FileResource resource = context.getFileResource();
            attachments.add(new FileAttachment(resource.getFileLabel()));
        }

        // 从一般性对话中提取历史记录
        List<GeneratingRecord> histories = convCtx.getNormalHistories(3);

        this.service.generateText(channel, unit, query, prompt.content, new GeneratingOption(false),
                histories, 0, attachments, null, true, new GenerateTextListener() {
                    @Override
                    public void onGenerated(AIGCChannel channel, GeneratingRecord record) {
                        if (null != prompt.prefix) {
                            record.answer = prompt.prefix + record.answer;
                        }
                        if (null != prompt.postfix) {
                            record.answer += prompt.postfix;
                        }

                        // 记录为一般性记录
                        convCtx.recordNormal(record);
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
        AIGCUnit unit = this.service.selectUnitByName(ModelConfig.BAIZE_UNIT);
        if (null == unit) {
            unit = this.service.selectIdleUnitByName(ModelConfig.BAIZE_X_UNIT);
            if (null == unit) {
                unit = this.service.selectIdleUnitByName(ModelConfig.BAIZE_NEXT_UNIT);
                if (null == unit) {
                    Logger.d(this.getClass(), "#polish - Can NOT find unit");
                    return text;
                }
            }
        }

        String prompt = String.format(Resource.getInstance().getCorpus(CORPUS_PROMPT, "FORMAT_POLISH"), text);
        GeneratingRecord result = this.service.syncGenerateText(unit, prompt, null, null, null);
        if (null == result) {
            return text;
        }
        return result.answer;
    }

    private Subtask matchSubtask(String query, ConversationContext convCtx) {
        // 尝试匹配量表评测任务
        if (TextUtils.containsString(query, TEST_WORDS)) {
            TFIDFAnalyzer analyzer = new TFIDFAnalyzer(this.service.getTokenizer());
            List<Keyword> keywordList = analyzer.analyze(query, 10);
            boolean hit = false;
            for (Keyword keyword : keywordList) {
                if (keyword.getTfidfValue() > 0.1 && TextUtils.containsString(keyword.getWord(), TEST_WORDS)) {
                    hit = true;
                    break;
                }
            }
            if (hit) {
                // 匹配量表名
                List<Scale> scaleList = Resource.getInstance().listScales(convCtx.getAuthToken().getContactId());
                String lowerCaseQuery = query.toLowerCase();
                for (Scale scale : scaleList) {
                    if (lowerCaseQuery.contains(scale.name.toLowerCase()) ||
                            lowerCaseQuery.contains(scale.displayName.toLowerCase())) {
                        Logger.d(this.getClass(), "#matchSubtask - Matching \"StartQuestionnaire\" by analyzer: " + query);
                        return Subtask.StartQuestionnaire;
                    }
                }
            }
        }

        final List<QuestionAnswer> list = new ArrayList<>();

        // 尝试匹配子任务
        boolean success = this.service.semanticSearch(query.replaceAll("\\*\\*", ""), new SemanticSearchListener() {
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
            Logger.d(this.getClass(), "#matchSubtask - " + query + " : " +
                    questionAnswer.getAnswers().get(0) + " - " + questionAnswer.getScore());

            if (questionAnswer.getScore() < 0.86) {
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

                if (null != convCtx.getCurrentReport()) {
                    // 如果当前有上下文关联的报告，排除量表等任务
                    if (subtask == Subtask.StartQuestionnaire || subtask == Subtask.StartGuideFlow ||
                            subtask == Subtask.StopQuestionnaire || subtask == Subtask.StopGuideFlow) {
                        continue;
                    }
                }

                if (Subtask.None != subtask) {
                    return subtask;
                }
            }
        }

        return Subtask.None;
    }
}
