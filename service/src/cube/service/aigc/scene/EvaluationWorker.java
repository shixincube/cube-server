/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.scene;

import cell.util.log.Logger;
import cube.aigc.ModelConfig;
import cube.aigc.psychology.*;
import cube.aigc.psychology.algorithm.AttachmentStyle;
import cube.aigc.psychology.algorithm.BigFivePersonality;
import cube.aigc.psychology.algorithm.PersonalityAccelerator;
import cube.aigc.psychology.composition.*;
import cube.auth.AuthToken;
import cube.common.Language;
import cube.common.entity.AIGCChannel;
import cube.common.entity.GeneratingOption;
import cube.common.entity.GeneratingRecord;
import cube.service.aigc.AIGCService;
import cube.util.FloatUtils;
import org.json.JSONObject;

import java.util.*;

/**
 * 评估工作器。
 */
public class EvaluationWorker {

    private final static String REFINE_CN = "对以下内容进行优化改写，让内容更容易理解，不要编造成分，直接输出内容。需要优化的内容如下：\n\n%s\n";

    private final static String REFINE_EN = "The following content requires moderate refinement. Please do not alter the original meaning of the content or add any additional information. The content to be refined is as follows:\n\n%s\n";

    private final static String PERSONALITY_FORMAT_CN = "已知受测人的人格特点如下：\n\n%s\n\n根据上述信息回答问题，不能编造成分，问题是：总结受测人的人格特点。";

    private final static String PERSONALITY_FORMAT_EN = "The personality traits of the test subject are known as follows:\n\n" +
            "%s" +
            "\n\nAnswer the question based on the above information. Do not fabricate scores. The question is: Summarize the personality traits of the test subject.";

    private final static String SCENARIO_CN = "关于%s的描述如下：\n\n%s\n\n给定%s的相关信息如下：\n\n%s\n\n请将上述描述和信息相结合，说明此%s心理特征在%s会有什么样的内心独白和行为表现。";

    private final static String SCENARIO_EN = "";

    public final boolean fast = true;

    private EvaluationReport evaluationReport;

    private AIGCService service;

    private Attribute attribute;

    private HexagonDimensionScore dimensionScore;

    private HexagonDimensionScore normDimensionScore;

    private List<ReportSection> reportSectionList;

    private String summary = "";

    private List<JSONObject> keywords;

    private PaintingFeatureSet paintingFeatureSet;

    private String keyFeatureDescription = "";

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
        report.setKeyFeatureDescription(this.keyFeatureDescription);
        report.setKeywords(this.keywords);

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

                // 关键特征描述
                this.inferKeyFeatureDescription(channel.getAuthToken(), theme, this.evaluationReport.getKeyFeatures());

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
                        return (int)(es1.calcScore() * 1000 - es2.calcScore() * 1000);
                    }
                });
                EvaluationScore score = attachmentScores.get(attachmentScores.size() - 1);
                List<EvaluationScore> newAttachmentScores = new ArrayList<>();
                newAttachmentScores.add(score);
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
                this.summary = this.inferSummaryByTemplate(channel.getAuthToken(), this.reportSectionList, this.attribute.language);

                // 内容场景化
                ReportSection section = this.reportSectionList.get(0);
                this.reportSectionList.clear();
                AttachmentStyle style = AttachmentStyle.parse(section.indicator);
                if (null != style) {
                    // 关键词
                    List<String> words = this.inferAttachmentSceneWords(style, section.report);
                    if (null != words) {
                        this.keywords = new ArrayList<>();
                        for (String word : style.getKeywords(this.attribute.language)) {
                            JSONObject wordValue = new JSONObject();
                            wordValue.put("word", word);
                            if (words.contains(word)) {
                                wordValue.put("score", FloatUtils.random(0.5, 1.0));
                            }
                            else {
                                wordValue.put("score", FloatUtils.random(0.0, 0.4));
                            }
                            this.keywords.add(wordValue);
                        }
                    }

                    // 场景
                    String[] scenes = new String[] {
                            style.getScene1Title(this.attribute.language),
                            style.getScene2Title(this.attribute.language),
                            style.getScene3Title(this.attribute.language)
                    };
                    for (String sceneTitle : scenes) {
                        String sceneContent = ContentTools.extract(sceneTitle, this.service.getTokenizer());
                        if (null == sceneContent) {
                            Logger.d(this.getClass(), "#make - Extract dataset error: " + sceneTitle);
                            continue;
                        }
                        // 关于%s的描述如下：
                        // %s
                        // 给定%s的相关信息如下：
                        // %s
                        // 请将上述描述和信息相结合，说明此%s心理特征在%s会有什么样的内心独白和行为表现。
                        String prompt = String.format(this.attribute.language.isChinese() ? SCENARIO_CN : SCENARIO_EN,
                                section.title, section.report,
                                sceneTitle, sceneContent,
                                section.title, sceneTitle);
                        GeneratingRecord record = this.service.syncGenerateText(channel.getAuthToken(),
                                ModelConfig.BAIZE_NEXT_UNIT, prompt, new GeneratingOption(),
                                null, null);
                        if (null != record) {
                            ReportSection sceneSection = new ReportSection(section.indicator,
                                    sceneTitle, record.answer, "", section.rate);
                            this.reportSectionList.add(sceneSection);
                        }
                    }
                }
                break;
            case PersonInRain:
                // 处理关键特征描述
                for (KeyFeature keyFeature : this.evaluationReport.getKeyFeatures()) {
                    String prompt = String.format(this.attribute.language.isChinese() ? REFINE_CN : REFINE_EN,
                            keyFeature.getDescription());
                    GeneratingRecord record = this.service.syncGenerateText(channel.getAuthToken(),
                            ModelConfig.BAIZE_NEXT_UNIT, prompt, new GeneratingOption(),
                            null, null);
                    if (null != record) {
                        keyFeature.setDescription(record.answer);
                    }
                }

                // 生成概述
                this.summary = this.inferSummaryWithKeyFeatures(channel.getAuthToken(),
                        this.evaluationReport.getKeyFeatures(), this.attribute.language);
                break;
            default:
                Logger.e(this.getClass(), "#make - No theme: " + theme.name);
                break;
        }

        return this;
    }

    private List<String> inferAttachmentSceneWords(AttachmentStyle attachmentStyle, String content) {
        String formatContent = "";
        switch (attachmentStyle) {
            case Secure:
                formatContent = "SECURE_ATTACHMENT_SCENE_WORDS";
                break;
            case AnxiousPreoccupied:
                formatContent = "ANXIOUS_PREOCCUPIED_ATTACHMENT_SCENE_WORDS";
                break;
            case DismissiveAvoidant:
                formatContent = "DISMISSIVE_AVOIDANT_ATTACHMENT_SCENE_WORDS";
                break;
            case Disorganized:
                formatContent = "DISORGANIZED_ATTACHMENT_SCENE_WORDS";
                break;
            default:
                break;
        }
        String prompt = String.format(Resource.getInstance().getCorpus("report", formatContent,
                this.attribute.language), content);
        GeneratingRecord record = this.service.syncGenerateText(ModelConfig.BAIZE_NEXT_UNIT, prompt,
                new GeneratingOption(), null, null);
        if (null != record) {
            String separator = record.answer.contains(",") ? "," : "，";
            String[] words = record.answer.split(separator);
            if (words.length > 1) {
                return Arrays.asList(words);
            }
        }
        return null;
    }

    /**
     * 推理关键特征描述。
     *
     * @param authToken
     * @param keyFeatures
     * @return
     */
    private void inferKeyFeatureDescription(AuthToken authToken, Theme theme, List<KeyFeature> keyFeatures) {
        StringBuilder content = new StringBuilder();

        for (KeyFeature keyFeature : keyFeatures) {
            content.append("## ").append(keyFeature.getName()).append("\n\n");

            String prompt = keyFeature.makePrompt(theme, this.attribute);
            GeneratingRecord record = this.service.syncGenerateText(authToken, ModelConfig.BAIZE_NEXT_UNIT, prompt,
                    new GeneratingOption(), null, null);
            if (null == record) {
                Logger.w(this.getClass(), "#inferKeyFeatureDescription - Generating failed");
                content.append(keyFeature.getDescription()).append("\n\n");
            }
            else {
                content.append(record.answer).append("\n\n");
            }
        }

        this.keyFeatureDescription = content.toString();
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
            answer = ContentTools.extract(prompt, this.service.getTokenizer());
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
            answer = ContentTools.extract(prompt, this.service.getTokenizer());
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
        feature.setObligingnessParaphrase(ContentTools.extract(feature.getObligingnessPrompt(), this.service.getTokenizer()));

        // 尽责性
        prompt = feature.generateConscientiousnessPrompt();
        answer = null;
        if (this.fast) {
            Logger.d(this.getClass(), "#inferPersonality - Conscientiousness prompt: \"" + prompt + "\"");
            answer = ContentTools.extract(prompt, this.service.getTokenizer());
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
        feature.setConscientiousnessParaphrase(
                ContentTools.extract(feature.getConscientiousnessPrompt(), this.service.getTokenizer()));

        // 外向性
        prompt = feature.generateExtraversionPrompt();
        answer = null;
        if (this.fast) {
            Logger.d(this.getClass(), "#inferPersonality - Extraversion prompt: \"" + prompt + "\"");
            answer = ContentTools.extract(prompt, this.service.getTokenizer());
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
        feature.setExtraversionParaphrase(ContentTools.extract(feature.getExtraversionPrompt(), this.service.getTokenizer()));

        // 进取性
        prompt = feature.generateAchievementPrompt();
        answer = null;
        if (this.fast) {
            Logger.d(this.getClass(), "#inferPersonality - Achievement prompt: \"" + prompt + "\"");
            answer = ContentTools.extract(prompt, this.service.getTokenizer());
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
        feature.setAchievementParaphrase(ContentTools.extract(feature.getAchievementPrompt(), this.service.getTokenizer()));

        // 情绪性
        prompt = feature.generateNeuroticismPrompt();
        answer = null;
        if (this.fast) {
            Logger.d(this.getClass(), "#inferPersonality - Neuroticism prompt: \"" + prompt + "\"");
            answer = ContentTools.extract(prompt, this.service.getTokenizer());
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
        feature.setNeuroticismParaphrase(ContentTools.extract(feature.getNeuroticismPrompt(), this.service.getTokenizer()));

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
                report = ContentTools.extract(prompt, this.service.getTokenizer());
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
                suggestion = ContentTools.extract(prompt, this.service.getTokenizer());
            }
            if (null == suggestion) {
                Logger.w(this.getClass(), "#inferScore - No suggestion for \"" + prompt + "\"");
                GeneratingRecord generating = this.service.syncGenerateText(ModelConfig.BAIZE_NEXT_UNIT, prompt, new GeneratingOption(),
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
        StringBuilder result = new StringBuilder();
        for (ReportSection section : list) {
            StringBuilder buf = new StringBuilder();
            buf.append("您是").append(section.title).append("。\n\n");
            buf.append(section.title).append("包含以下描述：\n\n");
            buf.append(section.report).append("\n\n");
            // make prompt
            String prompt = String.format(Resource.getInstance().getCorpus("report", "REPORT_SUMMARY", language),
                    section.title + "表现特征", buf.toString());
            GeneratingRecord generating = this.service.syncGenerateText(authToken, ModelConfig.BAIZE_NEXT_UNIT, prompt,
                    new GeneratingOption(), null, null);
            if (null != generating) {
                result.append(generating.answer).append("\n\n");
            }
        }
        result.delete(result.length() - 2, result.length());
        return result.toString();
    }

    private String inferSummaryWithKeyFeatures(AuthToken authToken, List<KeyFeature> list, Language language) {
        StringBuilder buf = new StringBuilder();

        buf.append("绘画里的关键特征有：\n\n");
        for (KeyFeature keyFeature : list) {
            buf.append(keyFeature.getDescription());
            buf.append("\n\n");
        }

        // make prompt
        String prompt = String.format(Resource.getInstance().getCorpus("report", "REPORT_SUMMARY", language),
                "绘画的主要特征描述", buf.toString());
        GeneratingRecord generating = this.service.syncGenerateText(authToken, ModelConfig.BAIZE_NEXT_UNIT, prompt,
                new GeneratingOption(), null, null);

        if (null != generating) {
            return generating.answer;
        }

        return null;
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
