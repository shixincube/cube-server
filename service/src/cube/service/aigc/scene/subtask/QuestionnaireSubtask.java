/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.scene.subtask;

import cube.aigc.ModelConfig;
import cube.aigc.psychology.Resource;
import cube.aigc.psychology.composition.*;
import cube.common.entity.AIGCChannel;
import cube.common.entity.ComplexContext;
import cube.common.entity.GeneratingRecord;
import cube.common.state.AIGCStateCode;
import cube.service.aigc.AIGCService;
import cube.service.aigc.listener.GenerateTextListener;
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
                        GeneratingRecord record = new GeneratingRecord(query);
                        record.answer = String.format(
                                Resource.getInstance().getCorpus(CORPUS, "FORMAT_ANSWER_SCALE_QUESTION"),
                                scaleTrack.questionCursor,
                                questionMD);
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
                        GeneratingRecord record = new GeneratingRecord(query);
                        record.answer = polish(Resource.getInstance().getCorpus(CORPUS,
                                "ANSWER_BAD_STOP_QUESTIONNAIRE"));
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
                            GeneratingRecord record = new GeneratingRecord(query);
                            record.answer = polish(String.format(
                                    Resource.getInstance().getCorpus(CORPUS,
                                    "FORMAT_ANSWER_MANY_OFF_TOPICS_END"),
                                    scaleTrack.offQuery.size()));
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
                            GeneratingRecord record = new GeneratingRecord(query);
                            record.answer = polish(Resource.getInstance().getCorpus(CORPUS,
                                    "ANSWER_PLEASE_START_QUESTIONNAIRE"));
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
                    TimeDuration duration = TimeUtils.calcTimeDuration(
                            scaleTrack.scale.getEndTimestamp() - scaleTrack.scale.getTimestamp());

                    GeneratingRecord record = new GeneratingRecord(query);
                    record.answer = polish(String.format(
                            Resource.getInstance().getCorpus(CORPUS, "FORMAT_ANSWER_GOOD_STOP_QUESTIONNAIRE"),
                            scaleTrack.scale.getAllChosenAnswers().size(),
                            duration.toHumanString(),
                            ""
                    ));
                    listener.onGenerated(channel, record);
                    channel.setProcessing(false);

                    SceneManager.getInstance().saveHistoryRecord(channel.getCode(), ModelConfig.AIXINLI,
                            convCtx, record);
                }
            });
            return AIGCStateCode.Ok;
        }
        else if (roundSubtask != Subtask.None) {
            // 无关话题
            scaleTrack.offQuery.add(query);
            return this.processOffQuery(scaleTrack, false);
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
                    GeneratingRecord record = new GeneratingRecord(query);
                    record.answer = String.format(
                            Resource.getInstance().getCorpus(CORPUS, "FORMAT_ANSWER_SCALE_QUESTION"),
                            scaleTrack.questionCursor,
                            questionMD);
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
                if (analysis) {
                    // 先分析提问，看提问是否与当前问题匹配
                    
                }

                if (scaleTrack.offQuery.size() > 2) {
                    // 停止答题
                    service.getExecutor().execute(new Runnable() {
                        @Override
                        public void run() {
                            GeneratingRecord record = new GeneratingRecord(query);
                            record.answer = polish(Resource.getInstance().getCorpus(CORPUS,
                                    "ANSWER_BAD_STOP_QUESTIONNAIRE"));
                            listener.onGenerated(channel, record);
                            channel.setProcessing(false);

                            SceneManager.getInstance().saveHistoryRecord(channel.getCode(), ModelConfig.AIXINLI,
                                    convCtx, record);

                            // 取消子任务
                            convCtx.cancelCurrentSubtask();
                        }
                    });
                }
                else if (scaleTrack.offQuery.size() > 1) {
                    // 提示并进行指导
                    service.getExecutor().execute(new Runnable() {
                        @Override
                        public void run() {
                            String prompt = String.format(
                                    Resource.getInstance().getCorpus("prompt", "FORMAT_ANSWER_OFF_TOPICS"),
                                    query);
                            GeneratingRecord result = service.syncGenerateText(ModelConfig.BAIZE_NEXT_UNIT,
                                    prompt, null, null, null);
                            if (null != result) {
                                GeneratingRecord record = new GeneratingRecord(query);
                                record.answer = result.answer;
                                record.thought = result.thought;
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
                                listener.onGenerated(channel, record);
                                channel.setProcessing(false);

                                SceneManager.getInstance().saveHistoryRecord(channel.getCode(), ModelConfig.AIXINLI,
                                        convCtx, record);
                            }
                        }
                    });
                }
                else {
                    service.getExecutor().execute(new Runnable() {
                        @Override
                        public void run() {
                            String questionMD = makeQuestion(scaleTrack.scale, scaleTrack.questionCursor);
                            GeneratingRecord record = new GeneratingRecord(query);
                            record.answer = String.format(
                                    Resource.getInstance().getCorpus(CORPUS, "FORMAT_ANSWER_OFF_TOPICS"),
                                    questionMD);
                            listener.onGenerated(channel, record);
                            channel.setProcessing(false);

                            SceneManager.getInstance().saveHistoryRecord(channel.getCode(), ModelConfig.AIXINLI,
                                    convCtx, record);
                        }
                    });
                }
            }
        });
        return AIGCStateCode.Ok;
    }

    private String makeQuestion(Scale scale, int sn) {
        StringBuilder buf = new StringBuilder();
        Question question = scale.getQuestion(sn);
        buf.append("问题").append(sn).append("：").append(question.content).append("\n\n");
        for (Answer answer : question.answers) {
            buf.append("* [");
            buf.append(answer.code).append(". ").append(answer.content);
            buf.append("](");
            buf.append("aixinli://scale.answer/").append(sn).append("/").append(answer.code);
            buf.append(")");
            buf.append("\n\n");
        }
        return buf.toString();
    }

    private String matchAnswer(String query, List<Answer> answers) {
        String queryText = query.toUpperCase();
        if (queryText.contains("A") || queryText.contains("1") || queryText.contains("一")) {
            return "A";
        }
        else if (queryText.contains("B") || queryText.contains("2") || queryText.contains("二")) {
            return "B";
        }
        else if (queryText.contains("C") || queryText.contains("3") || queryText.contains("三")) {
            return "C";
        }
        else if (queryText.contains("D") || queryText.contains("4") || queryText.contains("四")) {
            return "D";
        }
        else if (queryText.contains("E") || queryText.contains("5") || queryText.contains("五")) {
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
