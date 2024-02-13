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

    public Workflow(EvaluationReport evaluationReport, AIGCChannel channel, AIGCService service) {
        this.evaluationReport = evaluationReport;
        this.channel = channel;
        this.service = service;
    }

    public Workflow makeStress() {
        // 获取模板
        ThemeTemplate template = Resource.getInstance().getThemeTemplate(Theme.Stress.code);

        // 生成描述串
        String representationString = this.spliceRepresentation();

        template.formatDescriptionPrompt(representationString);

        /*
        // Phase 1
        for (EvaluationReport.ReportScore score : positiveScoreList) {
            String word = score.interpretation.getComment().word;

            AIGCGenerationRecord record = new AIGCGenerationRecord(UNIT, word, score.interpretation.getInterpretation());

            String prompt = template.formatFeaturePrompt(score.positive > 1 ? word + HighTrick : word);
            PromptChaining chaining = new PromptChaining(Consts.PROMPT_ROLE_PSYCHOLOGY);
            chaining.addPrompt(new Prompt(prompt));

            workflow.addPhase1(record, chaining);
        }
        for (EvaluationReport.ReportScore score : negativeScoreList) {
            String word = score.interpretation.getComment().word;
            AIGCGenerationRecord record = new AIGCGenerationRecord(UNIT, word, score.interpretation.getInterpretation());

            String prompt = template.formatFeaturePrompt(word + LowTrick);
            PromptChaining chaining = new PromptChaining(Consts.PROMPT_ROLE_PSYCHOLOGY);
            chaining.addPrompt(new Prompt(prompt));

            workflow.addPhase1(record, chaining);
        }

        PromptBuilder promptBuilder = new PromptBuilder();

        // 生成结果
        AIGCUnit unit = aigcService.selectUnitByName(ModelConfig.BAIZE_UNIT);
        for (Workflow.PromptGroup group : workflow.getPhase1Groups()) {
            List<AIGCGenerationRecord> records = new ArrayList<>();
            records.add(group.record);
            aigcService.generateText(channel, unit,
                    promptBuilder.serializePromptChaining(group.chaining),
                    promptBuilder.serializePromptChaining(group.chaining), records, null, false, false,
                    new GenerateTextListener() {
                        @Override
                        public void onGenerated(AIGCChannel channel, AIGCGenerationRecord record) {
                        }

                        @Override
                        public void onFailed(AIGCChannel channel, AIGCStateCode stateCode) {
                        }
                    });
        }*/

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
                buf.append(representation.interpretation.getComment()).append(HighTrick);
            }
            else if (representation.positive > 0) {
                buf.append(representation.interpretation.getComment()).append(NormalTrick);
            }
            else {
                buf.append(representation.interpretation.getComment()).append(LowTrick);
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
        PromptBuilder builder = new PromptBuilder();
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

        public Prompt descriptionPrompt;

        public String suggestion;

        public Prompt suggestionPrompt;

        public Paragraph() {
        }
    }
}
