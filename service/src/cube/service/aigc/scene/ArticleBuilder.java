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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private final static String sFormatAnxietyTaskDesc = "";

    private final static String sPromptName = "psy_template_report";

    public final Theme theme;

    public final String templateName;

    public final long timestamp;

    private PaintingReport report;

    private Map<Indicator, IndicatorRate> indicatorRateMap;

    private ReportArticle article;

    public ArticleBuilder(Theme theme, String templateName) {
        this.theme = theme;
        this.templateName = templateName;
        this.timestamp = System.currentTimeMillis();
        this.indicatorRateMap = new HashMap<>();
        this.article = new ReportArticle("内心的风景-潜意识情绪与抑郁倾向测评",
                theme, templateName, this.timestamp);
        this.article.state = AIGCStateCode.Processing;
    }

    public boolean isValidTemplate() {
        return (Depression.equalsIgnoreCase(this.templateName) ||
                Anxiety.equalsIgnoreCase(this.templateName));
    }

    public ReportArticle getArticle() {
        return this.article;
    }

    public ReportArticle build(AIGCService service, PaintingReport report) {
        this.report = report;

        // 1. 按照模板名提取关键指标
        IndicatorRate rate = this.evaluate(report);

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
        String title = this.makeTitle(rate);
        String task = String.format(sFormatDepressionTaskDesc, report.getAttribute().getGenderText(),
                report.getAttribute().getAgeText(), rate.displayName,
                ContentTools.makePaintingFeature(report.paintingFeatureSet));
        String content = "您的内心风景画像是： **" + title + "**\n\n" + contents.get(0);
        String prompt = Prompts.getPrompt(sPromptName);
        prompt = prompt.replace("{{task}}", task);
        prompt = prompt.replace("{{content}}", content);

        GeneratingRecord record = service.syncGenerateText(ModelConfig.BAIZE_NEXT_UNIT, prompt, null,
                null, null);
        if (null == record) {
            Logger.w(this.getClass(), "#build - The record is null: " + task);
            this.article.state = AIGCStateCode.Failure;
            return null;
        }

        this.article.addParagraph(title, title, record.answer);
        this.article.state = AIGCStateCode.Ok;
        return this.article;
    }

    private IndicatorRate evaluate(PaintingReport report) {
        if (Depression.equalsIgnoreCase(this.templateName)) {
            ReportSection section = report.getReportSection(Indicator.Depression);
            IndicatorRate rate = IndicatorRate.Lowest;
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
            return rate;
        }
        else if (Anxiety.equalsIgnoreCase(this.templateName)) {

        }

        return null;
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

        }

        return query;
    }

    private String makeTitle(IndicatorRate rate) {
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
