/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.scene;

import cell.util.log.Logger;
import cube.aigc.ModelConfig;
import cube.aigc.psychology.*;
import cube.aigc.psychology.algorithm.BigFivePersonality;
import cube.aigc.psychology.algorithm.PersonalityAccelerator;
import cube.aigc.psychology.composition.*;
import cube.auth.AuthToken;
import cube.common.Language;
import cube.common.entity.AIGCChannel;
import cube.common.entity.GeneratingOption;
import cube.common.entity.GeneratingRecord;
import cube.service.aigc.AIGCService;
import cube.service.tokenizer.keyword.Keyword;
import cube.service.tokenizer.keyword.TFIDFAnalyzer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 评估工作器。
 */
public class EvaluationWorker {

    private final static String REFINE_CN = "对以下内容进行适度润色，不要改变原内容的含义，不要添加额外内容。需要润色的内容如下：\n\n%s\n";

    private final static String REFINE_EN = "The following content requires moderate refinement. Please do not alter the original meaning of the content or add any additional information. The content to be refined is as follows:\n\n%s\n";

    private final static String PERSONALITY_FORMAT_CN = "已知受测人的人格特点如下：\n\n%s\n\n根据上述信息回答问题，不能编造成分，问题是：总结受测人的人格特点。";

    private final static String PERSONALITY_FORMAT_EN = "The personality traits of the test subject are known as follows:\n\n" +
            "%s" +
            "\n\nAnswer the question based on the above information. Do not fabricate scores. The question is: Summarize the personality traits of the test subject.";

    public final boolean fast = true;

    private EvaluationReport evaluationReport;

    private AIGCService service;

    private Attribute attribute;

    private HexagonDimensionScore dimensionScore;

    private HexagonDimensionScore normDimensionScore;

    private List<ReportSection> reportSectionList;

    private String summary = "";

    private PaintingFeatureSet paintingFeatureSet;

    public EvaluationWorker(AIGCService service, Attribute attribute) {
        this.service = service;
        this.attribute = attribute;
    }

    public EvaluationWorker(EvaluationReport evaluationReport, AIGCService service) {
        this.attribute = evaluationReport.getAttribute();
        this.evaluationReport = evaluationReport;
        this.service = service;
        this.reportSectionList = new ArrayList<>();
    }

    public EvaluationReport getEvaluationReport() {
        return this.evaluationReport;
    }

    public void setPaintingFeatureSet(PaintingFeatureSet featureSet) {
        this.paintingFeatureSet = featureSet;
    }

    public PaintingFeatureSet getPaintingFeatureSet() {
        return this.paintingFeatureSet;
    }

    public PaintingReport fillReport(PaintingReport report) {
        report.setEvaluationReport(this.evaluationReport);

        if (null != this.dimensionScore && null != this.normDimensionScore) {
            report.setDimensionalScore(this.dimensionScore, this.normDimensionScore);
        }

        report.setSummary(this.summary);
        report.setReportSectionList(this.reportSectionList);

        if (null != this.paintingFeatureSet) {
            this.paintingFeatureSet.setSN(report.sn);
        }

        return report;
    }

    public void mergeFactorSet(FactorSet factorSet) {
        Logger.d(this.getClass(), "#mergeFactorSet");

        this.evaluationReport.setFactorSet(factorSet);

        PersonalityAccelerator expectedPersonality = new PersonalityAccelerator(factorSet);

        Logger.d(this.getClass(), "#mergeFactorSet - " +
                expectedPersonality.getBigFivePersonality().getObligingness() + "/" +
                expectedPersonality.getBigFivePersonality().getConscientiousness() + "/" +
                expectedPersonality.getBigFivePersonality().getExtraversion() + "/" +
                expectedPersonality.getBigFivePersonality().getAchievement() + "/" +
                expectedPersonality.getBigFivePersonality().getNeuroticism() + "/");

        PersonalityAccelerator actualPersonality = this.evaluationReport.getPersonalityAccelerator();

        double obligingness = actualPersonality.getBigFivePersonality().getObligingness();
        double conscientiousness = actualPersonality.getBigFivePersonality().getConscientiousness();
        double extraversion = actualPersonality.getBigFivePersonality().getExtraversion();
        double achievement = actualPersonality.getBigFivePersonality().getAchievement();

        if (!actualPersonality.obligingnessConfidence) {
            obligingness = expectedPersonality.getBigFivePersonality().getObligingness();
        }
        if (!actualPersonality.conscientiousnessConfidence) {
            conscientiousness = expectedPersonality.getBigFivePersonality().getConscientiousness();
        }
        if (!actualPersonality.extraversionConfidence) {
            extraversion = expectedPersonality.getBigFivePersonality().getExtraversion();
        }
        if (!actualPersonality.achievementConfidence) {
            achievement = expectedPersonality.getBigFivePersonality().getAchievement();
        }

        // 情绪性
        double neuroticism = expectedPersonality.getBigFivePersonality().getNeuroticism();

        this.evaluationReport.getPersonalityAccelerator().reset(obligingness, conscientiousness,
                extraversion, achievement, neuroticism);

        // 重算关注等级
        this.evaluationReport.rollAttentionSuggestion();
    }

    public boolean isUnknown() {
        return this.evaluationReport.isUnknown();
    }

    /**
     * 生成报告内容。
     *
     * @param channel
     * @param theme
     * @param maxIndicatorTexts
     * @return
     */
    public EvaluationWorker make(AIGCChannel channel, Theme theme, int maxIndicatorTexts) {
        switch (theme) {
            case Generic:
            case HouseTreePerson:
                // 评估分推理
                List<EvaluationScore> scoreList = this.evaluationReport.getEvaluationScoresByRepresentation(Indicator.values().length);
                this.reportSectionList = this.inferScore(scoreList, maxIndicatorTexts);
                if (this.reportSectionList.isEmpty()) {
                    Logger.w(this.getClass(), "#make - Report text error");
                    return this;
                }

                if (Logger.isDebugLevel()) {
                    Logger.d(this.getClass(), "#make - The report section size: " + this.reportSectionList.size());
                }

                // 生成概述
                this.summary = this.inferSummary(channel.getAuthToken(), this.reportSectionList, channel.getLanguage());

                // 生成人格描述
                this.inferPersonality(channel.getAuthToken(), this.evaluationReport.getPersonalityAccelerator(),
                        channel.getLanguage());

                // 六维得分计算
                try {
                    this.dimensionScore = new HexagonDimensionScore(this.evaluationReport.getAttention(),
                            this.evaluationReport.getFullEvaluationScores(),
                            this.evaluationReport.getPaintingConfidence(), this.evaluationReport.getFactorSet());
                    this.normDimensionScore = new HexagonDimensionScore(
                            80, 80, 80, 80, 80, 80);
                    // 描述
                    ContentTools.fillHexagonScoreDescription(this.service.getTokenizer(), this.dimensionScore,
                            channel.getLanguage());
                } catch (Exception e) {
                    Logger.w(this.getClass(), "#make", e);
                }
                break;
            case AttachmentStyle:
                List<EvaluationScore> attachmentScores = this.evaluationReport.getEvaluationScores();
                // 进行指标排序
                Collections.sort(attachmentScores, new Comparator<EvaluationScore>() {
                    @Override
                    public int compare(EvaluationScore es1, EvaluationScore es2) {
                        return (int)(es1.calcScore() - es2.calcScore());
                    }
                });
                EvaluationScore score = attachmentScores.get(attachmentScores.size() - 1);
                List<EvaluationScore> newAttachmentScores = new ArrayList<>();
                newAttachmentScores.add(score);
                System.out.println("XJW: " + attachmentScores.size() + " - " + score.indicator.name);
                this.reportSectionList = this.inferScore(newAttachmentScores, maxIndicatorTexts);
                for (ReportSection section : this.reportSectionList) {
                    String prompt = String.format(this.attribute.language.isChinese() ? REFINE_CN : REFINE_EN,
                            section.report);
                    GeneratingRecord record = this.service.syncGenerateText(channel.getAuthToken(),
                            ModelConfig.BAIZE_NEXT_UNIT, prompt, new GeneratingOption(),
                            null, null);
                    if (null != record) {
                        section.report = record.answer;
                    }
                }

                // 生成概述
                this.summary = this.inferSummaryByTemplate(channel.getAuthToken(), this.reportSectionList, channel.getLanguage());

                break;
            default:
                break;
        }

        return this;
    }

    /**
     * 推理人格。
     *
     * @param authToken
     * @param personalityAccelerator
     * @param language
     * @return
     */
    private boolean inferPersonality(AuthToken authToken, PersonalityAccelerator personalityAccelerator,
                                     Language language) {
        BigFivePersonality feature = personalityAccelerator.getBigFivePersonality();
        String prompt = feature.generateReportPrompt();
        String answer = null;
        if (this.fast) {
            answer = this.extract(prompt);
        }
        if (null == answer) {
            Logger.w(this.getClass(), "#inferPersonality - No answer for \"" + prompt + "\"");

            GeneratingRecord generating = this.service.syncGenerateText(ModelConfig.BAIZE_UNIT, prompt, new GeneratingOption(),
                    null, null);
            answer = (null != generating) ? generating.answer : null;
        }
        if (null == answer) {
            Logger.w(this.getClass(), "#inferPersonality - report is null: " + prompt);
            return false;
        }

        // 对人格画像进行描述
        prompt = String.format(language.isChinese() ? PERSONALITY_FORMAT_CN : PERSONALITY_FORMAT_EN,
                fixSecondPerson(answer, language));
        GeneratingRecord generatingResult = this.service.syncGenerateText(authToken, ModelConfig.BAIZE_NEXT_UNIT,
                prompt, new GeneratingOption(), null, null);
        String fixAnswer = (null != generatingResult) ? generatingResult.answer : null;
        if (null != fixAnswer) {
            answer = fixThirdPerson(fixAnswer);
        }
        // 设置人格画像描述
        feature.setDescription(answer);

        // 宜人性
        prompt = feature.generateObligingnessPrompt();
        answer = null;
        if (this.fast) {
            Logger.d(this.getClass(), "#inferPersonality - Obligingness prompt: \"" + prompt + "\"");
            answer = this.extract(prompt);
        }
        if (null == answer) {
            Logger.w(this.getClass(), "#inferPersonality - No answer for \"" + prompt + "\"");
            GeneratingRecord generating = this.service.syncGenerateText(ModelConfig.BAIZE_UNIT, prompt, new GeneratingOption(),
                    null, null);
            answer = (null != generating) ? generating.answer : null;
        }
        if (null == answer) {
            Logger.w(this.getClass(), "#inferPersonality - Obligingness content error: " + prompt);
        }
        else {
            feature.setObligingnessContent(answer);
        }
        // 宜人性释义
        feature.setObligingnessParaphrase(this.extract(feature.getObligingnessPrompt()));

        // 尽责性
        prompt = feature.generateConscientiousnessPrompt();
        answer = null;
        if (this.fast) {
            Logger.d(this.getClass(), "#inferPersonality - Conscientiousness prompt: \"" + prompt + "\"");
            answer = this.extract(prompt);
        }
        if (null == answer) {
            Logger.w(this.getClass(), "#inferPersonality - No answer for \"" + prompt + "\"");
            GeneratingRecord generating = this.service.syncGenerateText(ModelConfig.BAIZE_UNIT, prompt, new GeneratingOption(),
                    null, null);
            answer = (null != generating) ? generating.answer : null;
        }
        if (null == answer) {
            Logger.w(this.getClass(), "#inferPersonality - Conscientiousness content error: " + prompt);
        }
        else {
            feature.setConscientiousnessContent(answer);
        }
        // 尽责性释义
        feature.setConscientiousnessParaphrase(this.extract(feature.getConscientiousnessPrompt()));

        // 外向性
        prompt = feature.generateExtraversionPrompt();
        answer = null;
        if (this.fast) {
            Logger.d(this.getClass(), "#inferPersonality - Extraversion prompt: \"" + prompt + "\"");
            answer = this.extract(prompt);
        }
        if (null == answer) {
            Logger.w(this.getClass(), "#inferPersonality - No answer for \"" + prompt + "\"");
            GeneratingRecord generating = this.service.syncGenerateText(ModelConfig.BAIZE_UNIT, prompt, new GeneratingOption(),
                    null, null);
            answer = (null != generating) ? generating.answer : null;
        }
        if (null == answer) {
            Logger.w(this.getClass(), "#inferPersonality - Extraversion content error: " + prompt);
        }
        else {
            feature.setExtraversionContent(answer);
        }
        // 外向性释义
        feature.setExtraversionParaphrase(this.extract(feature.getExtraversionPrompt()));

        // 进取性
        prompt = feature.generateAchievementPrompt();
        answer = null;
        if (this.fast) {
            Logger.d(this.getClass(), "#inferPersonality - Achievement prompt: \"" + prompt + "\"");
            answer = this.extract(prompt);
        }
        if (null == answer) {
            Logger.w(this.getClass(), "#inferPersonality - No answer for \"" + prompt + "\"");
            GeneratingRecord generating = this.service.syncGenerateText(ModelConfig.BAIZE_UNIT, prompt, new GeneratingOption(),
                    null, null);
            answer = (null != generating) ? generating.answer : null;
        }
        if (null == answer) {
            Logger.w(this.getClass(), "#inferPersonality - Achievement content error: " + prompt);
        }
        else {
            feature.setAchievementContent(answer);
        }
        // 进取性释义
        feature.setAchievementParaphrase(this.extract(feature.getAchievementPrompt()));

        // 情绪性
        prompt = feature.generateNeuroticismPrompt();
        answer = null;
        if (this.fast) {
            Logger.d(this.getClass(), "#inferPersonality - Neuroticism prompt: \"" + prompt + "\"");
            answer = this.extract(prompt);
        }
        if (null == answer) {
            Logger.w(this.getClass(), "#inferPersonality - No answer for \"" + prompt + "\"");
            GeneratingRecord generating = this.service.syncGenerateText(ModelConfig.BAIZE_UNIT, prompt, new GeneratingOption(),
                    null, null);
            answer = (null != generating) ? generating.answer : null;
        }
        if (null == answer) {
            Logger.w(this.getClass(), "#inferPersonality - Neuroticism content error: " + prompt);
        }
        else {
            feature.setNeuroticismContent(answer);
        }
        // 情绪性释义
        feature.setNeuroticismParaphrase(this.extract(feature.getNeuroticismPrompt()));

        return true;
    }

    private List<ReportSection> inferScore(List<EvaluationScore> scoreList, int maxIndicatorTexts) {
        List<ReportSection> result = new ArrayList<>();
        for (EvaluationScore es : scoreList) {
            Logger.d(this.getClass(), "#inferScore - score: " + es.indicator.name);

            String prompt = es.generateReportPrompt(this.attribute);
            if (null == prompt) {
                // 不需要进行报告推理，下一个
                continue;
            }

            String report = null;

            if (this.fast) {
                report = this.extract(prompt);
            }
            if (null == report) {
                Logger.w(this.getClass(), "#inferScore - No report for \"" + prompt + "\"");
                GeneratingRecord generating = this.service.syncGenerateText(ModelConfig.BAIZE_UNIT, prompt, new GeneratingOption(),
                        null, null);
                report = (null != generating) ? generating.answer : null;
            }

            prompt = es.generateSuggestionPrompt(this.attribute);
            if (null == prompt) {
                // 下一个
                result.add(new ReportSection(es.indicator, es.indicator.name,
                        report, "", es.getIndicatorRate(this.attribute)));
                if (result.size() >= maxIndicatorTexts) {
                    break;
                }
                continue;
            }

            String suggestion = null;

            if (this.fast) {
                suggestion = this.extract(prompt);
            }
            if (null == suggestion) {
                Logger.w(this.getClass(), "#inferScore - No suggestion for \"" + prompt + "\"");
                GeneratingRecord generating = this.service.syncGenerateText(ModelConfig.BAIZE_UNIT, prompt, new GeneratingOption(),
                        null, null);
                suggestion = (null != generating) ? generating.answer : null;
            }

            if (null != report && null != suggestion) {
                // 报告标题使用指标名称
                result.add(new ReportSection(es.indicator, es.indicator.name,
                        report, suggestion, es.getIndicatorRate(this.attribute)));
                if (result.size() >= maxIndicatorTexts) {
                    break;
                }
            }
        }

        return result;
    }

    private String inferSummary(AuthToken authToken, List<ReportSection> list, Language language) {
        if (list.isEmpty()) {
            return Resource.getInstance().getCorpus("report", "REPORT_NO_DATA_SUMMARY",
                    language);
        }

        String unitName = null;
        if (this.service.hasUnit(ModelConfig.BAIZE_NEXT_UNIT)) {
            unitName = ModelConfig.BAIZE_NEXT_UNIT;
        }
        else if (this.service.hasUnit(ModelConfig.BAIZE_X_UNIT)) {
            unitName = ModelConfig.BAIZE_X_UNIT;
        }
        else {
            unitName = ModelConfig.BAIZE_UNIT;
        }

        StringBuilder summary = new StringBuilder();

        // 生成概述
        StringBuilder prompt = new StringBuilder();
        prompt.append(language.isChinese() ? "已知信息：\n\n" : "The known information:\n\n");
        prompt.append(language.isChinese() ?
                "受测人心理评测结果如下：\n\n" : "The psychological assessment results of the test subjects are as follows:\n\n");
        for (ReportSection rs : list) {
            prompt.append(fixSecondPerson(rs.report, language)).append("\n\n");
            if (prompt.length() >= ModelConfig.getPromptLengthLimit(unitName)) {
                break;
            }
        }
        prompt.append("\n");
        prompt.append("根据上述已知信息，简洁和专业地来回答用户的问题。问题是：概述此人的心理评测结果，各内容之间分段展示。");
        GeneratingRecord generating = this.service.syncGenerateText(authToken, unitName, prompt.toString(),
                new GeneratingOption(), null, null);
        String result = (null != generating) ? generating.answer : null;

        if (null == result || result.contains("我遇到一些问题") || result.contains("我遇到一些技术问题")) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            generating = this.service.syncGenerateText(authToken, ModelConfig.BAIZE_NEXT_UNIT, prompt.toString(),
                    new GeneratingOption(), null, null);
            result = (null != generating) ? generating.answer : null;
        }

        if (null != result) {
            summary.append(result);
        }

        return fixThirdPerson(summary.toString());
    }

    private String inferSummaryByTemplate(AuthToken authToken, List<ReportSection> list, Language language) {
        StringBuilder buf = new StringBuilder();
        for (ReportSection section : list) {
            buf.append("受测人是").append(section.title).append("。\n\n");
            buf.append(section.title).append("包含以下描述：\n\n");
            buf.append(section.report).append("\n\n");
        }
        String prompt = String.format(Resource.getInstance().getCorpus("report", "REPORT_SUMMARY", language),
                buf.toString());
        GeneratingRecord generating = this.service.syncGenerateText(authToken, ModelConfig.BAIZE_NEXT_UNIT, prompt.toString(),
                new GeneratingOption(), null, null);
        if (null != generating) {
            return generating.answer;
        }
        return null;
    }

    private String extract(String query) {
        Dataset dataset = Resource.getInstance().loadDataset();
        if (null == dataset) {
            Logger.w(this.getClass(), "#infer - Read dataset failed");
            return null;
        }

        synchronized (dataset) {
            if (!dataset.hasAnalyzed()) {
                for (String question : dataset.getQuestions()) {
                    TFIDFAnalyzer analyzer = new TFIDFAnalyzer(this.service.getTokenizer());
                    List<Keyword> keywordList = analyzer.analyze(question, 7);
                    if (keywordList.isEmpty()) {
                        continue;
                    }

                    List<String> keywords = new ArrayList<>();
                    for (Keyword keyword : keywordList) {
                        keywords.add(keyword.getWord());
                    }
                    // 填充问题关键词
                    dataset.fillQuestionKeywords(question, keywords.toArray(new String[0]));
                }
            }
        }

        TFIDFAnalyzer analyzer = new TFIDFAnalyzer(this.service.getTokenizer());
        List<Keyword> keywordList = analyzer.analyze(query, 5);
        if (keywordList.isEmpty()) {
            Logger.w(this.getClass(), "#infer - Query keyword is none");
            return null;
        }

        List<String> keywords = new ArrayList<>();
        for (Keyword keyword : keywordList) {
            keywords.add(keyword.getWord());
        }

        return dataset.matchContent(keywords.toArray(new String[0]), 5);
    }

    private String fixSecondPerson(String text, Language language) {
        if (language.isChinese()) {
            String result = text.replaceAll("你", "受测人");
            return result.replaceAll("您", "受测人");
        }
        else {
            String result = text.replaceAll("you", "subject");
            return result.replaceAll("You", "Subject");
        }
    }

    private String fixThirdPerson(String text) {
        String result = text.replaceAll("此人", "受测人");
        result = result.replaceAll("人物", "受测人");
        result = result.replaceAll("该个体", "受测人");

        result = result.replaceAll("This person", "The subject");
        result = result.replaceAll("The individual", "The subject");

        StringBuffer buf = new StringBuffer();
        List<String> words = this.service.segmentation(result);
        for (String word : words) {
            if (word.equals("他") || word.equals("她")) {
                buf.append("受测人");
            }
            else if (word.equals("他的") || word.equals("她的")) {
                buf.append("受测人的");
            }
            else if (word.equals("he") || word.equals("she")) {
                buf.append("the subject");
            }
            else if (word.equals("He") || word.equals("She")) {
                buf.append("The subject");
            }
            else if (word.equals("his") || word.equals("her")) {
                buf.append("the subject's");
            }
            else if (word.equals("His") || word.equals("Her")) {
                buf.append("The subject's");
            }
            else {
                buf.append(word);
            }
        }
        return buf.toString();
    }
}
