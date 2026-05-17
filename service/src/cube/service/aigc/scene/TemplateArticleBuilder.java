/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.service.aigc.scene;

import cell.util.log.Logger;
import cube.aigc.ModelConfig;
import cube.aigc.Tokenizable;
import cube.aigc.psychology.*;
import cube.aigc.psychology.algorithm.IndicatorRate;
import cube.aigc.psychology.algorithm.Score;
import cube.aigc.psychology.composition.FactorSet;
import cube.aigc.psychology.composition.ReportArticle;
import cube.aigc.psychology.composition.ReportSection;
import cube.aigc.psychology.indicator.Indicator;
import cube.aigc.psychology.indicator.PIRIndicator;
import cube.common.entity.GeneratingRecord;
import cube.common.state.AIGCStateCode;
import cube.service.aigc.AIGCService;
import cube.service.aigc.guidance.Prompts;
import cube.util.MarkdownParser;

import java.util.List;

public class TemplateArticleBuilder {

    private final static String sPromptName = "psy_template_report";

    public final Theme theme;

    public final String templateName;

    public final long timestamp;

    private PaintingReport report;

    private boolean structured = false;

    private ReportArticle article;

    public TemplateArticleBuilder(Theme theme, String templateName, boolean structured) {
        this.theme = theme;
        this.templateName = templateName;
        this.timestamp = System.currentTimeMillis();
        this.structured = structured;
        this.article = new ReportArticle(makeTitle(templateName),
                theme, templateName, this.timestamp);
        this.article.state = AIGCStateCode.Processing;
    }

    public boolean isValidTemplate() {
        return (TemplateArticleConstants.Depression.equalsIgnoreCase(this.templateName) ||
                TemplateArticleConstants.Anxiety.equalsIgnoreCase(this.templateName) ||
                TemplateArticleConstants.Obsession.equalsIgnoreCase(this.templateName) ||
                TemplateArticleConstants.Stress.equalsIgnoreCase(this.templateName));
    }

    public void init(PaintingReport report) {
        this.report = report;
        this.article.sn = report.sn;
    }

    public String getFileCode() {
        return this.report.getFileCode();
    }

    public ReportArticle getArticle() {
        return this.article;
    }

    public int calcOutputTokens(Tokenizable tokenizer) {
        String base = this.report.makeMarkdown();
        String text = this.article.spliceParagraphContent();
        return tokenizer.segment(base).size() + tokenizer.segment(text).size();
    }

    public ReportArticle build(AIGCService service) {
        // 1. 按照模板名提取关键指标
        IndicatorRate rate = this.evaluate(report);
        this.article.rate = rate;

        // 2. 按照指标提取语料
        String query = this.makeQuery(rate);
        if (null == query) {
            Logger.w(this.getClass(), "#build - Make query failed: " + templateName);
            this.article.state = AIGCStateCode.Failure;
            return null;
        }

        List<String> keywords = service.segmentText(query);
        List<String> contents = Resource.getInstance().loadDataset().searchContentInOrder(
                keywords.toArray(new String[0]), keywords.size());
        if (contents.isEmpty()) {
            Logger.w(this.getClass(), "#build - No content for query: " + query);
            this.article.state = AIGCStateCode.Failure;
            return null;
        }

        // 3. 按照语料描述生成文章
        String title = this.makeParagraphTitle(rate);
        String taskFormat = "%s\n%s\n%s\n%s";
        String prefix = "您的画像是：";
        String sceneDescription = "无";
        if (TemplateArticleConstants.Depression.equalsIgnoreCase(this.templateName)) {
            taskFormat = TemplateArticleConstants.FormatDepressionTaskDesc;
            prefix = "您的内心风景画像是：";
            sceneDescription = ContentTools.makePaintingFeature(report.paintingFeatureSet);
        }
        else if (TemplateArticleConstants.Anxiety.equalsIgnoreCase(this.templateName)) {
            taskFormat = TemplateArticleConstants.FormatAnxietyTaskDesc;
            prefix = "您的内心焦虑画像是：";
            sceneDescription = ContentTools.makePaintingFeature(report.paintingFeatureSet);
        }
        else if (TemplateArticleConstants.Obsession.equalsIgnoreCase(this.templateName)) {
            taskFormat = TemplateArticleConstants.FormatObsessionTaskDesc;
            prefix = "您的内心强迫画像是：";
            sceneDescription = ContentTools.makePaintingFeature(report.paintingFeatureSet);
        }
        else if (TemplateArticleConstants.Stress.equalsIgnoreCase(this.templateName)) {
            taskFormat = TemplateArticleConstants.FormatStressTaskDesc;
            prefix = "您的内心压力画像是：";
            sceneDescription = ContentTools.makeKeyFeature(report.getEvaluationReport());
        }

        String task = String.format(taskFormat,
                report.getAttribute().getGenderText(),
                report.getAttribute().getAgeText(),
                rate.displayName,
                sceneDescription);
        String content = prefix + " **" + title + "**\n\n" + contents.get(0);
        String prompt = Prompts.getPrompt(sPromptName);
        prompt = prompt.replace("{{task}}", task);
        prompt = prompt.replace("{{content}}", content);
        prompt = prompt.replace("{{leading}}", prefix + " **" + title + "** 。");

        GeneratingRecord record = service.syncGenerateText(ModelConfig.BAIZE_NEXT_UNIT, prompt, null,
                null, null);
        if (null == record) {
            Logger.w(this.getClass(), "#build - The record is null: " + task);
            this.article.state = AIGCStateCode.Failure;
            return null;
        }

        // 4. 处理输出格式
        if (this.structured) {
            // 结构化输出
            MarkdownParser markdownParser = new MarkdownParser(record.answer);
            this.article.setContent(markdownParser.getOther());
            for (MarkdownParser.Paragraph paragraph : markdownParser.getParagraphs()) {
                this.article.addParagraph(paragraph.title, paragraph.content);
            }
        }
        else {
            // 非结构化输出
            this.article.addParagraph(title, record.answer);
        }

        this.article.state = AIGCStateCode.Ok;
        return this.article;
    }

    private IndicatorRate evaluate(PaintingReport report) {
        IndicatorRate rate = IndicatorRate.Lowest;

        if (TemplateArticleConstants.Depression.equalsIgnoreCase(this.templateName)) {
            ReportSection section = report.getReportSection(Indicator.Depression);

            if (null != section) {
                rate = fixRate(section.rate);
            }
            else {
                FactorSet factorSet = report.getEvaluationReport().getFactorSet();
                FactorSet.NormRange range = factorSet.normDepression();
                if (range.norm) {
                    rate = IndicatorRate.Low;
                }
                else if (range.value >= range.high) {
                    rate = IndicatorRate.Medium;
                }
                else if (range.value <= range.low) {
                    rate = IndicatorRate.Lowest;
                }
            }
        }
        else if (TemplateArticleConstants.Anxiety.equalsIgnoreCase(this.templateName)) {
            ReportSection section = report.getReportSection(Indicator.Anxiety);
            if (null != section) {
                rate = fixRate(section.rate);
            }
            else {
                FactorSet factorSet = report.getEvaluationReport().getFactorSet();
                FactorSet.NormRange range = factorSet.normAnxiety();
                if (range.norm) {
                    rate = IndicatorRate.Low;
                }
                else if (range.value >= range.high) {
                    rate = IndicatorRate.Medium;
                }
                else if (range.value <= range.low) {
                    rate = IndicatorRate.Lowest;
                }
            }
        }
        else if (TemplateArticleConstants.Obsession.equalsIgnoreCase(this.templateName)) {
            ReportSection section = report.getReportSection(Indicator.Obsession);
            if (null != section) {
                rate = fixRate(section.rate);
            }
            else {
                FactorSet factorSet = report.getEvaluationReport().getFactorSet();
                FactorSet.NormRange range = factorSet.normObsession();
                if (range.norm) {
                    rate = IndicatorRate.Low;
                }
                else if (range.value >= range.high) {
                    rate = IndicatorRate.Medium;
                }
                else if (range.value <= range.low) {
                    rate = IndicatorRate.Lowest;
                }
            }
        }
        else if (TemplateArticleConstants.Stress.equalsIgnoreCase(this.templateName)) {
            // 压力采用雨中人，输出关键特征
            List<KeyFeature> keyFeatureList = report.getEvaluationReport().getKeyFeatures();
            Score rainIntensity = null;
            Score rainShelterEffectiveness = null;
            Score rainShelteringMethod = null;
            Score personDetail = null;
            for (KeyFeature keyFeature : keyFeatureList) {
                Score score = keyFeature.getIndicatorScore(PIRIndicator.RainIntensity);
                if (null != score) {
                    rainIntensity = score;
                }
                score = keyFeature.getIndicatorScore(PIRIndicator.RainShelterEffectiveness);
                if (null != score) {
                    rainShelterEffectiveness = score;
                }
                score = keyFeature.getIndicatorScore(PIRIndicator.RainShelteringMethod);
                if (null != score) {
                    rainShelteringMethod = score;
                }
                score = keyFeature.getIndicatorScore(PIRIndicator.PersonDetail);
                if (null != score) {
                    personDetail = score;
                }
            }

            if (null != rainIntensity && null != rainShelterEffectiveness && null != personDetail
                    && null != rainShelteringMethod) {
                if (rainIntensity.scoring() < 0.4) {
                    // 雨势小
                    if (personDetail.scoring() < 0.4 &&
                            (rainShelterEffectiveness.scoring() < 0.4 || rainShelteringMethod.scoring() < 0.4)) {
                        // 人物细节匮乏，避雨无效或避雨方式无
                        return IndicatorRate.Low;
                    }
                    else {
                        return IndicatorRate.Lowest;
                    }
                }
                else if (rainIntensity.scoring() > 0.4 && rainIntensity.scoring() < 0.6) {
                    // 雨势一般
                    if (personDetail.scoring() < 0.4) {
                        // 人物细节匮乏
                        return IndicatorRate.Medium;
                    }
                    else {
                        return IndicatorRate.Low;
                    }
                }
                else if (rainIntensity.scoring() > 0.6) {
                    // 雨势大
                    if (personDetail.scoring() < 0.4 &&
                            (rainShelteringMethod.scoring() < 0.4 || rainShelterEffectiveness.scoring() < 0.4)) {
                        // 人物细节匮乏，避雨无效或避雨方式无
                        return IndicatorRate.High;
                    }
                    else {
                        return IndicatorRate.Medium;
                    }
                }
            }
            else {
                rate = IndicatorRate.Low;
            }
        }

        return rate;
    }

    private String makeTitle(String templateName) {
        if (TemplateArticleConstants.Depression.equalsIgnoreCase(templateName)) {
            return "内心的风景-潜意识情绪与抑郁倾向测评";
        }
        else if (TemplateArticleConstants.Anxiety.equalsIgnoreCase(templateName)) {
            return "迷雾与彼岸-潜意识焦虑情绪测评";
        }
        else if (TemplateArticleConstants.Obsession.equalsIgnoreCase(templateName)) {
            return "内心的秩序-潜意识控制欲与强迫测评";
        }
        else if (TemplateArticleConstants.Stress.equalsIgnoreCase(templateName)) {
            return "风雨中的你-潜意识压力测评";
        }
        else {
            return "潜意识测评";
        }
    }

    private String makeQuery(IndicatorRate rate) {
        String query = null;

        if (TemplateArticleConstants.Depression.equalsIgnoreCase(this.templateName)) {
            switch (rate) {
                case Lowest:
                    query = "抑郁倾向测试的盛夏繁茂之树的描述";
                    break;
                case Low:
                    query = "抑郁倾向测试的暮秋落叶之树的描述";
                    break;
                case Medium:
                    query = "抑郁倾向测试的寒冬蛰伏之树的描述";
                    break;
                case High:
                    query = "抑郁倾向测试的枯竭断裂之木的描述";
                    break;
                default:
                    break;
            }
        }
        else if (TemplateArticleConstants.Anxiety.equalsIgnoreCase(this.templateName)) {
            switch (rate) {
                case Lowest:
                    query = "焦虑测评的坚固平稳的石桥的描述";
                    break;
                case Low:
                    query = "焦虑测评的迷雾萦绕的晃动吊桥的描述";
                    break;
                case Medium:
                    query = "焦虑测评的狂风巨浪中的独木桥的描述";
                    break;
                case High:
                    query = "焦虑测评的濒临断裂的悬空残桥的描述";
                    break;
                default:
                    break;
            }
        }
        else if (TemplateArticleConstants.Obsession.equalsIgnoreCase(this.templateName)) {
            switch (rate) {
                case Lowest:
                    query = "强迫测评的旷野漫步者的描述";
                    break;
                case Low:
                    query = "强迫测评的精工雕刻师的描述";
                    break;
                case Medium:
                    query = "强迫测评的秩序守夜人的描述";
                    break;
                case High:
                    query = "强迫测评的无尽迷宫旅人的描述";
                    break;
                default:
                    break;
            }
        }
        else if (TemplateArticleConstants.Stress.equalsIgnoreCase(this.templateName)) {
            switch (rate) {
                case Lowest:
                    query = "压力测评的和风细雨中的漫步者的描述";
                    break;
                case Low:
                    query = "压力测评的风雨中拉扯的赶路人的描述";
                    break;
                case Medium:
                    query = "压力测评的逆雨前行的孤勇者的描述";
                    break;
                case High:
                    query = "压力测评的暴雨中搁浅的旅人的描述";
                    break;
                default:
                    break;
            }
        }

        return query;
    }

    private String makeParagraphTitle(IndicatorRate rate) {
        String title = null;

        if (TemplateArticleConstants.Depression.equalsIgnoreCase(this.templateName)) {
            switch (rate) {
                case Lowest:
                    title = "盛夏繁茂之树";
                    break;
                case Low:
                    title = "暮秋落叶之树";
                    break;
                case Medium:
                    title = "寒冬蛰伏之树";
                    break;
                case High:
                    title = "枯竭断裂之木";
                    break;
                default:
                    break;
            }
        }
        else if (TemplateArticleConstants.Anxiety.equalsIgnoreCase(this.templateName)) {
            switch (rate) {
                case Lowest:
                    title = "坚固平稳的石桥";
                    break;
                case Low:
                    title = "迷雾萦绕的晃动吊桥";
                    break;
                case Medium:
                    title = "狂风巨浪中的独木桥";
                    break;
                case High:
                    title = "濒临断裂的悬空残桥";
                    break;
                default:
                    break;
            }
        }
        else if (TemplateArticleConstants.Obsession.equalsIgnoreCase(this.templateName)) {
            switch (rate) {
                case Lowest:
                    title = "旷野漫步者";
                    break;
                case Low:
                    title = "精工雕刻师";
                    break;
                case Medium:
                    title = "秩序守夜人";
                    break;
                case High:
                    title = "无尽迷宫旅人";
                    break;
                default:
                    break;
            }
        }
        else if (TemplateArticleConstants.Stress.equalsIgnoreCase(this.templateName)) {
            switch (rate) {
                case Lowest:
                    title = "风细雨中的漫步者";
                    break;
                case Low:
                    title = "风雨中拉扯的赶路人";
                    break;
                case Medium:
                    title = "逆雨前行的孤勇者";
                    break;
                case High:
                    title = "暴雨中搁浅的旅人";
                    break;
                default:
                    break;
            }
        }

        return title;
    }

    private IndicatorRate fixRate(IndicatorRate input) {
        switch (input) {
            case None:
                // None 矫正为 Lowest
                return IndicatorRate.Lowest;
            case Highest:
                // Highest 矫正为 High
                return IndicatorRate.High;
            default:
                return input;
        }
    }
}
