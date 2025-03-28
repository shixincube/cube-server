/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.scene.subtask;

import cell.util.log.Logger;
import cube.aigc.ModelConfig;
import cube.aigc.psychology.Resource;
import cube.aigc.psychology.ScaleReport;
import cube.aigc.psychology.composition.*;
import cube.common.entity.AIGCChannel;
import cube.common.entity.ComplexContext;
import cube.common.entity.GeneratingRecord;
import cube.common.state.AIGCStateCode;
import cube.service.aigc.AIGCService;
import cube.service.aigc.listener.GenerateTextListener;
import cube.service.aigc.scene.ScaleReportListener;
import cube.service.aigc.scene.SceneManager;
import cube.util.TimeDuration;
import cube.util.TimeUtils;

import java.util.List;

public class QuestionnaireSubtask extends ConversationSubtask {

    public QuestionnaireSubtask(AIGCService service, AIGCChannel channel, String query,
                                ComplexContext context, ConversationRelation relation, ConversationContext convCtx,
                                GenerateTextListener listener) {
        super(Subtask.Questionnaire, service, channel, query, context, relation, convCtx, listener);
    }

    @Override
    public AIGCStateCode execute(Subtask roundSubtask) {
        final SceneManager.ScaleTrack scaleTrack = SceneManager.getInstance().getScaleTrack(this.channel.getCode());
        if (null == scaleTrack) {
            this.service.getExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    GeneratingRecord record = new GeneratingRecord(query);
                    record.answer = Resource.getInstance().getCorpus(CORPUS, "ANSWER_FAILED");
                    listener.onGenerated(channel, record);
                    channel.setProcessing(false);

                    SceneManager.getInstance().saveHistoryRecord(channel.getCode(), ModelConfig.AIXINLI,
                            convCtx, record);

                    // 取消子任务
                    convCtx.cancelCurrentSubtask();
                }
            });
            return AIGCStateCode.Ok;
        }

        if (!scaleTrack.started) {
            // 激活开始
            if (roundSubtask == Subtask.Yes) {
                scaleTrack.started = true;
                scaleTrack.questionCursor = 1;
                String questionMD = makeQuestion(scaleTrack.scale, scaleTrack.questionCursor);

                this.service.getExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        ComplexContext complexContext = new ComplexContext(ComplexContext.Type.Lightweight);
                        complexContext.setSubtask(Subtask.Questionnaire);

                        GeneratingRecord record = new GeneratingRecord(query);
                        record.answer = String.format(
                                Resource.getInstance().getCorpus(CORPUS, "FORMAT_ANSWER_SCALE_QUESTION"),
                                scaleTrack.questionCursor,
                                questionMD);
                        record.context = complexContext;
                        listener.onGenerated(channel, record);
                        channel.setProcessing(false);

                        SceneManager.getInstance().saveHistoryRecord(channel.getCode(), ModelConfig.AIXINLI,
                                convCtx, record);
                    }
                });
                return AIGCStateCode.Ok;
            }
            else if (roundSubtask == Subtask.No) {
                this.service.getExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        ComplexContext complexContext = new ComplexContext(ComplexContext.Type.Lightweight);
                        complexContext.setSubtask(Subtask.StopQuestionnaire);

                        GeneratingRecord record = new GeneratingRecord(query);
                        record.answer = polish(Resource.getInstance().getCorpus(CORPUS,
                                "ANSWER_BAD_STOP_QUESTIONNAIRE"));
                        record.context = complexContext;
                        listener.onGenerated(channel, record);
                        channel.setProcessing(false);

                        SceneManager.getInstance().saveHistoryRecord(channel.getCode(), ModelConfig.AIXINLI,
                                convCtx, record);

                        // 取消子任务
                        convCtx.cancelCurrentSubtask();
                    }
                });
                return AIGCStateCode.Ok;
            }
            else {
                // 无关提问
                scaleTrack.offQuery.add(query);

                if (scaleTrack.offQuery.size() > 2) {
                    this.service.getExecutor().execute(new Runnable() {
                        @Override
                        public void run() {
                            ComplexContext complexContext = new ComplexContext(ComplexContext.Type.Lightweight);
                            complexContext.setSubtask(Subtask.Questionnaire);

                            GeneratingRecord record = new GeneratingRecord(query);
                            record.answer = polish(String.format(
                                    Resource.getInstance().getCorpus(CORPUS,
                                    "FORMAT_ANSWER_MANY_OFF_TOPICS_END"),
                                    scaleTrack.offQuery.size()));
                            record.context = complexContext;
                            listener.onGenerated(channel, record);
                            channel.setProcessing(false);

                            SceneManager.getInstance().saveHistoryRecord(channel.getCode(), ModelConfig.AIXINLI,
                                    convCtx, record);

                            // 取消子任务
                            convCtx.cancelCurrentSubtask();
                        }
                    });
                    return AIGCStateCode.Ok;
                }
                else {
                    this.service.getExecutor().execute(new Runnable() {
                        @Override
                        public void run() {
                            ComplexContext complexContext = new ComplexContext(ComplexContext.Type.Lightweight);
                            complexContext.setSubtask(Subtask.Questionnaire);

                            GeneratingRecord record = new GeneratingRecord(query);
                            record.answer = polish(Resource.getInstance().getCorpus(CORPUS,
                                    "ANSWER_PLEASE_START_QUESTIONNAIRE"));
                            record.context = complexContext;
                            listener.onGenerated(channel, record);
                            channel.setProcessing(false);

                            SceneManager.getInstance().saveHistoryRecord(channel.getCode(), ModelConfig.AIXINLI,
                                    convCtx, record);
                        }
                    });
                    return AIGCStateCode.Ok;
                }
            }
        }

        if (roundSubtask == Subtask.StopQuestionnaire) {
            // 退出
            // 结束时间
            scaleTrack.scale.setEndTimestamp(System.currentTimeMillis());
            // 取消子任务
            this.convCtx.cancelCurrentSubtask();

            this.service.getExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    ComplexContext complexContext = new ComplexContext(ComplexContext.Type.Lightweight);
                    complexContext.setSubtask(Subtask.StopQuestionnaire);

                    TimeDuration duration = TimeUtils.calcTimeDuration(
                            scaleTrack.scale.getEndTimestamp() - scaleTrack.scale.getTimestamp());

                    GeneratingRecord record = new GeneratingRecord(query);
                    record.answer = polish(String.format(
                            Resource.getInstance().getCorpus(CORPUS, "FORMAT_ANSWER_GOOD_STOP_QUESTIONNAIRE"),
                            scaleTrack.scale.getAllChosenAnswers().size(),
                            duration.toHumanString(),
                            ""
                    ));
                    record.context = complexContext;
                    listener.onGenerated(channel, record);
                    channel.setProcessing(false);

                    SceneManager.getInstance().saveHistoryRecord(channel.getCode(), ModelConfig.AIXINLI,
                            convCtx, record);
                }
            });
            return AIGCStateCode.Ok;
        }

        scaleTrack.offQuery.add(query);
        // 判断 Query
        String answerCode = matchAnswer(query, scaleTrack.scale.getQuestion(scaleTrack.questionCursor).answers);
        if (null == answerCode) {
            // 无关话题
            return this.processOffQuery(scaleTrack, true);
        }

        scaleTrack.offQuery.clear();
        // 记录答案
        scaleTrack.scale.chooseAnswer(scaleTrack.questionCursor, answerCode);
        // 移动游标
        scaleTrack.questionCursor += 1;

        if (scaleTrack.questionCursor > scaleTrack.scale.numQuestions()) {
            // 结束
            this.service.getExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    ScaleReport report = service.generateScaleReport(channel, scaleTrack.scale, new ScaleReportListener() {
                        @Override
                        public void onReportEvaluating(ScaleReport report) {

                        }

                        @Override
                        public void onReportEvaluateCompleted(ScaleReport report) {

                        }

                        @Override
                        public void onReportEvaluateFailed(ScaleReport report) {

                        }
                    });

                    if (null == report) {

                    }

                    GeneratingRecord record = new GeneratingRecord(query);
                    record.answer = "结束";
                    listener.onGenerated(channel, record);
                    channel.setProcessing(false);

                    SceneManager.getInstance().saveHistoryRecord(channel.getCode(), ModelConfig.AIXINLI,
                            convCtx, record);
                }
            });
        }
        else {
            // 下一个问题
            String questionMD = makeQuestion(scaleTrack.scale, scaleTrack.questionCursor);
            this.service.getExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    ComplexContext complexContext = new ComplexContext(ComplexContext.Type.Lightweight);
                    complexContext.setSubtask(Subtask.Questionnaire);

                    StringBuilder answer = new StringBuilder();
                    answer.append(String.format(
                            Resource.getInstance().getCorpus(CORPUS, "FORMAT_ANSWER_SCALE_LAST_ANSWER"),
                            scaleTrack.questionCursor - 1,
                            answerCode,
                            scaleTrack.scale.getAnswer(scaleTrack.questionCursor - 1).content
                            ));
                    answer.append("\n\n");
                    answer.append(String.format(
                            Resource.getInstance().getCorpus(CORPUS, "FORMAT_ANSWER_SCALE_QUESTION"),
                            scaleTrack.questionCursor,
                            questionMD));

                    GeneratingRecord record = new GeneratingRecord(query);
                    record.answer = answer.toString();
                    record.context = complexContext;
                    listener.onGenerated(channel, record);
                    channel.setProcessing(false);

                    SceneManager.getInstance().saveHistoryRecord(channel.getCode(), ModelConfig.AIXINLI,
                            convCtx, record);
                }
            });
        }

        return AIGCStateCode.Ok;
    }

    private AIGCStateCode processOffQuery(SceneManager.ScaleTrack scaleTrack, boolean analysis) {
        this.service.getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                if (scaleTrack.offQuery.size() > 2) {
                    ComplexContext complexContext = new ComplexContext(ComplexContext.Type.Lightweight);
                    complexContext.setSubtask(Subtask.StopQuestionnaire);

                    // 停止答题
                    GeneratingRecord record = new GeneratingRecord(query);
                    record.answer = polish(Resource.getInstance().getCorpus(CORPUS,
                            "ANSWER_BAD_STOP_QUESTIONNAIRE"));
                    record.context = complexContext;
                    listener.onGenerated(channel, record);
                    channel.setProcessing(false);

                    SceneManager.getInstance().saveHistoryRecord(channel.getCode(), ModelConfig.AIXINLI,
                            convCtx, record);

                    // 取消子任务
                    convCtx.cancelCurrentSubtask();
                }
                else if (scaleTrack.offQuery.size() > 1) {
                    // 提示并进行指导
                    ComplexContext complexContext = new ComplexContext(ComplexContext.Type.Lightweight);
                    complexContext.setSubtask(Subtask.Questionnaire);

                    if (analysis) {
                        // 先分析提问，看提问是否与当前问题匹配
                        String prompt = String.format(
                                Resource.getInstance().getCorpus("prompt", "FORMAT_ANSWER_ANALYSIS_FOR_SCALE_QUESTION"),
                                scaleTrack.scale.getQuestion(scaleTrack.questionCursor).content,
                                query);
                        GeneratingRecord result = service.syncGenerateText(ModelConfig.BAIZE_NEXT_UNIT,
                                prompt, null, null, null);
                        if (null != result) {
                            String questionMD = makeQuestion(scaleTrack.scale, scaleTrack.questionCursor);

                            GeneratingRecord record = new GeneratingRecord(query);
                            record.answer = result.answer + "\n\n" + questionMD;
                            record.thought = result.thought;
                            record.context = complexContext;
                            listener.onGenerated(channel, record);
                            channel.setProcessing(false);

                            SceneManager.getInstance().saveHistoryRecord(channel.getCode(), ModelConfig.AIXINLI,
                                    convCtx, record);

                            if (result.answer.contains("不相关") || result.answer.contains("无关")) {
                                // 不相关，维持 off query 计数
                            }
                            else {
                                // 相关，减少 off query 计数
                                scaleTrack.offQuery.removeLast();
                            }
                        }
                        else {
                            String questionMD = makeQuestion(scaleTrack.scale, scaleTrack.questionCursor);
                            GeneratingRecord record = new GeneratingRecord(query);
                            record.answer = String.format(
                                    Resource.getInstance().getCorpus(CORPUS, "FORMAT_ANSWER_OFF_TOPICS"),
                                    questionMD);
                            record.context = complexContext;
                            listener.onGenerated(channel, record);
                            channel.setProcessing(false);

                            SceneManager.getInstance().saveHistoryRecord(channel.getCode(), ModelConfig.AIXINLI,
                                    convCtx, record);
                        }
                    }
                    else {
                        String prompt = String.format(
                                Resource.getInstance().getCorpus("prompt", "FORMAT_ANSWER_OFF_TOPICS"),
                                query);
                        GeneratingRecord result = service.syncGenerateText(ModelConfig.BAIZE_NEXT_UNIT,
                                prompt, null, null, null);
                        if (null != result) {
                            GeneratingRecord record = new GeneratingRecord(query);
                            record.answer = result.answer;
                            record.thought = result.thought;
                            record.context = complexContext;
                            listener.onGenerated(channel, record);
                            channel.setProcessing(false);

                            SceneManager.getInstance().saveHistoryRecord(channel.getCode(), ModelConfig.AIXINLI,
                                    convCtx, record);
                        }
                        else {
                            String questionMD = makeQuestion(scaleTrack.scale, scaleTrack.questionCursor);
                            GeneratingRecord record = new GeneratingRecord(query);
                            record.answer = String.format(
                                    Resource.getInstance().getCorpus(CORPUS, "FORMAT_ANSWER_OFF_TOPICS"),
                                    questionMD);
                            record.context = complexContext;
                            listener.onGenerated(channel, record);
                            channel.setProcessing(false);

                            SceneManager.getInstance().saveHistoryRecord(channel.getCode(), ModelConfig.AIXINLI,
                                    convCtx, record);
                        }
                    }
                }
                else {
                    ComplexContext complexContext = new ComplexContext(ComplexContext.Type.Lightweight);
                    complexContext.setSubtask(Subtask.Questionnaire);

                    if (analysis) {
                        // 先分析提问，看提问是否与当前问题匹配
                        String prompt = String.format(
                                Resource.getInstance().getCorpus("prompt", "FORMAT_ANSWER_ANALYSIS_FOR_SCALE_QUESTION"),
                                scaleTrack.scale.getQuestion(scaleTrack.questionCursor).content,
                                query);
                        GeneratingRecord result = service.syncGenerateText(ModelConfig.BAIZE_NEXT_UNIT,
                                prompt, null, null, null);
                        if (null != result) {
                            String questionMD = makeQuestion(scaleTrack.scale, scaleTrack.questionCursor);

                            GeneratingRecord record = new GeneratingRecord(query);
                            record.answer = result.answer + "\n\n" + questionMD;
                            record.thought = result.thought;
                            record.context = complexContext;
                            listener.onGenerated(channel, record);
                            channel.setProcessing(false);

                            SceneManager.getInstance().saveHistoryRecord(channel.getCode(), ModelConfig.AIXINLI,
                                    convCtx, record);

                            if (result.answer.contains("不相关") || result.answer.contains("无关")) {
                                // 不相关，维持 off query 计数
                            }
                            else {
                                // 相关，减少 off query 计数
                                scaleTrack.offQuery.removeLast();
                            }
                        }
                        else {
                            String questionMD = makeQuestion(scaleTrack.scale, scaleTrack.questionCursor);
                            GeneratingRecord record = new GeneratingRecord(query);
                            record.answer = String.format(
                                    Resource.getInstance().getCorpus(CORPUS, "FORMAT_ANSWER_OFF_TOPICS"),
                                    questionMD);
                            record.context = complexContext;
                            listener.onGenerated(channel, record);
                            channel.setProcessing(false);

                            SceneManager.getInstance().saveHistoryRecord(channel.getCode(), ModelConfig.AIXINLI,
                                    convCtx, record);
                        }
                    }
                    else {
                        String questionMD = makeQuestion(scaleTrack.scale, scaleTrack.questionCursor);
                        GeneratingRecord record = new GeneratingRecord(query);
                        record.answer = String.format(
                                Resource.getInstance().getCorpus(CORPUS, "FORMAT_ANSWER_OFF_TOPICS"),
                                questionMD);
                        record.context = complexContext;
                        listener.onGenerated(channel, record);
                        channel.setProcessing(false);

                        SceneManager.getInstance().saveHistoryRecord(channel.getCode(), ModelConfig.AIXINLI,
                                convCtx, record);
                    }
                }
            }
        });
        return AIGCStateCode.Ok;
    }

    private String makeQuestion(Scale scale, int sn) {
        StringBuilder buf = new StringBuilder();
        try {
            Question question = scale.getQuestion(sn);
            buf.append("问题").append(sn).append("：").append(question.content).append("\n\n");
            for (Answer answer : question.answers) {
                buf.append("* [");
                buf.append(answer.code).append(". ").append(answer.content);
                buf.append("](");
                buf.append("aixinli://scale.answer/")
                        .append(sn).append("/")
                        .append(answer.code).append("/")
                        .append(answer.content);
                buf.append(")");
                buf.append("\n\n");
            }
        } catch (Exception e) {
            Logger.e(this.getClass(), "#makeQuestion", e);
        }
        return buf.toString();
    }

    private String matchAnswer(String query, List<Answer> answers) {
        String queryText = query.toUpperCase();
        if (queryText.contains("A") || queryText.contains("1") ||
                queryText.contains("一") || queryText.contains("壹") || queryText.contains("①")) {
            return "A";
        }
        else if (queryText.contains("B") || queryText.contains("2") ||
                queryText.contains("二") || queryText.contains("贰") || queryText.contains("②")) {
            return "B";
        }
        else if (queryText.contains("C") || queryText.contains("3") ||
                queryText.contains("三") || queryText.contains("叁") || queryText.contains("③")) {
            return "C";
        }
        else if (queryText.contains("D") || queryText.contains("4") ||
                queryText.contains("四") || queryText.contains("肆") || queryText.contains("④")) {
            return "D";
        }
        else if (queryText.contains("E") || queryText.contains("5") ||
                queryText.contains("五") || queryText.contains("伍") || queryText.contains("⑤")) {
            return "E";
        }

        for (Answer answer : answers) {
            if (queryText.contains(answer.content) || queryText.contains(answer.code) ||
                    answer.content.contains(queryText)) {
                return answer.code;
            }
        }

        return null;
    }
}
