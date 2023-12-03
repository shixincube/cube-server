/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 Cube Team.
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
import cube.aigc.Consts;
import cube.aigc.Prompt;
import cube.aigc.PromptBuilder;
import cube.aigc.PromptChaining;
import cube.aigc.psychology.*;
import cube.aigc.psychology.composition.Score;
import cube.common.entity.AIGCChannel;
import cube.common.entity.AIGCGenerationRecord;
import cube.common.entity.AIGCUnit;
import cube.service.aigc.AIGCService;
import cube.service.aigc.listener.ChatListener;

import java.util.ArrayList;
import java.util.List;

/**
 * 评估报告。
 */
public class EvaluationReport {

    public final static String HighTrick = "明显";
    public final static String LowTrick = "不足";

    private List<ReportScore> reportScoreList;

    public EvaluationReport(List<Evaluation.Result> resultList) {
        this.reportScoreList = new ArrayList<>();
        this.build(resultList);
    }

    private void build(List<Evaluation.Result> resultList) {
        for (Evaluation.Result result : resultList) {
            ReportScore score = this.getReportScore(result.comment);
            if (null == score) {
                CommentInterpretation interpretation = Resource.getInstance().getCommentInterpretation(result.comment);
                if (null == interpretation) {
                    // 没有对应的释义
                    Logger.e(this.getClass(), "#build - Can NOT find comment interpretation: " + result.comment.word);
                    continue;
                }

                score = new ReportScore(interpretation);
                this.reportScoreList.add(score);
            }

            if (result.score == Score.High) {
                score.positive += 1;
            }
            else {
                score.negative += 1;
            }
        }
    }

    public ReportScore getReportScore(Comment comment) {
        for (ReportScore score : this.reportScoreList) {
            if (score.interpretation.getComment() == comment) {
                return score;
            }
        }
        return null;
    }

    public List<ReportScore> getReportScoreList() {
        return this.reportScoreList;
    }

    public ThemeTemplate makeStress(AIGCChannel channel, AIGCService aigcService) {
        // 正负向分类
        List<ReportScore> positiveScoreList = new ArrayList<>();
        List<ReportScore> negativeScoreList = new ArrayList<>();
        for (ReportScore score : this.reportScoreList) {
            if (score.positive > 0) {
                positiveScoreList.add(score);
            }
            else {
                negativeScoreList.add(score);
            }
        }

        // 创建模板
        ThemeTemplate template = ThemeTemplate.makeStressThemeTemplate();

        // 构建提示语
        Workflow workflow = new Workflow();

        // Phase 1
        for (ReportScore score : positiveScoreList) {
            String word = score.interpretation.getComment().word;

            AIGCGenerationRecord record = new AIGCGenerationRecord(word, score.interpretation.getInterpretation());

            String prompt = template.formatFeaturePrompt(score.positive > 1 ? word + HighTrick : word);
            PromptChaining chaining = new PromptChaining(Consts.PROMPT_ROLE_PSYCHOLOGY);
            chaining.addPrompt(new Prompt(prompt));

            workflow.addPhase1(record, chaining);
        }
        for (ReportScore score : negativeScoreList) {
            String word = score.interpretation.getComment().word;
            AIGCGenerationRecord record = new AIGCGenerationRecord(word, score.interpretation.getInterpretation());

            String prompt = template.formatFeaturePrompt(word + LowTrick);
            PromptChaining chaining = new PromptChaining(Consts.PROMPT_ROLE_PSYCHOLOGY);
            chaining.addPrompt(new Prompt(prompt));

            workflow.addPhase1(record, chaining);
        }

        PromptBuilder promptBuilder = new PromptBuilder();

        // 调用 AGI 生成结果
        AIGCUnit unit = aigcService.selectUnitByName("Chat");
        for (Workflow.PromptGroup group : workflow.getPhase1Groups()) {
            List<AIGCGenerationRecord> records = new ArrayList<>();
            records.add(group.record);
            aigcService.singleChat(channel, unit,
                    promptBuilder.serializePromptChaining(group.chaining), records,
                    new ChatListener() {
                        @Override
                        public void onChat(AIGCChannel channel, AIGCGenerationRecord record) {
                        }

                        @Override
                        public void onFailed(AIGCChannel channel) {
                        }
                    });
        }


        // Phase 2

        return template;
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

    public class ReportScore {

        public CommentInterpretation interpretation;

        public int positive = 0;

        public int negative = 0;

        public ReportScore(CommentInterpretation interpretation) {
            this.interpretation = interpretation;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof ReportScore) {
                ReportScore score = (ReportScore) obj;
                if (score.interpretation.getComment() == this.interpretation.getComment()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public int hashCode() {
            return this.interpretation.getComment().hashCode();
        }
    }
}
