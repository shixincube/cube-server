/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.scene;

import cell.util.log.Logger;
import cube.aigc.ModelConfig;
import cube.aigc.psychology.*;
import cube.aigc.psychology.algorithm.IndicatorRate;
import cube.aigc.psychology.algorithm.KnowledgeStrategy;
import cube.aigc.psychology.composition.*;
import cube.common.entity.GeneratingRecord;
import cube.common.entity.QuestionAnswer;
import cube.common.state.AIGCStateCode;
import cube.service.aigc.AIGCService;
import cube.service.aigc.listener.SemanticSearchListener;
import cube.service.tokenizer.Tokenizer;
import cube.service.tokenizer.keyword.TFIDFAnalyzer;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class QueryRevolver {

    private final static SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyy年MM月dd日HH时mm分ss秒");

    private final static String[] sKeywordPersonality = new String[] {
            "人格", "性格", "个性", "人性", "做人", "作人", "为人", "人格特质", "人格特性"
    };

    private final static String[] sKeywordWayOfThinking = new String[] {
            "思维",
            "思维方式",
            "思维模式",
            "思考方式",
            "思考模式",
            "思维特点",
            "思维风格",
    };

    private final static String[] sKeywordCommunicationStyle = new String[] {
            "沟通",
            "沟通风格",
            "沟通模式",
            "沟通方式",
            "沟通特点"
    };

    private final static String[] sKeywordWorkEnvironment = new String[] {
            "工作环境偏好",
            "工作环境喜好",
            "工作环境倾向",
            "工作环境"
    };

    private final static String[] sKeywordManagementRecommendation = new String[] {
            "管理建议",
            "管理方式",
            "管理方法",
            "管理"
    };

    private final static String[] sKeywordSuggestion = new String[] {
            "建议",
            "改善",
            "缓解",
            "解决"
    };

    private final static String[] sPaintingDesc = new String[] {
            "画", "画面", "图画", "图像", "照片", "绘画", "看"
    };

    /**
     * 症状得分关键词表。
     */
    private final static String[] sSymptomScoreWords = new String[] {
            "躯体化", "somatization",
            "强迫", "强迫症", "obsession",
            "人际关系", "人际敏感", "interpersonal",
            "抑郁", "抑郁症", "抑郁倾向", "depression",
            "焦虑", "焦虑情绪", "anxiety",
            "敌对", "敌意", "hostile",
            "恐怖", "horror",
            "偏执", "paranoid",
            "精神病性", "psychosis",
            "睡眠障碍和饮食不良", "sleep diet"
    };

    /**
     * Lazy 机制用于保护关键问题可以从数据集中被选取。
     */
    private final static String[] sLazyQuery = new String[] {
            "如何进行绘画评测"
    };

    private AIGCService service;

    private Tokenizer tokenizer;

    private PsychologyStorage storage;

    public QueryRevolver(AIGCService service, PsychologyStorage storage) {
        this.service = service;
        this.tokenizer = service.getTokenizer();
        this.storage = storage;
    }

    public String generatePrompt(ConversationContext context, String query) {
        final StringBuilder result = new StringBuilder();

        AtomicBoolean hitLazy = new AtomicBoolean(false);
        final List<QuestionAnswer> questionAnswerList = new ArrayList<>();
        boolean success = this.service.semanticSearch(query, new SemanticSearchListener() {
            @Override
            public void onCompleted(String query, List<QuestionAnswer> questionAnswers) {
                for (QuestionAnswer questionAnswer : questionAnswers) {
                    if (questionAnswer.getScore() < 0.8) {
                        // 排除得分较低答案
                        continue;
                    }

                    if (!hitLazy.get()) {
                        // 判断 Lazy 句子，如果是 Lazy 句子，则仅润色
                        for (String q : questionAnswer.getQuestions()) {
                            for (String lazy : sLazyQuery) {
                                if (fastSentenceSimilarity(q, lazy) >= 0.75) {
                                    hitLazy.set(true);
                                    break;
                                }
                            }
                            if (hitLazy.get()) {
                                break;
                            }
                        }
                    }

                    List<String> answers = questionAnswer.getAnswers();
                    for (String answer : answers) {
                        Subtask subtask = Subtask.extract(answer);
                        if (Subtask.None == subtask) {
                            // 不是子任务
                            if (!questionAnswerList.contains(questionAnswer)) {
                                questionAnswerList.add(questionAnswer);
                                break;
                            }
                        }
                    }

                    if (hitLazy.get()) {
                        break;
                    }
                }
                synchronized (result) {
                    result.notify();
                }
            }

            @Override
            public void onFailed(String query, AIGCStateCode stateCode) {
                synchronized (result) {
                    result.notify();
                }
            }
        });

        if (success) {
            synchronized (result) {
                try {
                    result.wait(30 * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        final int wordLimit = ModelConfig.BAIZE_NEXT_CONTEXT_LIMIT - 60;

        if (hitLazy.get()) {
            Logger.d(this.getClass(), "#generatePrompt - Hit lazy: " + query);

            if (!questionAnswerList.isEmpty()) {
                // 清空
                result.delete(0, result.length());

                for (QuestionAnswer qa : questionAnswerList) {
                    for (String answer : qa.getAnswers()) {
                        result.append(answer).append("\n\n");
                    }
                }
                String text = String.format(Resource.getInstance().getCorpus("prompt", "FORMAT_POLISH"),
                        result.toString());
                result.delete(0, result.length());
                result.append(text).append("\n");
            }
            else {
                result.append(query).append("\n");
            }
        }
        else {
            boolean hasReportData = false;
            if (null != context.getCurrentReport() && !context.getCurrentReport().isNull()) {
                hasReportData = true;
                PaintingReport report = context.getCurrentReport();
                result.append("已知评测数据：\n\n");
                result.append("此评测数据是由AiXinLi模型生成的，采用的评测方法是“房树人”绘画投射测试。");
                result.append("评测数据的受测人是匿名的，");
                result.append("年龄是：").append(report.getAttribute().age).append("岁，");
                result.append("性别是：").append(report.getAttribute().getGenderText()).append("性。\n\n");
                result.append("评测日期是：").append(formatReportDate(report)).append("。\n\n");
                result.append("受测人的心理特征摘要如下：");
                result.append(report.getSummary());
                result.append("\n\n");

                result.append("受测人主要心理特征描述如下：\n");
                List<String> symptomContent = this.extractEvaluationContent(report);
                for (String content : symptomContent) {
                    result.append(content).append("\n\n");
                }

                // 画面特征
                result.append(this.tryGeneratePaintingFeature(report, query));

                // 指标数据
                result.append(this.tryGenerateFactorDesc(report, query));

                if (result.length() < wordLimit) {
                    // 尝试生成人格数据
                    result.append(this.tryGeneratePersonality(report, query));
                }

                if (result.length() < wordLimit) {
                    // 尝试生成知识片段
                    result.append("\n");
                    result.append(this.generateKnowledgeFragment(report, query));
                }
            }

            if (!hasReportData) {
                // 不带报告数据
                // 问题是否和心理学相关
                String prompt = String.format(
                        Resource.getInstance().getCorpus("prompt", "FORMAT_QUESTION_PSYCHOLOGY_POSSIBILITY"),
                        query);
                GeneratingRecord queryResponse = this.service.syncGenerateText(ModelConfig.BAIZE_UNIT, prompt,
                        null, null, null);
                if (queryResponse.answer.equals("不相关")) {
                    Logger.d(this.getClass(), "#generatePrompt - No psychology question: " + query);
                    result.delete(0, result.length());

                    if (!questionAnswerList.isEmpty()) {
                        result.append("已知知识点：\n\n");
                        for (QuestionAnswer qa : questionAnswerList) {
                            for (String answer : qa.getAnswers()) {
                                result.append(answer).append("\n\n");
                            }
                        }
                        result.append("根据以上知识点，专业地回答问题。如果无法从中得到答案，请说“暂时没有获得足够的相关信息。”，");
                        result.append("不允许在答案中添加编造成分。");
                        result.append("问题是：").append(query).append("\n");
                    }
                }
                else {
                    if (!questionAnswerList.isEmpty()) {
                        if (result.length() == 0) {
                            result.append("已知知识点：\n\n");
                        }
                        else {
                            result.append("\n下面的内容是一些与问题相关的知识：\n");
                        }

                        for (QuestionAnswer qa : questionAnswerList) {
                            String question = qa.getQuestions().get(0);
                            result.append("**").append(question).append("** ：\n\n");

                            for (String answer : qa.getAnswers()) {
                                result.append(this.filterPersonalityDescription(answer)).append("\n\n");
                                if (result.length() >= wordLimit) {
                                    break;
                                }
                            }
                            if (result.length() >= wordLimit) {
                                break;
                            }
                        }
                    }

                    if (result.length() > 0) {
                        result.append("根据以上信息，专业地回答问题。如果无法从中得到答案，请说“暂时没有获得足够的相关信息。”，");
                        result.append("不允许在答案中添加编造成分。");
                        result.append("问题是：").append(query).append("\n");
                    }
                }
            }
            else {
                // 带报告数据
                if (result.length() > 0) {
                    result.append("根据以上信息，专业地回答问题。如果无法从中得到答案，请说“暂时没有获得足够的相关信息。”，");
                    result.append("不允许在答案中添加编造成分，可以以“根据您的提问”开头。");
                    result.append("问题是：").append(query).append("\n");
                }
            }

            if (result.length() == 0) {
                result.append("专业地回答问题，在答案最后加入一句：“\n\n我的更多功能您可以通过：**[功能介绍](aixinli://prompt.direct/请你介绍一下你自己。)** 进行了解。”，问题是：");
                result.append(query).append("\n");
            }
        }

        return result.toString();
    }

    public String generatePrompt(ConversationRelation relation, Report report, String query) {
        final StringBuilder result = new StringBuilder();

        AtomicBoolean hitLazy = new AtomicBoolean(false);
        final List<QuestionAnswer> questionAnswerList = new ArrayList<>();
        boolean success = this.service.semanticSearch(query, new SemanticSearchListener() {
            @Override
            public void onCompleted(String query, List<QuestionAnswer> questionAnswers) {
                for (QuestionAnswer questionAnswer : questionAnswers) {
                    if (questionAnswer.getScore() < 0.8) {
                        // 排除得分较低答案
                        continue;
                    }

                    if (!hitLazy.get()) {
                        // 判断 Lazy 句子，如果是 Lazy 句子，则仅润色
                        for (String q : questionAnswer.getQuestions()) {
                            for (String lazy : sLazyQuery) {
                                if (fastSentenceSimilarity(q, lazy) >= 0.75) {
                                    hitLazy.set(true);
                                    break;
                                }
                            }
                            if (hitLazy.get()) {
                                break;
                            }
                        }
                    }

                    List<String> answers = questionAnswer.getAnswers();
                    for (String answer : answers) {
                        Subtask subtask = Subtask.extract(answer);
                        if (Subtask.None == subtask) {
                            // 不是子任务
                            if (!questionAnswerList.contains(questionAnswer)) {
                                questionAnswerList.add(questionAnswer);
                                break;
                            }
                        }
                    }

                    if (hitLazy.get()) {
                        break;
                    }
                }
                synchronized (result) {
                    result.notify();
                }
            }

            @Override
            public void onFailed(String query, AIGCStateCode stateCode) {
                synchronized (result) {
                    result.notify();
                }
            }
        });

        if (success) {
            synchronized (result) {
                try {
                    result.wait(30 * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        final int wordLimit = ModelConfig.BAIZE_NEXT_CONTEXT_LIMIT - 60;

        if (hitLazy.get()) {
            Logger.d(this.getClass(), "#generatePrompt - Hit lazy: " + query);

            if (!questionAnswerList.isEmpty()) {
                // 清空
                result.delete(0, result.length());

                for (QuestionAnswer qa : questionAnswerList) {
                    for (String answer : qa.getAnswers()) {
                        result.append(answer).append("\n\n");
                    }
                }
                String text = String.format(Resource.getInstance().getCorpus("prompt", "FORMAT_POLISH"),
                        result.toString());
                result.delete(0, result.length());
                result.append(text).append("\n");
            }
            else {
                result.append(query).append("\n");
            }
        }
        else {
            if (report instanceof PaintingReport) {
                PaintingReport paintingReport = (PaintingReport) report;

                result.append("已知评测数据：\n\n");
                result.append("此评测数据是由AiXinLi模型生成的，采用的评测方法是“房树人”绘画投射测试。");
                result.append("评测数据的受测人是").append(this.fixName(relation.name, report.getAttribute())).append("，");
                result.append("年龄是：").append(report.getAttribute().age).append("岁，");
                result.append("性别是：").append(report.getAttribute().getGenderText()).append("性。\n\n");
                result.append("评测日期是：").append(formatReportDate(paintingReport)).append("。\n\n");
                result.append("受测人的心理特征摘要如下：");
                result.append(report.getSummary());
                result.append("\n\n");

                result.append("受测人主要心理特征描述如下：\n");
                List<String> symptomContent = this.extractEvaluationContent(paintingReport);
                for (String content : symptomContent) {
                    result.append(content).append("\n\n");
                }

                // 画面特征
                result.append(this.tryGeneratePaintingFeature(paintingReport, query));

                // 指标数据
                result.append(this.tryGenerateFactorDesc(paintingReport, query));

                if (result.length() < wordLimit) {
                    // 尝试生成人格数据
                    result.append(this.tryGeneratePersonality(paintingReport, query));
                }

                if (result.length() < wordLimit) {
                    // 尝试生成知识片段
                    result.append("\n");
                    result.append(this.generateKnowledgeFragment(report, query));
                }
            }
            else if (report instanceof ScaleReport) {
                ScaleReport scaleReport = (ScaleReport) report;
                if (null != scaleReport.getScale()) {
                    result.append(scaleReport.getScale().displayName);
                    result.append("量表的测验结果是：\n");
                }
                else {
                    result.append("受测人的心理表现有：\n");
                }

                for (ScaleFactor factor : scaleReport.getFactors()) {
                    result.append("\n* ").append(factor.description);
                    if (result.length() > ModelConfig.EXTRA_LONG_CONTEXT_LIMIT) {
                        break;
                    }
                }

                result.append("\n对于上述心理表现给出以下建议：\n");
                for (ScaleFactor factor : scaleReport.getFactors()) {
                    result.append("\n* ").append(factor.suggestion);
                    if (result.length() > ModelConfig.EXTRA_LONG_CONTEXT_LIMIT) {
                        break;
                    }
                }
                result.append("\n");
            }
        }

        if (result.length() > 0) {
            result.append("根据以上信息，专业地回答问题。如果无法从中得到答案，请说“暂时没有获得足够的相关信息。”，");
            result.append("不允许在答案中添加编造成分，可以以“根据您的提问”开头。");
            result.append("问题是：").append(query).append("\n");
        }
        else {
            result.append("专业地回答问题。”，问题是：");
            result.append(query).append("\n");
        }

        return result.toString();
    }

    public String generatePrompt(List<ConversationRelation> relations, List<Report> reports, String query) {
        StringBuilder result = new StringBuilder();

        result.append("已知评测信息：\n\n");
        result.append("受测人有").append(relations.size()).append("份数据信息如下：\n\n");

        for (int i = 0; i < relations.size(); ++i) {
            ConversationRelation relation = relations.get(i);
            Report report = reports.get(i);
            result.append("# 评测数据").append(i + 1);
            result.append("受测人的名称是：").append(this.fixName(
                    relation.name,
                    report.getAttribute())).append("，");
            result.append("年龄是：").append(report.getAttribute().age).append("岁，");
            result.append("性别是：").append(report.getAttribute().getGenderText()).append("性。\n");

            if (report instanceof PaintingReport) {
                result.append("受测人心理状态如下：\n");

                PaintingReport paintingReport = (PaintingReport) report;
                List<String> symptomContent = this.extractEvaluationContent(paintingReport);
                for (String content : symptomContent) {
                    result.append(content).append("\n");
                }
                result.append("\n");

                if (result.length() < ModelConfig.EXTRA_LONG_CONTEXT_LIMIT) {
                    result.append("\n");
                    result.append("受测人的大五人格画像是").append(paintingReport.getEvaluationReport()
                            .getPersonalityAccelerator().getBigFivePersonality().getDisplayName()).append("。\n");
                    // 性格特点
                    result.append(paintingReport.getEvaluationReport()
                                .getPersonalityAccelerator().getBigFivePersonality().getDisplayName()).append("的性格特点：");
                    result.append(this.filterPersonalityDescription(paintingReport.getEvaluationReport()
                                .getPersonalityAccelerator().getBigFivePersonality().getDescription()));
                }

                if (result.length() < ModelConfig.EXTRA_LONG_CONTEXT_LIMIT) {
                    result.append("\n");
                    result.append(this.generateKnowledgeFragment(report, query));
                }
            }
            else if (report instanceof ScaleReport) {
                ScaleReport scaleReport = (ScaleReport) report;
                if (null != scaleReport.getScale()) {
                    result.append(scaleReport.getScale().displayName);
                    result.append("量表的测验结果是：\n");
                }
                else {
                    result.append("受测人的心理表现有：\n");
                }

                for (ScaleFactor factor : scaleReport.getFactors()) {
                    result.append("\n* ").append(factor.description);
                    if (result.length() > ModelConfig.EXTRA_LONG_CONTEXT_LIMIT) {
                        break;
                    }
                }

                result.append("\n对于上述心理表现给出以下建议：\n");
                for (ScaleFactor factor : scaleReport.getFactors()) {
                    result.append("\n* ").append(factor.suggestion);
                    if (result.length() > ModelConfig.EXTRA_LONG_CONTEXT_LIMIT) {
                        break;
                    }
                }
                result.append("\n");
            }

            if (result.substring(result.length() - 1).equals("\n")) {
                result.append("\n");
            }
            else {
                result.append("\n\n");
            }
        }

        result.append("\n根据上述已知信息，专业地来回答用户的问题。");

        for (String word : sKeywordSuggestion) {
            if (query.contains(word)) {
                result.append("答案里不要出现药品信息。");
                break;
            }
        }

        result.append("问题是：");
        result.append(query);

        return this.filterSubjectNoun(result.toString(), reports.get(0).getAttribute());
    }

    public GeneratingRecord generateSupplement(ConversationRelation relation, Report report, String currentQuery) {
        StringBuilder query = new StringBuilder();
        StringBuilder answer = new StringBuilder();

        query.append("当前讨论的受测人有哪些信息？");

        answer.append("当前讨论的受测人的名称是：").append(this.fixName(relation.name,
                        report.getAttribute())).append("，");
        answer.append("年龄是：").append(report.getAttribute().age).append("岁，");
        answer.append("性别是：").append(report.getAttribute().getGenderText()).append("性。\n");

        if (report instanceof PaintingReport) {
            answer.append("受测人心理状态描述如下：");

            PaintingReport paintingReport = (PaintingReport) report;

            List<String> symptomContent = this.extractEvaluationContent(paintingReport);
            for (String content : symptomContent) {
                answer.append(content).append("\n");
            }
            answer.append("\n");

            if (answer.length() < ModelConfig.EXTRA_LONG_CONTEXT_LIMIT) {
                answer.append("\n受测人的大五人格画像是").append(paintingReport.getEvaluationReport()
                            .getPersonalityAccelerator().getBigFivePersonality().getDisplayName()).append("。");
                answer.append(this.filterPersonalityDescription(paintingReport.getEvaluationReport()
                            .getPersonalityAccelerator().getBigFivePersonality().getDescription()));
            }

            if (answer.length() < ModelConfig.EXTRA_LONG_CONTEXT_LIMIT) {
                answer.append("\n");
                answer.append(this.generateKnowledgeFragment(report, currentQuery));
            }
        }
        else if (report instanceof ScaleReport) {
            ScaleReport scaleReport = (ScaleReport) report;
            if (null != scaleReport.getScale()) {
                answer.append(scaleReport.getScale().displayName);
                answer.append("量表的测验结果是：\n");
            }
            else {
                answer.append("受测人的心理表现有：\n");
            }

            for (ScaleFactor factor : scaleReport.getFactors()) {
                answer.append("\n* ").append(factor.description);
                if (answer.length() > ModelConfig.EXTRA_LONG_CONTEXT_LIMIT) {
                    break;
                }
            }

            answer.append("\n对于上述心理表现给出以下建议：\n");
            for (ScaleFactor factor : scaleReport.getFactors()) {
                answer.append("\n* ").append(factor.suggestion);
                if (answer.length() > ModelConfig.EXTRA_LONG_CONTEXT_LIMIT) {
                    break;
                }
            }
        }

        GeneratingRecord result = new GeneratingRecord(ModelConfig.BAIZE_UNIT, query.toString(), answer.toString());
        return result;
    }

    private String formatReportDate(PaintingReport report) {
        return sDateFormat.format(new Date(report.getFinishedTimestamp()));
    }

    private String fixName(String name, Attribute attribute) {
        if (name.length() == 0) {
            return "匿名";
        }

        String gender = attribute.getGenderText();
        int age = attribute.age;
        if (age <= 24 || name.equals("匿名")) {
            return name;
        }
        else {
            return name + (gender.contains("男") ? "先生" : "女士");
        }
    }

    private List<String> extractEvaluationContent(PaintingReport report) {
        final int maxCount = 10;
        List<String> result = new ArrayList<>();

        for (EvaluationScore es : report.getEvaluationReport().getEvaluationScores()) {
            Logger.d(this.getClass(), "#extractEvaluationContent - indicator: " + es.indicator.name);

            String prompt = es.generateReportPrompt(report.getAttribute());
            if (null == prompt) {
                // 不需要进行报告推理，下一个
                continue;
            }

            String content = ContentTools.fastInfer(prompt, this.service.getTokenizer());
            if (null == content) {
                Logger.d(this.getClass(), "#extractEvaluationContent - Can NOT find content: " + prompt);
                continue;
            }

            content = this.filterPersonalityDescription(content);
            result.add(content);

            if (result.size() >= maxCount) {
                break;
            }
        }

        return result;

        /* FIXME 2025-03-17 从资源中直接提取
        final List<String> result = new ArrayList<>();

        List<EvaluationScore> evaluationScoreList = report.getEvaluationReport().getEvaluationScores();
        List<String> queries = new ArrayList<>();
        for (EvaluationScore es : evaluationScoreList) {
            String word = es.generateWord(report.getAttribute());
            if (null == word) {
                continue;
            }

            queries.add(word);
        }

        boolean success = this.service.retrieveReRank(queries, new RetrieveReRankListener() {
            @Override
            public void onCompleted(List<RetrieveReRankResult> retrieveReRankResults) {
                for (RetrieveReRankResult retrieveReRankResult : retrieveReRankResults) {
                    if (retrieveReRankResult.hasAnswer()) {
                        RetrieveReRankResult.Answer answer = retrieveReRankResult.getAnswerList().get(0);
                        result.add("**" + retrieveReRankResult.getQuery() + "** ：" + answer.content);
                    }
                }

                synchronized (result) {
                    result.notify();
                }
            }

            @Override
            public void onFailed(List<String> queries, AIGCStateCode stateCode) {
                synchronized (result) {
                    result.notify();
                }
            }
        });

        if (success) {
            synchronized (result) {
                try {
                    result.wait(60 * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        return result;*/
    }

    private String generateKnowledgeFragment(Report report, String query) {
        StringBuilder result = new StringBuilder();

        if (report instanceof PaintingReport) {
            PaintingReport paintingReport = (PaintingReport) report;

            if (null != paintingReport.getEvaluationReport().getPersonalityAccelerator()) {
                result.append("\n");
                String question = null;
                String answer = null;
                TFIDFAnalyzer analyzer = null;
                List<String> keywords = null;

                for (String word : sKeywordWayOfThinking) {
                    if (query.contains(word)) {
                        question = paintingReport.getEvaluationReport().getPersonalityAccelerator()
                                .getBigFivePersonality().getDisplayName() + "的思维方式";
                        analyzer = new TFIDFAnalyzer(this.tokenizer);
                        keywords = analyzer.analyzeOnlyWords(question, 10);

                        answer = Resource.getInstance().loadDataset().matchContent(keywords.toArray(new String[0]), 5);
                        if (null != answer) {
                            result.append(question).append("：");
                            result.append(answer).append("\n\n");
                        }

                        break;
                    }
                }

                for (String word : sKeywordCommunicationStyle) {
                    if (query.contains(word)) {
                        question = paintingReport.getEvaluationReport().getPersonalityAccelerator()
                                .getBigFivePersonality().getDisplayName() + "的沟通风格";
                        analyzer = new TFIDFAnalyzer(this.tokenizer);
                        keywords = analyzer.analyzeOnlyWords(question, 10);

                        answer = Resource.getInstance().loadDataset().matchContent(keywords.toArray(new String[0]), 5);
                        if (null != answer) {
                            result.append(question).append("：");
                            result.append(answer).append("\n\n");
                        }
                    }
                }

                for (String word : sKeywordWorkEnvironment) {
                    if (query.contains(word)) {
                        question = paintingReport.getEvaluationReport().getPersonalityAccelerator()
                                .getBigFivePersonality().getDisplayName() + "的工作环境偏好";
                        analyzer = new TFIDFAnalyzer(this.tokenizer);
                        keywords = analyzer.analyzeOnlyWords(question, 10);

                        answer = Resource.getInstance().loadDataset().matchContent(keywords.toArray(new String[0]), 5);
                        if (null != answer) {
                            result.append(question).append("：");
                            result.append(answer).append("\n\n");
                        }

                        break;
                    }
                }

                for (String word : sKeywordManagementRecommendation) {
                    if (query.contains(word)) {
                        question = paintingReport.getEvaluationReport().getPersonalityAccelerator()
                                .getBigFivePersonality().getDisplayName() + "的管理建议";
                        analyzer = new TFIDFAnalyzer(this.tokenizer);
                        keywords = analyzer.analyzeOnlyWords(question, 10);

                        answer = Resource.getInstance().loadDataset().matchContent(keywords.toArray(new String[0]), 5);
                        if (null != answer) {
                            result.append(question).append("：");
                            result.append(answer).append("\n\n");
                        }

                        break;
                    }
                }

                if (result.length() <= 17) {
                    result.delete(0, result.length());
                }
                else {
                    result.delete(result.length() - 1, result.length());
                    result.append("。");
                }
            }
        }

        // 术语
        List<String> words = this.service.segmentation(query);
        for (String word : words) {
            Term term = Term.parse(word);
            if (term == Term.Unknown) {
                continue;
            }

            KnowledgeStrategy knowledgeStrategy = Resource.getInstance().getTermInterpretation(term);
            if (null == knowledgeStrategy) {
                Logger.w(this.getClass(), "#generateKnowledgeFragment - No matching term: " + word);
                continue;
            }

            result.append(word).append("的专业解释如下：\n\n").append(knowledgeStrategy.getInterpretation()).append("\n\n");
            if (null != knowledgeStrategy.getExplain()) {
                result.append("从心理学角度正确理解").append(word).append("对个体的影响：\n\n")
                        .append(knowledgeStrategy.getExplain()).append("\n\n");
            }
        }

        return result.toString();
    }

    private String generateKnowledge(String query) {
        String fixQuery = query.replaceAll("你", "爱心理");

        TFIDFAnalyzer analyzer = new TFIDFAnalyzer(this.tokenizer);
        List<String> keywords = analyzer.analyzeOnlyWords(fixQuery, 7);

        StringBuilder buf = new StringBuilder();

        Dataset dataset = Resource.getInstance().loadDataset();
        List<String> list = dataset.searchContent(keywords.toArray(new String[0]), 2);
        for (String content : list) {
            buf.append(content).append("\n\n");
        }

        return buf.toString();
    }

    private String tryGenerateFactorDesc(PaintingReport report, String query) {
        StringBuilder result = new StringBuilder();

        FactorSet factorSet = report.getEvaluationReport().getFactorSet();
        if (null != factorSet) {
            List<String> symptomList = new ArrayList<>();

            List<String> words = this.service.segmentation(query);
            for (String word : words) {
                for (String symptomWord : sSymptomScoreWords) {
                    if (symptomWord.equalsIgnoreCase(word)) {
                        if (!symptomList.contains(symptomWord)) {
                            symptomList.add(symptomWord);
                        }
                    }
                }
            }

            if (!symptomList.isEmpty()) {
                result.append("以下是相关症状是否符合常模的描述，症状得分符合常模范围说明该症状在正常范围内：\n\n");
            }

            final String corpusTerm = "term";

            for (String symptomWord : symptomList) {
                // 选中症状
                String symptom = null;
                String desc = null;
                FactorSet.NormRange normRange = null;
                // 症状内容可以是 null 值
                ReportSection reportSection = null;

                if (symptomWord.equals("躯体化") || symptomWord.equalsIgnoreCase(FactorSet.Somatization)) {
                    symptom = "躯体化";
                    desc = Resource.getInstance().getCorpus(corpusTerm, FactorSet.Somatization);
                    normRange = factorSet.normSymptom(FactorSet.Somatization);
                }
                else if (symptomWord.equals("强迫") || symptomWord.equalsIgnoreCase(FactorSet.Obsession)) {
                    symptom = "强迫";
                    desc = Resource.getInstance().getCorpus(corpusTerm, FactorSet.Obsession);
                    normRange = factorSet.normSymptom(FactorSet.Obsession);
                    reportSection = report.getReportSection(Indicator.Obsession);
                }
                else if (symptomWord.equals("人际关系") || symptomWord.equalsIgnoreCase(FactorSet.Interpersonal)) {
                    symptom = "人际关系敏感";
                    desc = Resource.getInstance().getCorpus(corpusTerm, FactorSet.Interpersonal);
                    normRange = factorSet.normSymptom(FactorSet.Interpersonal);
                    reportSection = report.getReportSection(Indicator.InterpersonalRelation);
                }
                else if (symptomWord.equals("抑郁") || symptomWord.equalsIgnoreCase(FactorSet.Depression)) {
                    symptom = "抑郁倾向";
                    desc = Resource.getInstance().getCorpus(corpusTerm, FactorSet.Depression);
                    normRange = factorSet.normSymptom(FactorSet.Depression);
                    reportSection = report.getReportSection(Indicator.Depression);
                }
                else if (symptomWord.equals("焦虑") || symptomWord.equalsIgnoreCase(FactorSet.Anxiety)) {
                    symptom = "焦虑";
                    desc = Resource.getInstance().getCorpus(corpusTerm, FactorSet.Anxiety);
                    normRange = factorSet.normSymptom(FactorSet.Anxiety);
                    reportSection = report.getReportSection(Indicator.Anxiety);
                }
                else if (symptomWord.equals("敌对") || symptomWord.equalsIgnoreCase(FactorSet.Hostile)) {
                    symptom = "敌对";
                    desc = Resource.getInstance().getCorpus(corpusTerm, FactorSet.Hostile);
                    normRange = factorSet.normSymptom(FactorSet.Hostile);
                    reportSection = report.getReportSection(Indicator.Hostile);
                }
                else if (symptomWord.equals("恐怖") || symptomWord.equalsIgnoreCase(FactorSet.Horror)) {
                    symptom = "恐怖";
                    desc = Resource.getInstance().getCorpus(corpusTerm, FactorSet.Horror);
                    normRange = factorSet.normSymptom(FactorSet.Horror);
                }
                else if (symptomWord.equals("偏执") || symptomWord.equalsIgnoreCase(FactorSet.Paranoid)) {
                    symptom = "偏执";
                    desc = Resource.getInstance().getCorpus(corpusTerm, FactorSet.Paranoid);
                    normRange = factorSet.normSymptom(FactorSet.Paranoid);
                    reportSection = report.getReportSection(Indicator.Paranoid);
                }
                else if (symptomWord.equals("精神病性") || symptomWord.equalsIgnoreCase(FactorSet.Psychosis)) {
                    symptom = "精神病性";
                    desc = Resource.getInstance().getCorpus(corpusTerm, FactorSet.Psychosis);
                    normRange = factorSet.normSymptom(FactorSet.Psychosis);
                }

                if (null == symptom || null == desc || null == normRange) {
                    continue;
                }

                if (null != reportSection) {
                    result.append("受测人的" + symptom + "指标的描述是：\n");
                    result.append(reportSection.title).append("。");
                    result.append(this.filterPersonalityDescription(reportSection.report)).append("\n\n");
                }

                if (normRange.norm) {
                    if (null != reportSection && reportSection.rate != IndicatorRate.None) {
                        String content = "";
                        switch (reportSection.rate) {
                            case Highest:
                                content = symptom + "数据得分较高，受测人有症状，程度为中度到重度。";
                                break;
                            case High:
                                content = symptom + "数据得分高，受测人有症状，程度为中度。";
                                break;
                            case Medium:
                                content = symptom + "数据得分中等，受测人症状感受一般，程度为轻度到中度。";
                                break;
                            case Low:
                                content = symptom + "数据得分低，受测人略有症状，程度为轻度。";
                                break;
                            case Lowest:
                                content = symptom + "数据得分较低，受测人没有症状。";
                                break;
                            default:
                                break;
                        }
                        result.append(content).append("\n\n");
                    }
                    else {
                        String content = symptom + "数据得分在常模范围内，" + symptom + "症状正常。" + desc;
                        result.append(content).append("\n\n");
                    }
                }
                else {
                    String content = symptom + "数据得分超过常模范围，" + symptom + "症状可能异常。" + desc;
                    result.append(content).append("\n\n");
                }
            }
        }

        return result.toString();
    }

    private String tryGeneratePaintingFeature(PaintingReport paintingReport, String query) {
        StringBuilder buf = new StringBuilder();

        boolean hit = false;
        TFIDFAnalyzer analyzer = new TFIDFAnalyzer(this.tokenizer);
        List<String> keywords = analyzer.analyzeOnlyWords(query, 10);
        for (String keyword : keywords) {
            for (String word : sPaintingDesc) {
                if (keyword.contains(word)) {
                    hit = true;
                    break;
                }
            }
            if (hit) {
                break;
            }
        }

        if (hit) {
            PaintingFeatureSet featureSet = this.storage.readPaintingFeatureSet(paintingReport.sn);
            if (null != featureSet) {
                buf.append(featureSet.makeMarkdown(false));
            }
            else {
                Logger.w(this.getClass(), "#tryGeneratePaintingFeature - Can NOT find painting feature set data: " +
                        paintingReport.sn);
            }
        }

        return buf.toString();
    }

    private String tryGeneratePersonality(PaintingReport report, String query) {
        StringBuilder result = new StringBuilder();

        List<String> words = this.service.segmentation(query);
        boolean hit = false;
        for (String kw : sKeywordPersonality) {
            for (String word : words) {
                if (kw.equalsIgnoreCase(word)) {
                    hit = true;
                    break;
                }
            }
            if (hit) {
                break;
            }
        }

        if (!hit) {
            String[] keywordArray = Arrays.copyOf(sKeywordWayOfThinking,
                    sKeywordWayOfThinking.length +
                            sKeywordCommunicationStyle.length +
                            sKeywordWorkEnvironment.length +
                            sKeywordManagementRecommendation.length);
            System.arraycopy(sKeywordCommunicationStyle, 0,
                    keywordArray,
                    sKeywordWayOfThinking.length,
                    sKeywordCommunicationStyle.length);
            System.arraycopy(sKeywordWorkEnvironment, 0,
                    keywordArray,
                    sKeywordWayOfThinking.length + sKeywordCommunicationStyle.length,
                    sKeywordWorkEnvironment.length);
            System.arraycopy(sKeywordManagementRecommendation, 0,
                    keywordArray,
                    sKeywordWayOfThinking.length + sKeywordCommunicationStyle.length + sKeywordWorkEnvironment.length,
                    sKeywordManagementRecommendation.length);
            for (String kw : keywordArray) {
                for (String word : words) {
                    if (kw.equalsIgnoreCase(word)) {
                        hit = true;
                        break;
                    }
                }
                if (hit) {
                    break;
                }
            }
        }

        if (hit) {
            result.append("受测人的大五人格画像是").append(report.getEvaluationReport()
                    .getPersonalityAccelerator().getBigFivePersonality().getDisplayName()).append("。\n");
            // 性格特点
            result.append(report.getEvaluationReport()
                    .getPersonalityAccelerator().getBigFivePersonality().getDisplayName()).append("的性格特点：");
            result.append(this.filterPersonalityDescription(report.getEvaluationReport()
                    .getPersonalityAccelerator().getBigFivePersonality().getDescription()));
            result.append("\n\n");
        }
        return result.toString();
    }

    private double fastSentenceSimilarity(String sentenceA, String sentenceB) {
        TFIDFAnalyzer analyzer = new TFIDFAnalyzer(this.tokenizer);
        List<String> wordsA = analyzer.analyzeOnlyWords(sentenceA, 10);
        List<String> wordsB = analyzer.analyzeOnlyWords(sentenceB, 10);
        List<String> pole = null;
        List<String> monkey = null;
        if (wordsA.size() > wordsB.size()) {
            pole = wordsA;
            monkey = wordsB;
        }
        else {
            pole = wordsB;
            monkey = wordsA;
        }
        double count = 0;
        for (String word : pole) {
            if (monkey.contains(word)) {
                count += 1.0;
            }
        }
        return count / pole.size();
    }

    private String filterPersonalityDescription(String desc) {
        String result = desc.replaceAll("你", "受测人");
        return result.replaceAll("您", "受测人");
    }

    private String filterSubjectNoun(String content, Attribute attribute) {
        return content.replaceAll("受测人", attribute.isMale() ? "他" : "她");
    }
}
