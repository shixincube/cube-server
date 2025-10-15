/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.scene;

import cell.util.Utils;
import cell.util.log.Logger;
import cube.aigc.ModelConfig;
import cube.aigc.psychology.*;
import cube.aigc.psychology.algorithm.BigFivePersonality;
import cube.aigc.psychology.algorithm.PersonalityAccelerator;
import cube.aigc.psychology.composition.*;
import cube.common.entity.GeneratingOption;
import cube.common.entity.GeneratingRecord;
import cube.service.aigc.AIGCService;
import cube.service.tokenizer.keyword.Keyword;
import cube.service.tokenizer.keyword.TFIDFAnalyzer;
import cube.util.FileUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 评估工作器。
 */
public class EvaluationWorker {

    private final static String PERSONALITY_FORMAT = "已知受测人的人格特点如下：\n\n%s\n\n根据上述信息回答问题，不能编造成分，问题是：总结受测人的人格特点。";

    private boolean speed = true;

    private EvaluationReport evaluationReport;

    private AIGCService service;

    private Attribute attribute;

    private HexagonDimensionScore dimensionScore;

    private HexagonDimensionScore normDimensionScore;

    private List<ReportSection> reportTextList;

    private String summary = "";

    private MandalaFlower mandalaFlower = new MandalaFlower("AS_001");

    private PaintingFeatureSet paintingFeatureSet;

    public EvaluationWorker(AIGCService service, Attribute attribute) {
        this.service = service;
        this.attribute = attribute;
    }

    public EvaluationWorker(EvaluationReport evaluationReport, AIGCService service) {
        this.attribute = evaluationReport.getAttribute();
        this.evaluationReport = evaluationReport;
        this.service = service;
        this.reportTextList = new ArrayList<>();
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

    public boolean isSpeed() {
        return this.speed;
    }

    public PaintingReport fillReport(PaintingReport report) {
        report.setEvaluationReport(this.evaluationReport);

        if (null != this.dimensionScore && null != this.normDimensionScore) {
            report.setDimensionalScore(this.dimensionScore, this.normDimensionScore);
        }

        report.setSummary(this.summary);
        report.setReportTextList(this.reportTextList);
        report.setMandalaFlower(this.mandalaFlower);

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

    public EvaluationWorker make(Theme theme, int maxIndicatorTexts) {
        // 评估分推理
        List<EvaluationScore> scoreList = this.evaluationReport.getEvaluationScoresByRepresentation(Indicator.values().length);
        this.reportTextList = this.inferScore(scoreList, maxIndicatorTexts);
        if (this.reportTextList.isEmpty()) {
            Logger.w(this.getClass(), "#make - Report text error");
            return this;
        }

        if (Logger.isDebugLevel()) {
            Logger.d(this.getClass(), "#make - Report list size: " + this.reportTextList.size());
        }

        // 生成概述
        this.summary = this.inferSummary(this.reportTextList);

        // 生成人格描述
        this.inferPersonality(this.evaluationReport.getPersonalityAccelerator());

        // 六维得分计算
        try {
            this.dimensionScore = new HexagonDimensionScore(this.evaluationReport.getAttention(),
                    this.evaluationReport.getFullEvaluationScores(),
                    this.evaluationReport.getPaintingConfidence(), this.evaluationReport.getFactorSet());
            this.normDimensionScore = new HexagonDimensionScore(
                    80, 80, 80, 80, 80, 80);
            // 描述
            ContentTools.fillHexagonScoreDescription(this.service.getTokenizer(), this.dimensionScore);
        } catch (Exception e) {
            Logger.w(this.getClass(), "#make", e);
        }

        // 曼陀罗花
        this.mandalaFlower = this.inferMandalaFlower(this.evaluationReport.getPersonalityAccelerator());

        return this;
    }

    private MandalaFlower inferMandalaFlower(PersonalityAccelerator personalityAccelerator) {
        BigFivePersonality feature = personalityAccelerator.getBigFivePersonality();
        List<String> filenames = Resource.getInstance().getMandalaFlowerFiles();
        String color = "A";
        if (feature.getNeuroticism() < 3.0) {
            color = "B";
        }
        else if (feature.getNeuroticism() < 4.0) {
            color = "G";
        }
        else if (feature.getNeuroticism() < 5.0) {
            color = "Y";
        }
        else if (feature.getNeuroticism() < 6.0) {
            color = "R";
        }
        else {
            color = "P";
        }

        List<String> filenameList = new ArrayList<>();
        for (String filename : filenames) {
            if (filename.startsWith(color)) {
                filenameList.add(filename);
            }
        }
        MandalaFlower mandalaFlower = new MandalaFlower(
                FileUtils.extractFileName(filenameList.get(Utils.randomInt(0, filenameList.size() - 1))));
        return mandalaFlower;
    }

    /**
     * 推理人格。
     *
     * @param personalityAccelerator
     * @return
     */
    private boolean inferPersonality(PersonalityAccelerator personalityAccelerator) {
        BigFivePersonality feature = personalityAccelerator.getBigFivePersonality();
        String prompt = feature.generateReportPrompt();
        String answer = null;
        if (this.speed) {
            answer = this.infer(prompt);
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

        // 对数据集数据进行推理
        prompt = String.format(PERSONALITY_FORMAT, fixSecondPerson(answer));
        GeneratingRecord generatingResult = this.service.syncGenerateText(ModelConfig.BAIZE_NEXT_UNIT, prompt, new GeneratingOption(),
                null, null);
        String fixAnswer = (null != generatingResult) ? generatingResult.answer : null;
        if (null != fixAnswer) {
            answer = fixThirdPerson(fixAnswer);
        }
        // 设置人格画像描述
        feature.setDescription(answer);

        // 宜人性
        prompt = feature.generateObligingnessPrompt();
        answer = null;
        if (this.speed) {
            Logger.d(this.getClass(), "#inferPersonality - Obligingness prompt: \"" + prompt + "\"");
            answer = this.infer(prompt);
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
        feature.setObligingnessParaphrase(this.infer(feature.getObligingnessPrompt()));

        // 尽责性
        prompt = feature.generateConscientiousnessPrompt();
        answer = null;
        if (this.speed) {
            Logger.d(this.getClass(), "#inferPersonality - Conscientiousness prompt: \"" + prompt + "\"");
            answer = this.infer(prompt);
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
        feature.setConscientiousnessParaphrase(this.infer(feature.getConscientiousnessPrompt()));

        // 外向性
        prompt = feature.generateExtraversionPrompt();
        answer = null;
        if (this.speed) {
            Logger.d(this.getClass(), "#inferPersonality - Extraversion prompt: \"" + prompt + "\"");
            answer = this.infer(prompt);
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
        feature.setExtraversionParaphrase(this.infer(feature.getExtraversionPrompt()));

        // 进取性
        prompt = feature.generateAchievementPrompt();
        answer = null;
        if (this.speed) {
            Logger.d(this.getClass(), "#inferPersonality - Achievement prompt: \"" + prompt + "\"");
            answer = this.infer(prompt);
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
        feature.setAchievementParaphrase(this.infer(feature.getAchievementPrompt()));

        // 情绪性
        prompt = feature.generateNeuroticismPrompt();
        answer = null;
        if (this.speed) {
            Logger.d(this.getClass(), "#inferPersonality - Neuroticism prompt: \"" + prompt + "\"");
            answer = this.infer(prompt);
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
        feature.setNeuroticismParaphrase(this.infer(feature.getNeuroticismPrompt()));

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

            if (this.speed) {
                report = this.infer(prompt);
            }
            if (null == report) {
                Logger.w(this.getClass(), "#inferScore - No report for \"" + prompt + "\"");
                GeneratingRecord generating = this.service.syncGenerateText(ModelConfig.BAIZE_UNIT, prompt, new GeneratingOption(),
                        null, null);
                report = (null != generating) ? generating.answer : null;
            }

            prompt = es.generateSuggestionPrompt(this.attribute);
            if (null == prompt) {
                // 不进行推理，下一个
                continue;
            }

            String suggestion = null;

            if (this.speed) {
                suggestion = this.infer(prompt);
            }
            if (null == suggestion) {
                Logger.w(this.getClass(), "#inferScore - No suggestion for \"" + prompt + "\"");
                GeneratingRecord generating = this.service.syncGenerateText(ModelConfig.BAIZE_UNIT, prompt, new GeneratingOption(),
                        null, null);
                suggestion = (null != generating) ? generating.answer : null;
            }

            if (null != report && null != suggestion) {
                result.add(new ReportSection(es.indicator, es.generateWord(this.attribute),
                        report, suggestion, es.getIndicatorRate(this.attribute)));
                if (result.size() >= maxIndicatorTexts) {
                    break;
                }
            }
        }

        return result;
    }

    private String inferSummary(List<ReportSection> list) {
        if (list.isEmpty()) {
            return Resource.getInstance().getCorpus("report", "REPORT_NO_DATA_SUMMARY");
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
        StringBuilder prompt = new StringBuilder("已知信息：\n\n");
        prompt.append("受测人心理评测结果如下：\n\n");
        for (ReportSection rs : list) {
//            prompt.append("* **").append(rs.title).append("** ：");
            prompt.append(fixSecondPerson(rs.report)).append("\n\n");
            if (prompt.length() >= ModelConfig.getPromptLengthLimit(unitName)) {
                break;
            }
        }
        prompt.append("\n");
        prompt.append("根据上述已知信息，简洁和专业地来回答用户的问题。问题是：概述此人的心理评测结果，各内容之间分段展示。");
        GeneratingRecord generating = this.service.syncGenerateText(unitName, prompt.toString(), new GeneratingOption(),
                null, null);
        String result = (null != generating) ? generating.answer : null;

        if (null == result || result.contains("我遇到一些问题") || result.contains("我遇到一些技术问题")) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            generating = this.service.syncGenerateText(ModelConfig.BAIZE_NEXT_UNIT, prompt.toString(), new GeneratingOption(),
                    null, null);
            result = (null != generating) ? generating.answer : null;
        }

        if (null != result) {
            summary.append(result);
        }

        return fixThirdPerson(summary.toString());
    }

    private String infer(String query) {
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

    private String fixSecondPerson(String text) {
        String result = text.replaceAll("你", "受测人");
        return result.replaceAll("您", "受测人");
    }

    private String fixThirdPerson(String text) {
        String result = text.replaceAll("此人", "受测人");
        result = result.replaceAll("人物", "受测人");
        result = result.replaceAll("该个体", "受测人");

        StringBuffer buf = new StringBuffer();
        List<String> words = this.service.segmentation(result);
        for (String word : words) {
            if (word.equals("他")) {
                buf.append("受测人");
            }
            else if (word.equals("他的")) {
                buf.append("受测人的");
            }
            else {
                buf.append(word);
            }
        }
        return buf.toString();
    }
}
