/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.service.aigc.scene;

import cell.core.talk.dialect.ActionDialect;
import cell.util.log.Logger;
import cube.aigc.ModelConfig;
import cube.aigc.psychology.ComprehensiveReport;
import cube.aigc.psychology.EvaluationReport;
import cube.aigc.psychology.Painting;
import cube.aigc.psychology.Theme;
import cube.aigc.psychology.algorithm.Score;
import cube.aigc.psychology.composition.Answer;
import cube.aigc.psychology.composition.Comprehensive;
import cube.aigc.psychology.composition.EvaluationScore;
import cube.aigc.psychology.composition.Scale;
import cube.common.Packet;
import cube.common.action.AIGCAction;
import cube.common.entity.AIGCChannel;
import cube.common.entity.AIGCUnit;
import cube.common.entity.FileLabel;
import cube.common.state.AIGCStateCode;
import cube.service.aigc.AIGCService;
import cube.service.aigc.scene.evaluation.Evaluation;
import cube.service.aigc.scene.evaluation.SubconsciousRelationshipBetweenACoupleEvaluation;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ComprehensiveReportWorker implements Runnable {

    private AIGCService service;

    private AIGCChannel channel;

    private ComprehensiveReportListener listener;

    private ComprehensiveReport report;

    public ComprehensiveReportWorker(AIGCService service, AIGCChannel channel, Theme theme,
                                     List<Comprehensive> comprehensives, ComprehensiveReportListener listener) {
        this.service = service;
        this.channel = channel;
        this.listener = listener;
        this.report = new ComprehensiveReport(theme, comprehensives);
    }

    public ComprehensiveReport getReport() {
        return this.report;
    }

    @Override
    public void run() {
        try {
            // 设置为正在操作
            this.channel.setProcessing(true);

            // 获取单元
            AIGCUnit unit = this.service.selectUnitByName(ModelConfig.PSYCHOLOGY_UNIT);
            if (null == unit) {
                // 没有可用单元
                this.report.state = AIGCStateCode.UnitError;
                this.report.finished = true;
                this.listener.onEvaluateFailed(this.report);
                return;
            }

            // 更新单元状态
            unit.setRunning(true);

            // 回调正在评估
            this.listener.onEvaluating(this.report);

            for (Comprehensive comprehensive : this.report.comprehensives) {
                // 回调正在预测内容
                this.listener.onPredicting(this.report, comprehensive);

                // 1. 推理绘画
                Painting painting = this.predictPainting(unit, comprehensive.getFileLabel(), true, false);
                if (null == painting) {
                    // 预测绘图失败
                    Logger.w(PsychologyScene.class, "#run - #predictPainting failed: " +
                            comprehensive.getFileLabel().getFileCode());
                    // 更新单元状态
                    unit.setRunning(false);
                    this.report.state = AIGCStateCode.FileError;
                    this.report.finished = true;
                    this.listener.onEvaluateFailed(this.report);
                    return;
                }

                // 设置绘画
                painting.setAttribute(comprehensive.getAttribute());
                painting.fileLabel = comprehensive.getFileLabel();
                comprehensive.setPainting(comprehensive.getFileLabel().getFileCode(), painting);

                // 2. 执行绘画评估
                Evaluation evaluation = this.evaluate(painting);
                if (null == evaluation) {
                    Logger.w(PsychologyScene.class, "#run - #evaluate failed: " +
                            comprehensive.getFileLabel().getFileCode());
                    // 更新单元状态
                    unit.setRunning(false);
                    this.report.state = AIGCStateCode.FileError;
                    this.report.finished = true;
                    this.listener.onEvaluateFailed(this.report);
                    return;
                }

                // 制作报告
                EvaluationReport evaluationReport = evaluation.makeEvaluationReport();

                // 3. 执行预测
                this.predictComprehensive(comprehensive, evaluation, evaluationReport);
            }

            // 4. 合成结果


            // 更新单元状态
            unit.setRunning(false);
        } catch (Exception e) {
            Logger.e(this.getClass(), "#run", e);
        } finally {
            this.channel.setProcessing(false);
        }
    }

    private Painting predictPainting(AIGCUnit unit, FileLabel fileLabel, boolean adjust, boolean upload) {
        JSONObject data = new JSONObject();
        data.put("fileLabel", fileLabel.toJSON());
        data.put("adjust", adjust);
        data.put("upload", upload);
        Packet request = new Packet(AIGCAction.PredictPsychologyPainting.name, data);
        ActionDialect dialect = this.service.getCellet().transmit(unit.getContext(), request.toDialect(), 5 * 60 * 1000);
        if (null == dialect) {
            Logger.w(this.getClass(), "#predictPainting - Predict image unit error");
            return null;
        }

        Packet response = new Packet(dialect);
        if (Packet.extractCode(response) != AIGCStateCode.Ok.code) {
            Logger.w(this.getClass(), "#predictPainting - Predict image response state: " +
                    Packet.extractCode(response));
            return null;
        }

        try {
            JSONObject responseData = Packet.extractDataPayload(response);
            // 绘画识别结果
            Painting painting = new Painting(responseData.getJSONArray("result").getJSONObject(0));
            return painting;
        } catch (Exception e) {
            Logger.e(this.getClass(), "#predictPainting", e);
            return null;
        }
    }

    private Evaluation evaluate(Painting painting) {
        Evaluation evaluation = null;
        switch (this.report.theme) {
            case SubconsciousRelationshipBetweenACouple:
                evaluation = new SubconsciousRelationshipBetweenACoupleEvaluation(
                        this.channel.getAuthToken().getContactId(), painting);
                break;
            default:
                break;
        }

        if (null == evaluation) {
            Logger.w(this.getClass(), "#evaluate - Unsupported evaluate theme: " + this.report.theme.code);
            return null;
        }

        return evaluation;
    }

    private void predictComprehensive(Comprehensive comprehensive, Evaluation evaluation, EvaluationReport evaluationReport) {
        switch (this.report.theme) {
            case SubconsciousRelationshipBetweenACouple:
                SubconsciousRelationshipBetweenACoupleEvaluation srbcEvaluation =
                        (SubconsciousRelationshipBetweenACoupleEvaluation) evaluation;

                // 提取绘画分数
                List<EvaluationScore> scoreList = evaluationReport.getEvaluationScores();
                // 合并指标
                List<EvaluationScore> paintingScores = srbcEvaluation.mergeScoreList(scoreList);
                printEvaluationScore("Painting Scores", paintingScores);

                // 提取词分数
                Scale scale = comprehensive.getScale();
                List<Answer> answerList = scale.getQuestions().get(0).getChosenAnswers();
                List<String> words = new ArrayList<>();
                for (Answer answer : answerList) {
                    words.add(answer.content);
                }
                // 词指标
                List<EvaluationScore> wordScores = srbcEvaluation.evaluateWords(words);
                printEvaluationScore("Word Scores", wordScores);

                // 计算指标得分
                List<Score> scores = srbcEvaluation.caleIndicatorScores(paintingScores, wordScores);
                printScore("Total", scores);

                Score result = null;
                int max = 0;
                for (Score score : scores) {
                    if (score.value > max) {
                        max = score.value;
                        result = score;
                    }
                }



                // Resource.getInstance().loadDataset().getContent();
                break;
            default:
                Logger.w(this.getClass(), "#predictComprehensive - Unsupported theme: " + this.report.theme.code);
                break;
        }
    }

    private void printEvaluationScore(String title, List<EvaluationScore> scores) {
        StringBuilder buf = new StringBuilder();
        buf.append("----------------------------------------------------------------\n");
        buf.append(title).append("\n");
        for (EvaluationScore score : scores) {
            buf.append(score.indicator.getName());
            buf.append(" - ");
            buf.append(score.calcScore());
            buf.append("\n");
        }
        System.out.println(buf.toString());
    }

    private void printScore(String title, List<Score> scores) {
        StringBuilder buf = new StringBuilder();
        buf.append("----------------------------------------------------------------\n");
        buf.append(title).append("\n");
        for (Score score : scores) {
            buf.append(score.indicator.getName());
            buf.append(" - ");
            buf.append(score.value);
            buf.append("\n");
        }
        System.out.println(buf.toString());
    }
}
