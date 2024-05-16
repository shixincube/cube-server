/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 Ambrose Xu.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package cube.service.aigc.scene;

import cell.util.log.Logger;
import cube.aigc.ModelConfig;
import cube.aigc.psychology.*;
import cube.aigc.psychology.algorithm.Representation;
import cube.aigc.psychology.composition.BehaviorSuggestion;
import cube.aigc.psychology.composition.EvaluationScore;
import cube.aigc.psychology.composition.ReportSuggestion;
import cube.common.entity.AIGCChannel;
import cube.common.entity.GenerativeOption;
import cube.service.aigc.AIGCService;
import cube.util.TextUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 工作流。
 */
public class Workflow {

    public final static String HighTrick = "明显";
    public final static String NormalTrick = "具有";
    public final static String LowTrick = "缺乏";//"不足";

    private EvaluationReport evaluationReport;

    private MBTIEvaluation mbtiEvaluation;

    private AIGCChannel channel;

    private AIGCService service;

    private List<BehaviorSuggestion> behaviorTextList;

    private List<ReportSuggestion> reportTextList;

    private List<ReportParagraph> paragraphList;

    private String unitName = ModelConfig.BAIZE_UNIT;

    private int maxContext = ModelConfig.BAIZE_CONTEXT_LIMIT - 60;

    public Workflow(EvaluationReport evaluationReport, AIGCChannel channel, AIGCService service) {
        this.evaluationReport = evaluationReport;
        this.channel = channel;
        this.service = service;
        this.behaviorTextList = new ArrayList<>();
        this.reportTextList = new ArrayList<>();
        this.paragraphList = new ArrayList<>();
    }

    public void setUnitName(String unitName, int maxContext) {
        this.unitName = unitName;
        this.maxContext = maxContext - 60;
    }

    public PsychologyReport fillReport(PsychologyReport report) {
        report.setEvaluationReport(this.evaluationReport);

        if (null != this.mbtiEvaluation) {
            report.setMBTIFeature(this.mbtiEvaluation.getResult());
        }

        report.setBehaviorList(this.behaviorTextList);
        report.setReportTextList(this.reportTextList);
        report.setParagraphs(this.paragraphList);
        return report;
    }

    public Workflow make(Theme theme, int maxBehaviorTexts, int maxIndicatorTexts) {
        // 获取模板
        ThemeTemplate template = Resource.getInstance().getThemeTemplate(theme.code);

        // MBTI 评估
        this.mbtiEvaluation = new MBTIEvaluation(this.evaluationReport.getRepresentationList(),
            this.evaluationReport.getEvaluationScores());

        int age = this.evaluationReport.getAttribute().age;
        String gender = this.evaluationReport.getAttribute().gender;
        // 逐一推理每一条表征
        this.behaviorTextList = this.inferBehavior(template, age, gender, maxBehaviorTexts);
        if (this.behaviorTextList.isEmpty()) {
            Logger.w(this.getClass(), "#make - Behavior text error");
            return this;
        }

        // 评估分推理
        List<EvaluationScore> scoreList = this.evaluationReport.getEvaluationScoresByRepresentation(maxIndicatorTexts);
        this.reportTextList = this.inferScore(scoreList);
        if (this.reportTextList.isEmpty()) {
            Logger.w(this.getClass(), "#make - Report text error");
            return this;
        }

        if (Logger.isDebugLevel()) {
            Logger.d(this.getClass(), "#make - List size: " +
                    this.behaviorTextList.size() + "/" + this.reportTextList.size());
        }

        /* FIXME XJW 以下步骤不再推荐使用
        for (String title : template.getTitles()) {
            this.paragraphList.add(new ReportParagraph(title));
        }

        // 生成表征内容
        boolean paragraphInferrable = false;
        if (paragraphInferrable) {
    //        String representation = this.spliceRepresentationInterpretation();
            List<String> representations = this.spliceBehaviorList(this.behaviorList);
            if (Logger.isDebugLevel()) {
                Logger.d(this.getClass(), "#make - representation num: " + representations.size());
            }

            // 逐一生成提示词并推理
            for (int i = 0; i < this.paragraphList.size(); ++i) {
                ReportParagraph paragraph = this.paragraphList.get(i);

                // 生成标题的上下文内容
                List<GenerativeRecord> records = new ArrayList<>();
                records.add(new GenerativeRecord(this.unitName, paragraph.title,
                        template.getExplain(i)));
                if (Logger.isDebugLevel()) {
                    Logger.d(this.getClass(), "#make - \"" + paragraph.title + "\" context num: " + records.size());
                }

                // 推理特征
                StringBuilder result = new StringBuilder();
                for (String representation : representations) {
                    String prompt = template.formatFeaturePrompt(i, representation);
                    String answer = this.service.syncGenerateText(this.unitName, prompt, new GenerativeOption(),
                            records, null);
                    if (null == answer) {
                        Logger.w(this.getClass(), "#make - Infer feature failed");
                        break;
                    }
                    // 记录结果
                    result.append(answer).append("\n");
                }

                List<String> list = this.extractList(result.toString());
                if (list.isEmpty()) {
                    Logger.w(this.getClass(), "#make - extract feature list error");
                    break;
                }
                // 添加特性
                paragraph.addFeatures(this.plainText(list, false));

                // 推理描述
                result = new StringBuilder();
                List<List<String>> featuresList = TextUtils.splitList(paragraph.getFeatures(), this.maxContext);
                for (List<String> features : featuresList) {
                    // 将特征结果进行拼合
                    String prompt = template.formatDescriptionPrompt(i, this.spliceList(features));
                    String answer = this.service.syncGenerateText(this.unitName, prompt, new GenerativeOption(),
                            records, null);
                    if (null == answer) {
                        Logger.w(this.getClass(), "#make - Infer description failed");
                        break;
                    }
                    // 记录结果
                    result.append(answer).append("\n");
                }

                // 转平滑文本，过滤噪音
                String description = this.filterNoise(result.toString());
                paragraph.setDescription(description);

                // 推理建议
                result = new StringBuilder();
                for (String representation : representations) {
                    String prompt = template.formatSuggestionPrompt(i, representation);
                    String answer = this.service.syncGenerateText(this.unitName, prompt, new GenerativeOption(),
                            records, null);
                    if (null == answer) {
                        Logger.w(this.getClass(), "#make - Infer suggestion failed");
                        break;
                    }
                    // 记录结果
                    result.append(answer).append("\n");
                }
                // 添加建议
                paragraph.addSuggestions(this.extractList(result.toString()));

                // 推理意见
                result = new StringBuilder();
                List<List<String>> suggestionsList = TextUtils.splitList(paragraph.getSuggestions(), this.maxContext);
                for (List<String> suggestions : suggestionsList) {
                    String prompt = template.formatOpinionPrompt(i, this.spliceList(suggestions));
                    String answer = this.service.syncGenerateText(this.unitName, prompt, new GenerativeOption(),
                            records, null);
                    if (null == answer) {
                        Logger.w(this.getClass(), "#make - Infer opinion failed");
                        break;
                    }
                    // 记录结果
                    result.append(answer).append("\n");
                }

                // 转平滑文本，过滤噪音
                String opinion = this.filterNoise(result.toString());
                paragraph.setOpinion(opinion);
            }
        }*/

        return this;
    }

    private List<BehaviorSuggestion> inferBehavior(ThemeTemplate template, int age, String gender, int maxRepresentation) {
        List<BehaviorSuggestion> result = new ArrayList<>();

        List<Representation> representations = this.evaluationReport.getRepresentationListByEvaluationScore(100);
        for (Representation representation : representations) {
            String marked = null;
            // 趋势
            if (representation.positiveCorrelation == representation.negativeCorrelation) {
                marked = NormalTrick + representation.knowledgeStrategy.getTerm().word;
            }
            else if (representation.negativeCorrelation > 0 &&
                    representation.positiveCorrelation < representation.negativeCorrelation) {
                marked = LowTrick + representation.knowledgeStrategy.getTerm().word;
            }
            else if (representation.positiveCorrelation >= 2 ||
                    (representation.positiveCorrelation - representation.negativeCorrelation) >= 2) {
                marked = HighTrick + representation.knowledgeStrategy.getTerm().word;
            }
            else {
                marked = NormalTrick + representation.knowledgeStrategy.getTerm().word;
            }

            // 设置短描述
            representation.description = marked;
        }

        int count = 0;
        for (Representation representation : representations) {
//            String interpretation = representation.knowledgeStrategy.getInterpretation();
//            KnowledgeStrategy.Scene scene = representation.knowledgeStrategy.getScene(template.theme);
//            StringBuilder content = new StringBuilder();
//            if (null == scene) {
//                content.append(interpretation);
//            }
//            else {
//                content.append(scene.explain);
//            }

            Logger.d(this.getClass(), "#inferBehavior - ");

            // 推理表征
            String prompt = template.formatBehaviorPrompt(representation.knowledgeStrategy.getTerm().word,
                    age, gender, representation.description);
            String behavior = this.service.syncGenerateText(this.unitName, prompt, new GenerativeOption(),
                    null, null);

            prompt = template.formatSuggestionPrompt(representation.knowledgeStrategy.getTerm().word);
            String suggestion = this.service.syncGenerateText(this.unitName, prompt, new GenerativeOption(),
                    null, null);

            if (null != behavior && null != suggestion) {
                result.add(new BehaviorSuggestion(representation.knowledgeStrategy.getTerm(),
                        representation.description, behavior, suggestion));
            }

            ++count;
            if (count >= maxRepresentation) {
                break;
            }
        }

        return result;
    }

    private List<ReportSuggestion> inferScore(List<EvaluationScore> scoreList) {
        List<ReportSuggestion> result = new ArrayList<>();
        for (EvaluationScore es : scoreList) {
            String prompt = es.generateReportPrompt();
            if (null == prompt) {
                continue;
            }
            String report = this.service.syncGenerateText(this.unitName, prompt, new GenerativeOption(),
                    null, null);

            prompt = es.generateSuggestionPrompt();
            if (null == prompt) {
                continue;
            }
            String suggestion = this.service.syncGenerateText(this.unitName, prompt, new GenerativeOption(),
                    null, null);

            if (null != report && null != suggestion) {
                result.add(new ReportSuggestion(es.indicator, es.positiveScore > es.negativeScore ? 1 : -1,
                        report, suggestion));
            }
        }
        return result;
    }

    /*
    private List<String> spliceBehaviorList(List<String> behaviorList) {
        List<String> result = new ArrayList<>();
        StringBuilder buf = new StringBuilder();

        for (String text : behaviorList) {
            List<String> list = this.extractList(text);
            for (String content : list) {
                if (TextUtils.startsWithNumberSign(content)) {
                    int index = content.indexOf(".");
                    content = content.substring(index + 1).trim();

                    if (buf.length() + content.length() > this.maxContext) {
                        result.add(buf.toString());
                        buf = new StringBuilder();
                    }

                    buf.append(content).append("\n");
                }
            }
        }

        result.add(buf.toString());
        return result;
    }

    private String spliceRepresentationInterpretation() {
        StringBuilder buf = new StringBuilder();
        for (Representation representation : this.evaluationReport.getRepresentationListByEvaluationScore(100)) {
            buf.append(representation.knowledgeStrategy.getInterpretation()).append("\n");
            if (buf.length() > this.maxContext - 100) {
                Logger.w(this.getClass(), "#spliceRepresentationInterpretation - Context length is overflow: " + buf.length());
                break;
            }
        }
        buf.delete(buf.length() - 1, buf.length());
        return buf.toString();
    }

    private List<GenerativeRecord> makeRepresentationContext(List<Representation> list) {
        List<GenerativeRecord> result = new ArrayList<>();
        for (Representation representation : list) {
            GenerativeRecord record = new GenerativeRecord(this.unitName,
                    representation.knowledgeStrategy.getComment().word,
                    representation.knowledgeStrategy.getInterpretation());
            result.add(record);
        }
        return result;
    }*/

    /**
     * 将内容里的列表数据提取到列表里。
     *
     * @param content
     * @return
     */
    private List<String> extractList(String content) {
        List<String> result = new ArrayList<>();
        String[] buf = content.split("\n");
        List<String> lines = new ArrayList<>();
        for (String text : buf) {
            if (text.trim().length() <= 1) {
                continue;
            }

            lines.add(text.trim());

            if (TextUtils.startsWithNumberSign(text.trim())) {
                result.add(text.trim());
            }
        }

        if (result.isEmpty()) {
            Logger.d(this.getClass(), "#extractList - The result is not list, extracts list by symbol");

            int index = 0;
            for (String line : lines) {
                if (line.endsWith("：") || line.endsWith(":")) {
                    continue;
                }

                buf = line.split("。");
                for (String text : buf) {
                    text = text.trim().replaceAll("\n", "");
                    result.add((index + 1) + ". " + text + "。");
                    ++index;
                }
            }
        }

        return result;
    }

    /**
     * 将列表合并成文本内容。
     *
     * @param list
     * @return
     */
    private String spliceList(List<String> list) {
        StringBuilder buf = new StringBuilder();
        for (String text : list) {
            buf.append(text).append("\n");
        }
        buf.delete(buf.length() - 1, buf.length());
        return buf.toString();
    }

    /**
     *
     * @param list
     * @param sequenceFilter 是否过滤段落序号。
     * @return
     */
    private List<String> plainText(List<String> list, boolean sequenceFilter) {
        List<String> result = new ArrayList<>(list.size());
        for (String text : list) {
            String content = text;

            if (sequenceFilter) {
                String[] tmp = text.split("：");
                if (tmp.length == 1) {
                    tmp = text.split(":");
                }
                content = tmp[tmp.length - 1];
                if (TextUtils.startsWithNumberSign(content)) {
                    int index = content.indexOf(".");
                    content = content.substring(index + 1).trim();
                }
            }

            content = content
                    .replaceAll("他们", "你")
                    .replaceAll("他", "你")
                    .replaceAll("这个人", "你")
                    .replaceAll("该人", "你")
                    .replaceAll("该个人", "你")
                    .replaceAll("本人", "你")
                    .replace("人们", "");
            result.add(content);
        }
        return result;
    }

    private String filterNoise(String text) {
        List<String> content = new ArrayList<>();

        // 按行读取
        List<String> lines = new ArrayList<>();
        String[] tmp = text.split("\n");
        for (String t : tmp) {
            if (t.trim().length() <= 2) {
                continue;
            }
            lines.add(t.trim());
        }
        // 将 Markdown 格式文本转平滑文本
        lines = this.plainText(lines, true);

        for (String line : lines) {
            if (line.contains("根据")) {
                continue;
            }

            String[] sentences = line.split("。");
            for (String sentence : sentences) {
                if (sentence.contains("人工智能") || sentence.contains("AI")) {
                    continue;
                }
                else if (sentence.contains("总的来说") || sentence.contains("总之")) {
                    continue;
                }

                if (sentence.startsWith("但")) {
                    content.add(sentence.replaceFirst("但", ""));
                }
                else {
                    // 进行细分
                    String[] sub = sentence.split("，");
                    StringBuilder ss = new StringBuilder();
                    for (String s : sub) {
                        String rs = s;
                        if (s.contains("然而") || s.contains("但是")) {
                            continue;
                        }
                        else if (s.contains("提供的信息")) {
                            rs = s.replace("提供的信息", "绘画");
                        }

                        ss.append(rs).append("，");
                    }
                    if (ss.length() > 1) {
                        ss.delete(ss.length() - 1, ss.length());
                    }

                    content.add(ss.toString());
                }
            }
        }

        StringBuilder buf = new StringBuilder();
        for (String c : content) {
            buf.append(c).append("。");
        }
        return buf.toString();
    }
}
