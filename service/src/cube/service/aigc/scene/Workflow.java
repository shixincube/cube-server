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

    public final static String HighTrick = "过度";//"明显";
    public final static String NormalTrick = "一般";
    public final static String LowTrick = "缺乏";//"不足";

    private EvaluationReport evaluationReport;

    private AIGCChannel channel;

    private AIGCService service;

    private List<ReportParagraph> paragraphList;

    private int maxRepresentationNum = 9;

    public Workflow(EvaluationReport evaluationReport, AIGCChannel channel, AIGCService service) {
        this.evaluationReport = evaluationReport;
        this.channel = channel;
        this.service = service;
        this.paragraphList = new ArrayList<>();
    }

    public PsychologyReport fillReport(PsychologyReport report) {
        report.setParagraphs(this.paragraphList);
        return report;
    }

    public Workflow makeStress() {
        // 获取模板
        ThemeTemplate template = Resource.getInstance().getThemeTemplate(Theme.Stress.code);

        this.evaluationReport.setTopN(this.maxRepresentationNum);

        int age = this.evaluationReport.getAttribute().age;
        String gender = this.evaluationReport.getAttribute().gender;
        // 逐一推理每一条表征
        List<String> behaviorList = this.inferBehavior(template, age, gender);
        if (null == behaviorList || behaviorList.isEmpty()) {
            Logger.w(this.getClass(), "#makeStress - Behavior error");
            return this;
        }

        for (String title : template.getTitles()) {
            this.paragraphList.add(new ReportParagraph(title));
        }

        // 生成表征内容
        String representation = this.spliceRepresentationInterpretation();

        // 逐一生成提示词并推理
        for (int i = 0; i < this.paragraphList.size(); ++i) {
            ReportParagraph paragraph = this.paragraphList.get(i);

            // 生成标题的上下文内容
            List<AIGCGenerationRecord> records = new ArrayList<>();
            records.add(new AIGCGenerationRecord(ModelConfig.BAIZE_UNIT, paragraph.title,
                    template.getExplain(i)));
            if (Logger.isDebugLevel()) {
                Logger.d(this.getClass(), "#makeStress - \"" + paragraph.title + "\" context length: " + records.size());
            }

            // 推理特征
            String prompt = template.formatFeaturePrompt(i, representation);
            String result = this.service.syncGenerateText(ModelConfig.BAIZE_UNIT, prompt, records);
            if (null == result) {
                Logger.w(this.getClass(), "#makeStress - Infer feature failed");
                break;
            }

            List<String> list = this.extractList(result);
            if (list.isEmpty()) {
                Logger.w(this.getClass(), "#makeStress - extract feature list error");
                break;
            }
            paragraph.addFeatures(list);

            // 将特征结果进行拼合
            prompt = template.formatDescriptionPrompt(i, this.spliceList(paragraph.getFeatures()));
            result = this.service.syncGenerateText(ModelConfig.BAIZE_UNIT, prompt, records);
            if (null == result) {
                Logger.w(this.getClass(), "#makeStress - Infer description failed");
                break;
            }
            // 转平滑文本，过滤噪音
            String description = this.filterNoise(result);
            paragraph.setDescription(description);

            // 推理建议
            prompt = template.formatSuggestionPrompt(i, representation);
            result = this.service.syncGenerateText(ModelConfig.BAIZE_UNIT, prompt, records);
            if (null == result) {
                Logger.w(this.getClass(), "#makeStress - Infer suggestion failed");
                break;
            }
            paragraph.addSuggestions(this.extractList(result));

            // 将建议结果进行拼合
            prompt = template.formatOpinionPrompt(i, this.spliceList(paragraph.getSuggestions()));
            result = this.service.syncGenerateText(ModelConfig.BAIZE_UNIT, prompt, records);
            // 转平滑文本，过滤噪音
            String opinion = this.filterNoise(result);
            paragraph.setOpinion(opinion);
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

    private List<String> inferBehavior(ThemeTemplate template, int age, String gender) {
        List<String> result = new ArrayList<>();

        for (EvaluationReport.Representation representation : this.evaluationReport.getRepresentationListOrderByScore()) {
            String marked = null;
            // 计分
            if (representation.positive > 1) {
                marked = HighTrick + "的" + representation.knowledgeStrategy.getComment().word;
            }
            else if (representation.positive > 0) {
                marked = NormalTrick + "的" + representation.knowledgeStrategy.getComment().word;
            }
            else {
                marked = LowTrick + "的" + representation.knowledgeStrategy.getComment().word;
            }

            String interpretation = representation.knowledgeStrategy.getInterpretation();
            KnowledgeStrategy.Scene scene = representation.knowledgeStrategy.getScene(template.theme);

            StringBuilder content = new StringBuilder();
            if (null == scene) {
                content.append(interpretation);
            }
            else {
                content.append(scene.explain);
            }

            // 表征
            String prompt = template.formatBehaviorPrompt(content.toString(), age, gender, marked);
            String answer = this.service.syncGenerateText(ModelConfig.BAIZE_UNIT, prompt, null);

            if (null != answer) {
                result.add(answer);
                System.out.println("****************************************");
                System.out.println("XJW:\n" + prompt);
                System.out.println("----------------------------------------");
                System.out.println("XJW:\n" + answer);
                System.out.println("****************************************");
            }
        }

        return null;
    }

//    private List<String> markRepresentation() {
//        List<String> result = new ArrayList<>();
//        for (EvaluationReport.Representation representation : this.evaluationReport.getRepresentationListOrderByScore()) {
//            if (representation.positive > 1) {
//                result.add(HighTrick + "的" + representation.interpretation.getComment().word);
//            }
//            else if (representation.positive > 0) {
//                result.add(NormalTrick + "的" + representation.interpretation.getComment().word);
//            }
//            else {
//                result.add(LowTrick + "的" + representation.interpretation.getComment().word);
//            }
//        }
//        return result;
//    }

    private String spliceRepresentationInterpretation() {
        StringBuilder buf = new StringBuilder();
        for (EvaluationReport.Representation representation : this.evaluationReport.getRepresentationListOrderByScore()) {
            buf.append(representation.knowledgeStrategy.getInterpretation()).append("\n");
            if (buf.length() > ModelConfig.BAIZE_UNIT_CONTEXT_LIMIT - 100) {
                Logger.w(this.getClass(), "#spliceRepresentationInterpretation - Context length is overflow: " + buf.length());
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
                    representation.knowledgeStrategy.getComment().word,
                    representation.knowledgeStrategy.getInterpretation());
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

            int index = 0;
            for (String line : lines) {
                if (line.contains("：") || line.contains(":")) {
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

    private List<String> plainText(List<String> list) {
        List<String> result = new ArrayList<>(list.size());
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
            content = content.replaceAll("我", "你")
                    .replaceAll("他们", "你")
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
        lines = this.plainText(lines);

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
