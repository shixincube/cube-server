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
import cube.common.entity.AIGCChannel;
import cube.common.entity.AIGCGenerationRecord;
import cube.service.aigc.AIGCService;
import cube.util.TextUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 工作流。
 */
public class Workflow {

    public final static String HighTrick = "明显";
    public final static String NormalTrick = "";
    public final static String LowTrick = "不足";

    private EvaluationReport evaluationReport;

    private AIGCChannel channel;

    private AIGCService service;

    private List<ReportParagraph> paragraphList;

    public Workflow(EvaluationReport evaluationReport, AIGCChannel channel, AIGCService service) {
        this.evaluationReport = evaluationReport;
        this.channel = channel;
        this.service = service;
        this.paragraphList = new ArrayList<>();
    }

    public PsychologyReport fillReport(PsychologyReport report) {
        report.setReportParagraph(this.paragraphList);
        return report;
    }

    public Workflow makeStress() {
        // 获取模板
        ThemeTemplate template = Resource.getInstance().getThemeTemplate(Theme.Stress.code);

        // 生成上下文
        List<AIGCGenerationRecord> records = new ArrayList<>();
        //this.makeRepresentationContext(this.evaluationReport.getRepresentationList());
        if (Logger.isDebugLevel()) {
            Logger.d(this.getClass(), "#makeStress - Context length: " + records.size());
        }

        for (String title : template.getTitles()) {
            this.paragraphList.add(new ReportParagraph(title));
        }

        // 生成表征内容
        String representationString = this.spliceRepresentationContent();

        // 格式化所有段落提示词
        List<String> featurePromptList = template.formatFeaturePrompt(representationString);
        List<String> suggestionPromptList = template.formatSuggestionPrompt(representationString);
        for (int i = 0; i < this.paragraphList.size(); ++i) {
            ReportParagraph paragraph = this.paragraphList.get(i);

            // 生成描述
            String prompt = featurePromptList.get(i);
            String result = this.service.syncGenerateText(ModelConfig.BAIZE_UNIT, prompt, records);
            if (null == result) {
                Logger.w(this.getClass(), "#makeStress - Infer failed");
            }
            else {
                List<String> list = this.extractList(result);
                if (list.isEmpty()) {
                    Logger.w(this.getClass(), "#makeStress - extract list error");
                    break;
                }
                paragraph.addFeatures(list);
            }

            // 将特征结果进行拼合
            prompt = template.formatDescriptionPrompt(i, this.spliceList(paragraph.getFeatures()));
            result = this.service.syncGenerateText(ModelConfig.BAIZE_UNIT, prompt, null);
            // 提取列表
            List<String> contentList = this.extractList(result);
            // 转平滑文本，过滤噪音
            String description = this.filterNoise(this.plainText(contentList));
            paragraph.setDescription(description);

            // 生成建议
            prompt = suggestionPromptList.get(i);
            result = this.service.syncGenerateText(ModelConfig.BAIZE_UNIT, prompt, records);
            if (null == result) {
                Logger.w(this.getClass(), "#makeStress - Infer failed");
            }
            else {
                paragraph.addSuggestions(this.extractList(result));
            }
        }

        return this;
    }

    public ThemeTemplate makeFamilyRelationships() {
        return null;
    }

    public ThemeTemplate makeIntimacy() {
        return null;
    }

    public ThemeTemplate makeCognition() {
        return null;
    }

    private String spliceRepresentation() {
        StringBuilder buf = new StringBuilder();
        for (EvaluationReport.Representation representation : this.evaluationReport.getRepresentationList()) {
            if (representation.positive > 1) {
                buf.append(representation.interpretation.getComment().word).append(HighTrick);
            }
            else if (representation.positive > 0) {
                buf.append(representation.interpretation.getComment().word).append(NormalTrick);
            }
            else {
                buf.append(representation.interpretation.getComment().word).append(LowTrick);
            }
            buf.append("，");
        }
        buf.delete(buf.length() - 1, buf.length());
        return buf.toString();
    }

    private String spliceRepresentationContent() {
        StringBuilder buf = new StringBuilder();
        for (EvaluationReport.Representation representation : this.evaluationReport.getRepresentationList()) {
            buf.append(representation.interpretation.getInterpretation()).append("\n");
            if (buf.length() > ModelConfig.BAIZE_UNIT_CONTEXT_LIMIT - 50) {
                Logger.w(this.getClass(), "#spliceRepresentationContent - Context length is overflow: " + buf.length());
                break;
            }
        }
        buf.delete(buf.length() - 1, buf.length());
        return buf.toString();
    }

    private List<AIGCGenerationRecord> makeRepresentationContext(List<EvaluationReport.Representation> list) {
        List<AIGCGenerationRecord> result = new ArrayList<>();
        for (EvaluationReport.Representation representation : list) {
            AIGCGenerationRecord record = new AIGCGenerationRecord(ModelConfig.BAIZE_UNIT,
                    representation.interpretation.getComment().word,
                    representation.interpretation.getInterpretation());
            result.add(record);
        }
        return result;
    }

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

            lines.add(text);

            if (TextUtils.startsWithNumberSign(text.trim())) {
                result.add(text.trim());
            }
        }

        if (result.isEmpty()) {
            Logger.d(this.getClass(), "#extractList - The result is not list, extracts list by symbol");

            String targetContent = content;
            if (lines.size() >= 3) {
                targetContent = lines.get(1);
            }

            buf = targetContent.split("。");
            for (int i = 0; i < buf.length; ++i) {
                String text = buf[i].trim();
                text = text.replaceAll("\n", "");
                result.add((i + 1) + ". " + text + "。");
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

    private String plainText(List<String> list) {
        StringBuilder buf = new StringBuilder();
        for (String text : list) {
            String[] tmp = text.split("：");
            if (tmp.length == 1) {
                tmp = text.split(":");
            }
            String content = tmp[tmp.length - 1];
            if (TextUtils.startsWithNumberSign(content)) {
                int index = content.indexOf(".");
                content = content.substring(index + 1).trim();
            }
            buf.append(content);
        }
        return buf.toString().replaceAll("我", "你");
    }

    private String filterNoise(String text) {
        List<String> content = new ArrayList<>();
        String[] sentences = text.split("。");
        for (String sentence : sentences) {
            if (sentence.contains("人工智能") || sentence.contains("AI")) {
                continue;
            }

            if (sentence.startsWith("但")) {
                content.add(sentence.replaceFirst("但", ""));
            }
            else {
                content.add(sentence);
            }
        }

        StringBuilder buf = new StringBuilder();
        for (String c : content) {
            buf.append(c).append("。");
        }
        return buf.toString();
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
//        for (PromptGroup group : this.phase1Groups) {
//            buf.append("[R] user: ").append(group.record.query).append('\n');
//            buf.append("[R] assistant: ").append(group.record.answer).append('\n');
//            buf.append("[Prompt]\n");
//            buf.append(builder.serializePromptChaining(group.chaining));
//            buf.append("--------------------------------------------------------------------------------\n");
//        }
        return buf.toString();
    }
}
