/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.scene;

import cell.core.net.Endpoint;
import cell.util.log.Logger;
import cube.aigc.Consts;
import cube.aigc.psychology.*;
import cube.aigc.psychology.algorithm.*;
import cube.aigc.psychology.app.Link;
import cube.aigc.psychology.composition.*;
import cube.common.entity.AIGCChannel;
import cube.common.entity.Membership;
import cube.common.entity.User;
import cube.service.tokenizer.Tokenizer;
import cube.service.tokenizer.keyword.Keyword;
import cube.service.tokenizer.keyword.TFIDFAnalyzer;
import cube.util.FileLabels;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class ContentTools {

    public static final SimpleDateFormat gsDateFormat = new SimpleDateFormat("yyyy年MM月dd日HH时");

    public static final SimpleDateFormat gsShortDateFormat = new SimpleDateFormat("yyyy-MM-dd");

    private ContentTools() {
    }

    public static void fillHexagonScoreDescription(Tokenizer tokenizer, HexagonDimensionScore sds) {
        TFIDFAnalyzer analyzer = new TFIDFAnalyzer(tokenizer);
        for (HexagonDimension dim : HexagonDimension.values()) {
            int score = sds.getDimensionScore(dim);
            String query = null;
            int rate = sds.getDimensionRate(dim);
            if (rate == IndicatorRate.None.value) {
                query = "六维分析中" + dim.displayName + "维度表现常规";
            } else if (rate <= IndicatorRate.Low.value) {
                query = "六维分析中" + dim.displayName + "维度得分低的表现";
            } else if (score >= IndicatorRate.High.value) {
                query = "六维分析中" + dim.displayName + "维度得分高的表现";
            } else {
                query = "六维分析中" + dim.displayName + "维度得分中等的表现";
            }

            List<String> keywordList = analyzer.analyzeOnlyWords(query, 7);

            Dataset dataset = Resource.getInstance().loadDataset();
            String answer = dataset.matchContent(keywordList.toArray(new String[0]), 7);
            if (null != answer) {
                sds.recordDescription(dim, answer);
            }
            else {
                Logger.e(ContentTools.class, "#fillHexagonScoreDescription - Answer is null: " + query);
            }
        }
    }

    public static String makeBrief(AIGCChannel channel, PaintingReport report) {
        StringBuilder buf = new StringBuilder();

        if (report.isNull()) {
            buf.append("根据提供的绘画文件，绘画里没有发现有效的心理投射内容，建议检查一下绘画文件内容。");
            return buf.toString();
        }

        EvaluationReport evalReport = report.getEvaluationReport();
        buf.append("根据评测的绘画内容，");
        if (evalReport.isHesitating()) {
            buf.append("绘画画面内容并不容易被识别。");
        }
        else {
            buf.append("按照心理学绘画投射理论进行解读。\n\n");
        }
        buf.append("受测人为**").append(report.getAttribute().getAgeText()).append("**的**")
                .append(report.getAttribute().getGenderText()).append("性**。")
                .append("评测日期是**")
                .append(gsDateFormat.format(new Date(report.timestamp))).append("**。\n\n");
        buf.append("在这幅绘画中投射出了**").append(evalReport.numRepresentations()).append("个心理表征**。\n\n");

        ReportPermission permission = report.getPermission();

        // 图片
        buf.append("**绘画图片**\n\n");
        if (permission.file) {
            buf.append("![绘画](");
            buf.append(FileLabels.makeFileHttpsURL(report.getFileLabel(),
                    channel.getAuthToken().getCode(), channel.getHttpsEndpoint()));
            buf.append(")\n\n");
        }
        else {
            buf.append("***暂时无法查看***。\n\n");
        }

        return buf.toString();
    }

    public static String makeSummary(AIGCChannel channel, PaintingReport report) {
        StringBuilder buf = new StringBuilder();
        buf.append(makeBrief(channel, report));

        ReportPermission permission = report.getPermission();
        buf.append("## 概述\n\n");
        if (permission.indicatorSummary) {
            buf.append(report.getSummary());
            buf.append("\n\n");
        }
        else {
            buf.append(clipContentByLines(report.getSummary(), 20));
        }

        return buf.toString();
    }

    public static String makeRatingInformation(PaintingReport report) {
        StringBuilder buf = new StringBuilder();

        buf.append("# 关注等级与建议\n\n");
        Attention attention = report.getEvaluationReport().getAttention();
        buf.append("**关注等级**：");
        if (report.getPermission().attention) {
            buf.append(" ***").append(attention.name).append("***\n\n");
            buf.append(attention.name).append(attention.description).append("\n\n");
        }
        else {
            buf.append(clipContent(""));
        }

        Suggestion suggestion = report.getEvaluationReport().getSuggestion();
        buf.append("**建议**：");
        if (report.getPermission().suggestion) {
            buf.append(" ***").append(suggestion.title).append("***\n\n");
            buf.append(suggestion.description).append("\n\n");
        }
        else {
            buf.append(clipContent(""));
        }

        return buf.toString();
    }

    public static String makePageLink(Endpoint endpoint, String token, PaintingReport report,
                                         boolean indicatorLink, boolean personalityLink) {
        StringBuilder buf = new StringBuilder();

        if (indicatorLink) {
            if (report.getPermission().indicatorDetails) {
                buf.append("[查看数据指标](");
                buf.append("https://");
                buf.append(endpoint.toString());
                buf.append("/aigc/psychology/report/page/");
                buf.append(token);
                buf.append("/?page=indicator&sn=");
                buf.append(report.sn);
                buf.append(")\n");
            }
            else {
                buf.append(tipContent("了解如何查看数据指标"));
            }
        }

        if (personalityLink) {
            if (indicatorLink) {
                buf.append("\n");
            }

            if (report.getPermission().personalityDetails) {
                buf.append("[查看人格特质](");
                buf.append("https://");
                buf.append(endpoint.toString());
                buf.append("/aigc/psychology/report/page/");
                buf.append(token);
                buf.append("/?page=bigfive&sn=");
                buf.append(report.sn);
                buf.append(")\n");
            }
            else {
                buf.append(tipContent("了解如何查看人格特质"));
            }
        }

        return buf.toString();
    }

    public static String makeContent(PaintingReport report, boolean summary, int maxIndicators,
                                     boolean personality) {
        StringBuilder buf = new StringBuilder();
        if (report.isNull()) {
            buf.append("根据提供的绘画文件，绘画里没有发现有效的心理投射内容，建议检查一下绘画文件内容。\n");
            return buf.toString();
        }

        EvaluationReport evalReport = report.getEvaluationReport();
        buf.append("根据评测的绘画图片，");
        if (evalReport.isHesitating()) {
            buf.append("绘画画面内容要素较特别。\n\n");
        }
        else {
            buf.append("按照心理学绘画投射理论进行解读。\n\n");
        }
        buf.append("受测人为**").append(report.getAttribute().getAgeText()).append("**的**")
                .append(report.getAttribute().getGenderText()).append("性**。")
                .append("评测日期是**")
                .append(gsDateFormat.format(new Date(report.timestamp))).append("**。\n\n");
        buf.append("在这幅绘画中分析出了").append(evalReport.numRepresentations()).append("个心理表征。\n\n");

        if (summary) {
            buf.append("# 概述\n\n");
            if (report.getPermission().indicatorSummary) {
                buf.append(report.getSummary()).append("\n\n");
            }
            else {
                buf.append(clipContentByLines(report.getSummary(), 20));
            }
        }

        int numIndicators = 0;
        if (evalReport.numEvaluationScores() > 0 && maxIndicators > 0) {
            buf.append("# 指标因子\n\n");
            for (EvaluationScore score : evalReport.getEvaluationScores()) {
                ReportSection section = report.getReportSection(score.indicator);
                if (null == section) {
                    continue;
                }

                if (report.getPermission().indicatorDetails) {
                    buf.append("## ").append(section.title).append("\n\n");
                    buf.append("* **评级** ：").append(score.rate.value).append("级 （").append(score.rate.displayName).append("）\n\n");
                    buf.append("**【描述】**\n\n").append(section.report).append("\n\n");
                    buf.append("**【建议】**\n\n").append(section.suggestion).append("\n\n");
                }
                else {
                    buf.append("## ").append(clipContent(section.title, false));
                    buf.append("* **评级** ：").append(clipContent("", false));
                    buf.append("**【描述】**\n\n").append(clipContent(section.report, false));
                    buf.append("**【建议】**\n\n").append(clipContent(section.suggestion, false));
                }

                ++numIndicators;
                if (numIndicators >= maxIndicators) {
                    break;
                }
            }
            if (!report.getPermission().indicatorDetails) {
                buf.append(clipContent(""));
            }
        }

        if (personality) {
            PersonalityAccelerator personalityAccelerator = evalReport.getPersonalityAccelerator();
            if (null != personalityAccelerator) {
                BigFivePersonality bigFivePersonality = personalityAccelerator.getBigFivePersonality();
                buf.append("# 人格特质（大五人格）\n\n");

                if (report.getPermission().personalityPortrait) {
                    buf.append("**【人格画像】** ：**").append(bigFivePersonality.getDisplayName()).append("**。\n\n");
                    buf.append("**【人格描述】** ：\n\n").append(bigFivePersonality.getDescription()).append("\n\n");
                }
                else {
                    buf.append("**【人格画像】** ：**").append(clipContent(bigFivePersonality.getDisplayName(), false));
                    buf.append("**【人格描述】** ：\n\n").append(clipContent(bigFivePersonality.getDescription()));
                }

                buf.append("大五人格理论，通过宜人性、尽责性、外向性、进取性和情绪性五个维度的评测，直观地展示受测人在五个维度上的得分情况，有助于更清晰地认识受测人的性格轮廓。以下是五个维度数据：\n\n");

                if (report.getPermission().personalityDetails) {
                    buf.append("### **宜人性** （")
                            .append(String.format("%.1f", bigFivePersonality.getObligingness())).append("）\n\n");
                    buf.append("* **评级** ：")
                            .append(evalPersonalityScore(bigFivePersonality.getObligingness())).append("\n\n");
                    buf.append(bigFivePersonality.getObligingnessContent()).append("\n\n");

                    buf.append("### **尽责性** （")
                            .append(String.format("%.1f", bigFivePersonality.getConscientiousness())).append("）\n\n");
                    buf.append("* **评级** ：")
                            .append(evalPersonalityScore(bigFivePersonality.getConscientiousness())).append("\n\n");
                    buf.append(bigFivePersonality.getConscientiousnessContent()).append("\n\n");

                    buf.append("### **外向性** （")
                            .append(String.format("%.1f", bigFivePersonality.getExtraversion())).append("）\n\n");
                    buf.append("* **评级** ：")
                            .append(evalPersonalityScore(bigFivePersonality.getExtraversion())).append("\n\n");
                    buf.append(bigFivePersonality.getExtraversionContent()).append("\n\n");

                    buf.append("### **进取性** （")
                            .append(String.format("%.1f", bigFivePersonality.getAchievement())).append("）\n\n");
                    buf.append("* **评级** ：")
                            .append(evalPersonalityScore(bigFivePersonality.getAchievement())).append("\n\n");
                    buf.append(bigFivePersonality.getAchievementContent()).append("\n\n");

                    buf.append("### **情绪性** （")
                            .append(String.format("%.1f", bigFivePersonality.getNeuroticism())).append("）\n\n");
                    buf.append("* **评级** ：")
                            .append(evalPersonalityScore(bigFivePersonality.getNeuroticism())).append("\n\n");
                    buf.append(bigFivePersonality.getNeuroticismContent()).append("\n\n");
                }
                else {
                    buf.append("### **宜人性** （").append("...").append("）\n\n");
                    buf.append("* **评级** ：").append(clipContent("", false));
                    buf.append(clipContent(bigFivePersonality.getObligingnessContent()));

                    buf.append("### **尽责性** （").append("...").append("）\n\n");
                    buf.append("* **评级** ：").append(clipContent("", false));
                    buf.append(clipContent(bigFivePersonality.getConscientiousnessContent()));

                    buf.append("### **外向性** （").append("...").append("）\n\n");
                    buf.append("* **评级** ：").append(clipContent("", false));
                    buf.append(clipContent(bigFivePersonality.getExtraversionContent()));

                    buf.append("### **进取性** （").append("...").append("）\n\n");
                    buf.append("* **评级** ：").append(clipContent("", false));
                    buf.append(clipContent(bigFivePersonality.getAchievementContent()));

                    buf.append("### **情绪性** （").append("...").append("）\n\n");
                    buf.append("* **评级** ：").append(clipContent("", false));
                    buf.append(clipContent(bigFivePersonality.getNeuroticismContent()));
                }
            }
        }

        if (maxIndicators > 0 || personality) {
            buf.append("综上所述，通过各项评测描述可以帮助受测人对自身有一个清晰、客观、全面的认识，从而进行科学、有效的管理。通过对自身的心理状态、人格特质等方面的了解，认识到更多的可能性，从而对生活和工作方向提供参考。\n\n");

            if (report.getPermission().attention || report.getPermission().suggestion) {
                // 关注等级或者建议有授权
                if (evalReport.getAttention().level <= Attention.GeneralAttention.level && !evalReport.isHesitating()) {
                    buf.append("本次评测中，**受测人目前的心理状态尚可，应当积极保持良好的作息和积极的生活、工作习惯。遇到困难可积极应对。**");
                }
                else {
                    buf.append("本次评测中，**受测人应当关注自己近期的心理状态变化，如果有需要应当积极需求帮助。**");
                }
                buf.append("\n\n");
            }

            if (report.getPermission().indicatorDetails || report.getPermission().personalityDetails) {
                buf.append("对于本次评测，您还需要知道的是：\n\n");
                buf.append("1. **不要将测试结果当作永久的“标签”。** 测试的结果仅仅是根据最近一周或者近期的感觉，其结果也只是表明短期内的心理健康状态，是可以调整变化的，不必产生心理负担。\n\n");
                buf.append("2. **评测结果没有“好”与“坏”之分。** 评测结果与个人道德品质无关，只反映你目前的心理状态，但不同的特点对于不同的工作、生活状态会存在“合适”和“不合适”的区别，从而表现出具体条件的优势和劣势。\n\n");
                buf.append("3. **以整体的观点来看待测试结果。** 很多测验都包含多个分测验，对于这类测验来说，不应该孤立地理解单个分测验的成绩。在评定一个人的特征时，一方面需要理解每一个分测验分数的意义，但更重要的是综合所有信息全面分析。\n\n");
            }
        }

        return buf.toString();
    }

    private static String evalPersonalityScore(double score) {
        if (score <= 3.5) {
            return "低";
        }
        else if (score >= 7.5) {
            return "高";
        }
        else {
            return "中";
        }
    }

    public static String makePaintingFeature(PaintingFeatureSet featureSet) {
        StringBuilder buf = new StringBuilder();
        buf.append("绘画");
        buf.append(featureSet.makeMarkdown(false));
        buf.append("\n");
        buf.append("根据上述特征，需要结合专业的知识结构，并根据对应症状的得分进行阐述并生成评测数据。");
        return buf.toString();
    }

    public static String makeReportList(List<PaintingReport> reports) {
        StringBuilder buf = new StringBuilder();
        int index = 0;
        for (PaintingReport report : reports) {
            ++index;
            Date date = new Date(report.timestamp);
            buf.append("### ").append(index).append(" ");
            buf.append(makeReportThemeName(report));
            buf.append("（");
            buf.append(gsShortDateFormat.format(date));
            buf.append("）\n\n");

            String tool = null;
            switch (report.getTheme()) {
                case Generic:
                case HouseTreePerson:
                    tool = "房树人绘画";
                    break;
                case PersonInTheRain:
                    tool = "雨中人绘画";
                    break;
                case TreeTest:
                    tool = "树木绘画";
                    break;
                case SelfPortrait:
                    tool = "自画像";
                    break;
                default:
                    tool = "量表";
                    break;
            }
            buf.append("* 测验工具：").append(tool).append("\n");
            buf.append("* 评测日期：").append(gsDateFormat.format(date)).append("\n");
            buf.append("* 受测人：").append(report.getAttribute().getGenderText()).append("性，")
                    .append(report.getAttribute().getAgeText()).append("\n");
            if (null != report.painting) {
                buf.append("* 绘画是否有效：").append(report.painting.isValid() ? "有效" : "无效").append("\n");
            }
//            buf.append("* 绘画图片：\n");
//            buf.append("![绘画](");
//            buf.append(FileLabels.makeFileHttpsURL(report.getFileLabel(),
//                    channel.getAuthToken().getCode(), channel.getHttpsEndpoint()));
//            buf.append(")");
            buf.append("\n\n");
        }

        return buf.toString();
    }

    public static String makeReportTitle(PaintingReport report) {
        StringBuffer buf = new StringBuffer();
        buf.append(makeReportThemeName(report));
        buf.append("-").append(report.getAttribute().getGenderText());
        buf.append("-").append(report.getAttribute().getAgeText());
        buf.append("-").append(makeReportDate(report));
        return buf.toString();
    }

    public static String makeReportThemeName(PaintingReport report) {
        switch (report.getTheme()) {
            case Generic:
            case HouseTreePerson:
                return "房树人绘画测验";
            case PersonInTheRain:
                return "雨中人绘画测验";
            case TreeTest:
                return "树木绘画测验";
            case SelfPortrait:
                return "自画像绘画测验";
            default:
                return "心理测验";
        }
    }

    public static String makeReportDate(PaintingReport report) {
        return gsDateFormat.format(new Date(report.timestamp));
    }

    public static String makeReportPaintingLink(AIGCChannel channel, PaintingReport report) {
        StringBuffer buf = new StringBuffer();
        buf.append("![");
        buf.append(report.getName());
        buf.append("](");
        buf.append(FileLabels.makeFileHttpsURL(report.getFileLabel(),
                channel.getAuthToken().getCode(), channel.getHttpsEndpoint()));
        buf.append(")\n");
        return buf.toString();
    }

    public static String makeMembership(User user, Membership membership) {
        StringBuffer buf = new StringBuffer();
        if (null == membership) {
            buf.append("用户“").append(user.getName()).append("”不是白泽灵思会员，");
            List<String> benefitsList = null;
            if (user.isRegistered()) {
                buf.append("其是注册用户，享受免费版权益。\n\n");
                benefitsList = Resource.getInstance().getMemberBenefits(Consts.USER_TYPE_FREE);
            }
            else {
                buf.append("其是访客，享受访客权益。\n\n");
                benefitsList = Resource.getInstance().getMemberBenefits(Consts.USER_TYPE_VISITOR);
            }
            buf.append("其可享受的产品权益有：\n");
            for (String line : benefitsList) {
                buf.append("* ").append(line).append("\n");
            }
            buf.append("\n");
        }
        else {
            buf.append("用户“").append(user.getName()).append("”是白泽灵思");
            if (membership.type.equals(Membership.TYPE_ORDINARY)) {
                buf.append("专业版会员。\n\n");
                buf.append("其可享受的专业版会员权益有：\n");
            }
            else {
                buf.append("旗舰版会员。\n\n");
                buf.append("其可享受的旗舰版会员权益有：\n");
            }
            List<String> benefitsList = Resource.getInstance().getMemberBenefits(membership.type);
            for (String line : benefitsList) {
                buf.append("* ").append(line).append("\n");
            }
            buf.append("\n");

            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(membership.getTimestamp());
            buf.append("会员有效期从");
            buf.append(calendar.get(Calendar.YEAR)).append("年");
            buf.append(calendar.get(Calendar.MONTH) + 1).append("月");
            buf.append(calendar.get(Calendar.DATE)).append("日");
            buf.append("至");
            calendar.setTimeInMillis(membership.getTimestamp() + membership.duration);
            buf.append(calendar.get(Calendar.YEAR)).append("年");
            buf.append(calendar.get(Calendar.MONTH) + 1).append("月");
            buf.append(calendar.get(Calendar.DATE)).append("日");
            buf.append("\n\n");
        }
        return buf.toString();
    }

    public static String fastInfer(String query, Tokenizer tokenizer) {
        Dataset dataset = Resource.getInstance().loadDataset();
        if (null == dataset) {
            Logger.w(ContentTools.class, "#fastInfer - Read dataset failed");
            return null;
        }

        synchronized (dataset) {
            if (!dataset.hasAnalyzed()) {
                for (String question : dataset.getQuestions()) {
                    TFIDFAnalyzer analyzer = new TFIDFAnalyzer(tokenizer);
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

        TFIDFAnalyzer analyzer = new TFIDFAnalyzer(tokenizer);
        List<Keyword> keywordList = analyzer.analyze(query, 5);
        if (keywordList.isEmpty()) {
            Logger.w(ContentTools.class, "#fastInfer - Query keyword is none");
            return null;
        }

        List<String> keywords = new ArrayList<>();
        for (Keyword keyword : keywordList) {
            keywords.add(keyword.getWord());
        }

        return dataset.matchContent(keywords.toArray(new String[0]), 5);
    }

    private static String clipContent(String content) {
        return clipContent(content, true);
    }

    private static String clipContent(String content, boolean tipLink) {
        String value = "";
        if (null != content && content.length() > 1) {
            int end = Math.min((int)Math.ceil((double) content.length() * 0.2), 50);
            value = content.substring(0, end);
        }
        StringBuilder buf = new StringBuilder(value);
        buf.append("...");
        if (tipLink) {
            buf.append(" （");
            buf.append(Link.formatPromptDirectMarkdown("点击了解如何查看全部内容", "如何查看报告的全部内容？"));
            buf.append("）\n\n");
        }
        else {
            buf.append("\n\n");
        }
        return buf.toString();
    }

    private static String clipContentByLines(String content, int limit) {
        StringBuilder buf = new StringBuilder();
        String[] lines = content.split("\n");
        for (String line : lines) {
            if (line.length() == 0) {
                buf.append("\n");
            }
            else {
                String newLine = line.substring(0, Math.min(limit, line.length()));
                buf.append(newLine);
                if (line.length() > limit) {
                    buf.append("...");
                }
                buf.append("\n");
            }
        }
        buf.append("\n\n");
        buf.append("您暂时无法查看此评测报告的全部数据，");
        buf.append(Link.formatPromptDirectMarkdown("点击了解如何查看全部内容", "如何查看报告的全部内容？"));
        buf.append("。");
        buf.append("\n\n");
        return buf.toString();
    }

    private static String tipContent(String content) {
        return Link.formatPromptDirectMarkdown(content, "如何查看报告的全部内容？") + "\n\n";
    }
}
