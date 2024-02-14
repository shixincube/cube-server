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
import cube.aigc.Prompt;
import cube.aigc.PromptBuilder;
import cube.aigc.psychology.EvaluationReport;
import cube.aigc.psychology.Resource;
import cube.aigc.psychology.Theme;
import cube.aigc.psychology.ThemeTemplate;
import cube.common.entity.AIGCChannel;
import cube.common.entity.AIGCGenerationRecord;
import cube.service.aigc.AIGCService;

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

    private List<Paragraph> paragraphList;

    public Workflow(EvaluationReport evaluationReport, AIGCChannel channel, AIGCService service) {
        this.evaluationReport = evaluationReport;
        this.channel = channel;
        this.service = service;
        this.paragraphList = new ArrayList<>();
    }

    public Workflow makeStress() {
        // 获取模板
        ThemeTemplate template = Resource.getInstance().getThemeTemplate(Theme.Stress.code);

        // 生成上下文
        List<AIGCGenerationRecord> records = this.makeRepresentationContext(this.evaluationReport.getRepresentationList());
        if (Logger.isDebugLevel()) {
            Logger.d(this.getClass(), "#makeStress - Context length: " + records.size());
        }

        for (String title : template.getTitles()) {
            this.paragraphList.add(new Paragraph(title));
        }

        // 生成描述串
        String representationString = this.spliceRepresentation();

        // 格式化所有段落提示词
        List<String> descriptionPromptList = template.formatDescriptionPrompt(representationString);
        List<String> suggestionPromptList = template.formatSuggestionPrompt(representationString);
        for (int i = 0; i < this.paragraphList.size(); ++i) {
            Paragraph paragraph = this.paragraphList.get(i);

            String prompt = descriptionPromptList.get(i);
//            String result = this.service.syncGenerateText(ModelConfig.BAIZE_UNIT, prompt, records);
            String result = "测试-description-" + prompt;
            if (null == result) {
                Logger.w(this.getClass(), "#makeStress - Infer failed");
            }
            else {
                paragraph.description = result;
            }

            prompt = suggestionPromptList.get(i);
//            result = this.service.syncGenerateText(ModelConfig.BAIZE_UNIT, prompt, records);
            result = "测试-suggestion-" + prompt;
            if (null == result) {
                Logger.w(this.getClass(), "#makeStress - Infer failed");
            }
            else {
                paragraph.suggestion = result;
            }
        }

        for (Paragraph paragraph : this.paragraphList) {
            System.out.println(paragraph.markdown());
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

    public class Paragraph {

        public String title;

        public int score;

        public String description;

        public String suggestion;

        public Paragraph(String title) {
            this.title = title;
        }

        public String markdown() {
            StringBuilder buf = new StringBuilder();
            buf.append("## ").append(this.title).append("\n\n");
            buf.append("### ").append("描述\n\n");
            buf.append(this.description);
            buf.append("\n\n");
            buf.append("### ").append("建议\n\n");
            buf.append(this.suggestion);
            buf.append("\n");
            return buf.toString();
        }
    }
}
