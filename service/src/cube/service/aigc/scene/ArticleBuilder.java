/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.service.aigc.scene;

import cell.util.log.Logger;
import cube.aigc.ModelConfig;
import cube.aigc.psychology.PaintingReport;
import cube.aigc.psychology.Resource;
import cube.aigc.psychology.Theme;
import cube.aigc.psychology.algorithm.IndicatorRate;
import cube.aigc.psychology.composition.FactorSet;
import cube.aigc.psychology.composition.ReportArticle;
import cube.aigc.psychology.composition.ReportSection;
import cube.aigc.psychology.indicator.Indicator;
import cube.common.entity.GeneratingRecord;
import cube.common.state.AIGCStateCode;
import cube.service.aigc.AIGCService;
import cube.service.aigc.guidance.Prompts;
import cube.util.MarkdownParser;

import java.util.List;

public class ArticleBuilder {

    /**
     * 抑郁。
     */
    public final static String Depression = "depression";

    private final static String sFormatDepressionTaskDesc = "该测评是使用绘画投射方式进行的抑郁倾向测评，受测人是%s性，%s。" +
            "抑郁倾向级别是 **%s** （测评级别从低到高依次分为：很低、低、中等、高）。\n\n画面内容如下：\n%s\n";

    /**
     * 焦虑。
     */
    public final static String Anxiety = "anxiety";

    private final static String sFormatAnxietyTaskDesc = "该测评是使用绘画投射方式进行的焦虑情绪测评，受测人是%s性，%s。" +
            "焦虑情绪级别是 **%s** （测评级别从低到高依次分为：很低、低、中等、高）。\n\n画面内容如下：\n%s\n";

    private final static String sPromptName = "psy_template_report";

    public final Theme theme;

    public final String templateName;

    public final long timestamp;

    private PaintingReport report;

    private boolean structured = false;

    private ReportArticle article;

    public ArticleBuilder(Theme theme, String templateName, boolean structured) {
        this.theme = theme;
        this.templateName = templateName;
        this.timestamp = System.currentTimeMillis();
        this.structured = structured;
        this.article = new ReportArticle(makeTitle(templateName),
                theme, templateName, this.timestamp);
        this.article.state = AIGCStateCode.Processing;
    }

    public boolean isValidTemplate() {
        return (Depression.equalsIgnoreCase(this.templateName) ||
                Anxiety.equalsIgnoreCase(this.templateName));
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
        if (Depression.equalsIgnoreCase(this.templateName)) {
            taskFormat = sFormatDepressionTaskDesc;
            prefix = "您的内心风景画像是：";
        }
        else if (Anxiety.equalsIgnoreCase(this.templateName)) {
            taskFormat = sFormatAnxietyTaskDesc;
            prefix = "您的内心焦虑画像是：";
        }

        String task = String.format(taskFormat, report.getAttribute().getGenderText(),
                report.getAttribute().getAgeText(), rate.displayName,
                ContentTools.makePaintingFeature(report.paintingFeatureSet));
        String content = prefix + " **" + title + "**\n\n" + contents.get(0);
        String prompt = Prompts.getPrompt(sPromptName);
        prompt = prompt.replace("{{task}}", task);
        prompt = prompt.replace("{{content}}", content);
        prompt = prompt.replace("{{leading}}", prefix + " **" + title + "** 。\n\n");

        GeneratingRecord record = service.syncGenerateText(ModelConfig.BAIZE_NEXT_UNIT, prompt, null,
                null, null);
        if (null == record) {
            Logger.w(this.getClass(), "#build - The record is null: " + task);
            this.article.state = AIGCStateCode.Failure;
            return null;
        }

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

        if (Depression.equalsIgnoreCase(this.templateName)) {
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
        else if (Anxiety.equalsIgnoreCase(this.templateName)) {
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

        return rate;
    }

    private String makeTitle(String templateName) {
        if (Depression.equalsIgnoreCase(templateName)) {
            return "内心的风景-潜意识情绪与抑郁倾向测评";
        }
        else if (Anxiety.equalsIgnoreCase(templateName)) {
            return "迷雾与彼岸-潜意识焦虑测评";
        }
        else {
            return "潜意识测评";
        }
    }

    private String makeQuery(IndicatorRate rate) {
        String query = null;

        if (Depression.equalsIgnoreCase(this.templateName)) {
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
        else if (Anxiety.equalsIgnoreCase(this.templateName)) {
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

        return query;
    }

    private String makeParagraphTitle(IndicatorRate rate) {
        String title = null;

        if (Depression.equalsIgnoreCase(this.templateName)) {
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
        else if (Anxiety.equalsIgnoreCase(this.templateName)) {
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
