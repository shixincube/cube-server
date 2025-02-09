/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.scene;

import cell.util.log.Logger;
import cube.aigc.psychology.Dataset;
import cube.aigc.psychology.EvaluationReport;
import cube.aigc.psychology.PaintingReport;
import cube.aigc.psychology.Resource;
import cube.aigc.psychology.algorithm.Attention;
import cube.aigc.psychology.algorithm.BigFivePersonality;
import cube.aigc.psychology.algorithm.PersonalityAccelerator;
import cube.aigc.psychology.composition.*;
import cube.service.tokenizer.Tokenizer;
import cube.service.tokenizer.keyword.TFIDFAnalyzer;

import java.util.List;

public class PsychologyHelper {

    private PsychologyHelper() {
    }

    public static void fillDimensionScoreDescription(Tokenizer tokenizer, HexagonDimensionScore sds) {
        TFIDFAnalyzer analyzer = new TFIDFAnalyzer(tokenizer);
        for (HexagonDimension dim : HexagonDimension.values()) {
            int score = sds.getDimensionScore(dim);
            String query = null;
            int rate = 0;
            if (score <= 60) {
                query = "六维分析中" + dim.displayName + "维度得分低的表现";
                rate = 1;
            } else if (score >= 90) {
                query = "六维分析中" + dim.displayName + "维度得分高的表现";
                rate = 3;
            } else {
                query = "六维分析中" + dim.displayName + "维度得分中等的表现";
                rate = 2;
            }

            List<String> keywordList = analyzer.analyzeOnlyWords(query, 7);

            Dataset dataset = Resource.getInstance().loadDataset();
            String answer = dataset.matchContent(keywordList.toArray(new String[0]), 7);
            if (null != answer) {
                sds.record(dim, rate, answer);
            }
            else {
                Logger.e(PsychologyHelper.class, "#fillDimensionScoreDescription - Answer is null: " + query);
            }
        }
    }

    public static String makeMarkdown(PaintingReport report) {
        StringBuilder buf = new StringBuilder();
        if (report.isNull()) {
            buf.append("根据您提供的绘画文件，我没有发现有效的心理投射内容，建议您检查一下图片文件内容。");
            return buf.toString();
        }

        EvaluationReport evalReport = report.getEvaluationReport();
        buf.append("根据您提供的绘画图片，");
        if (evalReport.isHesitating()) {
            buf.append("我发现画面内容并不容易被识别。");
        }
        else {
            buf.append("我已经按照心理学绘画投射理论进行了解读。");
        }
        buf.append("这位**").append(report.getAttribute().getAgeText()).append("**的**")
                .append(report.getAttribute().getGenderText()).append("性**")
                .append("被测人，");
        buf.append("在这幅绘画中投射出了").append(evalReport.numRepresentations()).append("个心理表征，");
        buf.append("相关的评测报告内容如下：\n\n");

        buf.append("# 概述\n\n");
        buf.append(report.getSummary()).append("\n\n");

        if (evalReport.numEvaluationScores() > 0) {
            buf.append("# 指标数据\n\n");
            for (EvaluationScore score : evalReport.getEvaluationScores()) {
                ReportSection section = report.getReportSection(score.indicator);
                if (null == section) {
                    continue;
                }

                buf.append("## ").append(section.title).append("\n\n");
                buf.append("* **评级** ：").append(score.rate.value).append("级 （").append(score.rate.displayName).append("）\n\n");
                buf.append("**【描述】**\n\n").append(section.report).append("\n\n");
                buf.append("**【建议】**\n\n").append(section.suggestion).append("\n\n");
            }
            buf.append("\n");
        }

        PersonalityAccelerator personality = evalReport.getPersonalityAccelerator();
        if (null != personality) {
            BigFivePersonality bigFivePersonality = personality.getBigFivePersonality();
            buf.append("# 大五人格\n\n");
            buf.append("**【人格画像】** ：**").append(bigFivePersonality.getDisplayName()).append("**。\n\n");
            buf.append("**【人格描述】** ：\n\n").append(bigFivePersonality.getDescription()).append("\n\n");
            buf.append("**【维度描述】** ：\n\n");
            buf.append("* **宜人性**（").append(bigFivePersonality.getObligingness()).append("）\n");
            buf.append(bigFivePersonality.getObligingnessContent()).append("\n\n");
            buf.append("* **尽责性**（").append(bigFivePersonality.getConscientiousness()).append("）\n");
            buf.append(bigFivePersonality.getConscientiousnessContent()).append("\n\n");
            buf.append("* **外向性**（").append(bigFivePersonality.getExtraversion()).append("）\n");
            buf.append(bigFivePersonality.getExtraversionContent()).append("\n\n");
            buf.append("* **进取性**（").append(bigFivePersonality.getAchievement()).append("）\n");
            buf.append(bigFivePersonality.getAchievementContent()).append("\n\n");
            buf.append("* **情绪性**（").append(bigFivePersonality.getNeuroticism()).append("）\n");
            buf.append(bigFivePersonality.getNeuroticismContent()).append("\n\n");
        }

        buf.append("综上所述，通过各项评测描述可以帮助被测人对自身有一个清晰、客观、全面的认识，从而进行科学、有效的管理。通过对自身的心理状态、人格特质等方面的了解，认识到更多的可能性，从而对生活和工作方向提供参考。");
        if (evalReport.getAttention().level <= Attention.GeneralAttention.level && !evalReport.isHesitating()) {
            buf.append("被测人目前的心理状态尚可，应当积极保持良好的作息和积极的生活、工作习惯。遇到困难积极应对。");
        }
        else {
            buf.append("被测人应当关注自己最近的心理状态变化，如果有需要应当积极需求帮助。");
        }
        buf.append("\n\n");

        buf.append("需要注意的是：\n\n");
        buf.append("1. **不要将测试结果当作永久的“标签”。**测试的结果仅仅是根据最近一周或者近期的感觉，其结果也只是表明短期内的心理健康状态，是可以调整变化的，不必产生心理负担。\n\n");
        buf.append("2. **报告结果没有“好”与“坏”之分。**报告结果与个人道德品质无关，只反映你目前的心理状态，但不同的特点对于不同的工作、生活状态会存在“合适”和“不合适”的区别，从而表现出具体条件的优势和劣势。\n\n");
        buf.append("3. **以整体的观点来看待测试结果。**很多测验都包含多个分测验，对于这类测验来说，不应该孤立地理解单个分测验的成绩。在评定一个人的特征时，一方面需要理解每一个分测验分数的意义，但更重要的是综合所有信息全面分析。\n\n");

        return buf.toString();
    }

    public static String makeMarkdown(PaintingFeatureSet featureSet) {
        StringBuilder buf = new StringBuilder();
        buf.append("我看到的");
        buf.append(featureSet.makeMarkdown(false));
        buf.append("\n");
        buf.append("根据上述特征，我需要结合专业的知识结构，并根据对应症状的得分进行阐述并生成报告。");
        return buf.toString();
    }
}
