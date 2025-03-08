/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.scene;

import cell.util.log.Logger;
import cube.aigc.ModelConfig;
import cube.aigc.psychology.*;
import cube.aigc.psychology.composition.*;
import cube.common.entity.GeneratingRecord;
import cube.common.entity.QuestionAnswer;
import cube.common.entity.RetrieveReRankResult;
import cube.common.state.AIGCStateCode;
import cube.service.aigc.AIGCService;
import cube.service.aigc.listener.RetrieveReRankListener;
import cube.service.aigc.listener.SemanticSearchListener;
import cube.service.tokenizer.Tokenizer;
import cube.service.tokenizer.keyword.TFIDFAnalyzer;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class QueryRevolver {

    private final static SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyy年MM月dd日HH时mm分ss秒");

    private String[] keywordWorkEnvironment = new String[] {
            "工作环境偏好",
            "工作环境喜好",
            "工作环境倾向",
            "工作环境"
    };

    private String[] keywordManagementRecommendation = new String[] {
            "管理建议",
            "管理方式",
            "管理方法",
            "管理"
    };

    private String[] keywordSuggestion = new String[] {
            "建议",
            "改善",
            "缓解",
            "解决"
    };

    private String[] paintingDesc = new String[] {
            "画", "画面", "图画", "图像", "照片", "绘画", "看"
    };

    private AIGCService service;

    private Tokenizer tokenizer;

    private PsychologyStorage storage;

    public QueryRevolver(AIGCService service, PsychologyStorage storage) {
        this.service = service;
        this.tokenizer = service.getTokenizer();
        this.storage = storage;
    }

    private String formatReportDate(PaintingReport report) {
        return sDateFormat.format(new Date(report.getFinishedTimestamp()));
    }

    public String generatePrompt(ConversationContext context, String query) {
        final StringBuilder result = new StringBuilder();

        int wordLimit = ModelConfig.BAIZE_NEXT_CONTEXT_LIMIT - 60;

        if (null != context.getCurrentReport() && !context.getCurrentReport().isNull()) {
            PaintingReport report = context.getCurrentReport();
            result.append("已知信息：\n\n");
            result.append("当前评测报告的受测人是匿名的，");
            result.append("年龄是：").append(report.getAttribute().age).append("岁，");
            result.append("性别是：").append(report.getAttribute().getGenderText()).append("性。\n");
            result.append("报告日期是：").append(formatReportDate(report)).append("。\n");
            result.append("受测人的心理特征摘要如下：");
            result.append(report.getSummary());
            result.append("\n\n");

            result.append("受测人有以下心理状态：\n");
            List<String> symptomContent = this.extractSymptomContent(report.getEvaluationReport().getEvaluationScores(),
                    report.getAttribute());
            for (String content : symptomContent) {
                result.append("* ").append(content).append("\n");
            }
            result.append("\n");

            // 画面特征
            result.append(this.tryGeneratePaintingFeature(report, query));

            // 更新长度限制
            wordLimit -= result.length();

            if (wordLimit > 0) {
                result.append("\n受测人的大五人格画像是").append(report.getEvaluationReport()
                        .getPersonalityAccelerator().getBigFivePersonality().getDisplayName()).append("。\n");
                // 性格特点
                result.append(report.getEvaluationReport()
                        .getPersonalityAccelerator().getBigFivePersonality().getDisplayName()).append("的性格特点：");
                result.append(this.filterPersonalityDescription(report.getEvaluationReport()
                                .getPersonalityAccelerator().getBigFivePersonality().getDescription(),
                        report.getAttribute()));
                // 更新长度限制
                wordLimit -= result.length();
            }

            if (wordLimit > 300) {
                // 尝试生成数据片段
                result.append("\n");
                result.append(this.generateFragment(report, query));
            }

            // 更新长度限制
            wordLimit -= result.length();
        }

        final List<String> answerList = new ArrayList<>();
        boolean success = this.service.semanticSearch(query, new SemanticSearchListener() {
            @Override
            public void onCompleted(String query, List<QuestionAnswer> questionAnswers) {
                for (QuestionAnswer questionAnswer : questionAnswers) {
                    if (questionAnswer.getScore() > 0.8) {
                        // 排除得分较低答案
                        List<String> answers = questionAnswer.getAnswers();
                        for (String answer : answers) {
                            Subtask subtask = Subtask.extract(answer);
                            if (Subtask.None == subtask) {
                                // 不是子任务
                                answerList.add(answer);
                            }
                        }
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

        if (!answerList.isEmpty()) {
            if (result.length() == 0) {
                result.append("已知信息：\n\n");
            }
            else {
                result.append("\n下面的内容是一些与问题相关的知识：\n");
            }

            for (String answer : answerList) {
                if (result.length() + answer.length() >= wordLimit) {
                    break;
                }
                result.append(answer).append("\n\n");
            }
        }

        if (result.length() > 0) {
            result.append("\n根据以上信息，专业地回答问题，如果无法从中得到答案，请说“暂时没有获得足够的相关信息。”，不允许在答案中添加编造成分。问题是：");
            result.append(query).append("\n");
        }
        else {
            result.append("专业地回答问题，在答案最后加入一句：“我的更多功能您可以通过：**功能介绍** 进行了解。”，问题是：");
            result.append(query).append("\n");
        }

        return result.toString();
    }

    public String generatePrompt(ConversationRelation relation, Report report, String query) {
        StringBuilder result = new StringBuilder();

        result.append("已知信息：\n\n当前受测人的名称是：").append(this.fixName(
                relation.name,
                report.getAttribute().getGenderText(),
                report.getAttribute().age)).append("，");
        result.append("年龄是：").append(report.getAttribute().age).append("岁，");
        result.append("性别是：").append(report.getAttribute().getGenderText()).append("性。\n");

        if (report instanceof PaintingReport) {
            result.append("受测人有以下心理状态：\n");

            PaintingReport paintingReport = (PaintingReport) report;

            List<String> symptomContent = this.extractSymptomContent(paintingReport.getEvaluationReport().getEvaluationScores(),
                    report.getAttribute());
            for (String content : symptomContent) {
                result.append("* ").append(content).append("\n");
            }
            result.append("\n");

            // 画面特征
            result.append(this.tryGeneratePaintingFeature(paintingReport, query));

            if (result.length() < ModelConfig.EXTRA_LONG_CONTEXT_LIMIT) {
                result.append("\n受测人的大五人格画像是").append(paintingReport.getEvaluationReport()
                        .getPersonalityAccelerator().getBigFivePersonality().getDisplayName()).append("。\n");
                // 性格特点
                result.append(paintingReport.getEvaluationReport()
                            .getPersonalityAccelerator().getBigFivePersonality().getDisplayName()).append("的性格特点：");
                result.append(this.filterPersonalityDescription(paintingReport.getEvaluationReport()
                            .getPersonalityAccelerator().getBigFivePersonality().getDescription(),
                        paintingReport.getAttribute()));
            }

            if (result.length() < ModelConfig.EXTRA_LONG_CONTEXT_LIMIT) {
                result.append("\n");
                result.append(this.generateFragment(report, query));
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

        result.append("\n根据上述已知信息，专业地来回答用户的问题。");

        for (String word : this.keywordSuggestion) {
            if (query.contains(word)) {
                result.append("答案里不要出现药品信息。");
                break;
            }
        }

        result.append("问题是：");
        result.append(query);

        return this.filterSubjectNoun(result.toString(), report.getAttribute());
    }

    public String generatePrompt(List<ConversationRelation> relations, List<Report> reports, String query) {
        StringBuilder result = new StringBuilder();

        result.append("已知信息：\n\n");
        result.append("受测人有").append(relations.size()).append("份报告信息如下：\n\n");

        for (int i = 0; i < relations.size(); ++i) {
            ConversationRelation relation = relations.get(i);
            Report report = reports.get(i);
            result.append("# 报告").append(i + 1);
            result.append("受测人的名称是：").append(this.fixName(
                    relation.name,
                    report.getAttribute().getGenderText(),
                    report.getAttribute().age)).append("，");
            result.append("年龄是：").append(report.getAttribute().age).append("岁，");
            result.append("性别是：").append(report.getAttribute().getGenderText()).append("性。\n");

            if (report instanceof PaintingReport) {
                result.append("受测人有以下心理状态：\n");

                PaintingReport paintingReport = (PaintingReport) report;
                List<String> symptomContent = this.extractSymptomContent(paintingReport.getEvaluationReport().getEvaluationScores(),
                        report.getAttribute());
                for (String content : symptomContent) {
                    result.append("* ").append(content).append("\n");
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
                                .getPersonalityAccelerator().getBigFivePersonality().getDescription(),
                            paintingReport.getAttribute()));
                }

                if (result.length() < ModelConfig.EXTRA_LONG_CONTEXT_LIMIT) {
                    result.append("\n");
                    result.append(this.generateFragment(report, query));
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

        for (String word : this.keywordSuggestion) {
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
                        report.getAttribute().getGenderText(),
                        report.getAttribute().age)).append("，");
        answer.append("年龄是：").append(report.getAttribute().age).append("岁，");
        answer.append("性别是：").append(report.getAttribute().getGenderText()).append("性。\n");

        if (report instanceof PaintingReport) {
            answer.append("受测人的心理状态如下：");

            PaintingReport paintingReport = (PaintingReport) report;

            List<String> symptomContent = this.extractSymptomContent(paintingReport.getEvaluationReport().getEvaluationScores(),
                    report.getAttribute());
            for (String content : symptomContent) {
                answer.append("* ").append(content).append("\n");
            }
            answer.append("\n");

            if (answer.length() < ModelConfig.EXTRA_LONG_CONTEXT_LIMIT) {
                answer.append("\n受测人的大五人格画像是").append(paintingReport.getEvaluationReport()
                            .getPersonalityAccelerator().getBigFivePersonality().getDisplayName()).append("。");
                answer.append(this.filterPersonalityDescription(paintingReport.getEvaluationReport()
                            .getPersonalityAccelerator().getBigFivePersonality().getDescription(),
                        paintingReport.getAttribute()));
            }

            if (answer.length() < ModelConfig.EXTRA_LONG_CONTEXT_LIMIT) {
                answer.append("\n");
                answer.append(this.generateFragment(report, currentQuery));
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

    private String fixName(String name, String gender, int age) {
        if (age <= 22) {
            return name;
        }
        else {
            return name + (gender.contains("男") ? "先生" : "女士");
        }
    }

    private List<String> extractSymptomContent(List<EvaluationScore> list, Attribute attribute) {
        final List<String> result = new ArrayList<>();

        List<String> queries = new ArrayList<>();
        for (EvaluationScore es : list) {
            String word = es.generateWord(attribute);
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
                        result.add("**" + retrieveReRankResult.getAnswerList() + "**。" + retrieveReRankResult);
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
                    result.wait(30 * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        return result;
    }

    private String generateFragment(Report report, String query) {
        StringBuilder result = new StringBuilder();

        if (report instanceof PaintingReport) {
            PaintingReport paintingReport = (PaintingReport) report;

            if (null != paintingReport.getEvaluationReport().getPersonalityAccelerator()) {
                result.append("\n");
                String question = null;
                String answer = null;
                TFIDFAnalyzer analyzer = null;
                List<String> keywords = null;

                question = paintingReport.getEvaluationReport().getPersonalityAccelerator()
                        .getBigFivePersonality().getDisplayName() + "的思维方式";
                analyzer = new TFIDFAnalyzer(this.tokenizer);
                keywords = analyzer.analyzeOnlyWords(question, 10);

                answer = Resource.getInstance().loadDataset().matchContent(keywords.toArray(new String[0]), 5);
                if (null != answer) {
                    result.append(question).append("：");
                    result.append(answer).append("\n\n");
                }

                question = paintingReport.getEvaluationReport().getPersonalityAccelerator()
                        .getBigFivePersonality().getDisplayName() + "的沟通风格";
                analyzer = new TFIDFAnalyzer(this.tokenizer);
                keywords = analyzer.analyzeOnlyWords(question, 10);

                answer = Resource.getInstance().loadDataset().matchContent(keywords.toArray(new String[0]), 5);
                if (null != answer) {
                    result.append(question).append("：");
                    result.append(answer).append("\n\n");
                }

                for (String word : this.keywordWorkEnvironment) {
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

                for (String word : this.keywordManagementRecommendation) {
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

    private String tryGeneratePaintingFeature(PaintingReport paintingReport, String query) {
        StringBuilder buf = new StringBuilder();

        boolean hit = false;
        TFIDFAnalyzer analyzer = new TFIDFAnalyzer(this.tokenizer);
        List<String> keywords = analyzer.analyzeOnlyWords(query, 10);
        for (String keyword : keywords) {
            for (String word : this.paintingDesc) {
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

    private String filterPersonalityDescription(String desc, Attribute attribute) {
        return desc.replaceAll("你", attribute.isMale() ? "他" : "她");
    }

    private String filterSubjectNoun(String content, Attribute attribute) {
        return content.replaceAll("受测人", "他");
    }
}
