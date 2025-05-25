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
import cube.aigc.psychology.app.Link;
import cube.aigc.psychology.composition.*;
import cube.common.entity.AIGCChannel;
import cube.common.entity.ComplexContext;
import cube.common.entity.GeneratingRecord;
import cube.common.state.AIGCStateCode;
import cube.service.aigc.AIGCService;
import cube.service.aigc.listener.GenerateTextListener;
import cube.service.aigc.scene.PsychologyScene;
import cube.service.aigc.scene.ScaleReportListener;
import cube.service.aigc.scene.SceneManager;
import cube.util.TextUtils;
import cube.util.TimeDuration;
import cube.util.TimeUtils;

import java.util.List;

public class QuestionnaireSubtask extends ConversationSubtask {

    /**
     * 放弃当前问卷的无关话题阈值。
     * 当无关话题数据达到该值时，放弃当前问卷。
     */
    private final int abortQuestionnaireThreshold = 3;

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

                        if (scaleTrack.getQuestion().isDescriptive()) {
                            GeneratingRecord result = service.syncGenerateText(ModelConfig.BAIZE_X_UNIT, questionMD,
                                    null, null, null);
                            if (null == result) {
                                result = service.syncGenerateText(ModelConfig.BAIZE_UNIT, questionMD,
                                        null, null, null);
                            }
                            record.answer = (null != result) ? result.answer : scaleTrack.getQuestion().content;
                            record.answer = filterList(record.answer, 5);
                            scaleTrack.getQuestion().questionContent = record.answer;
                        }
                        else {
                            record.answer = String.format(
                                    Resource.getInstance().getCorpus(CORPUS, "FORMAT_ANSWER_SCALE_QUESTION"),
                                    scaleTrack.questionCursor,
                                    questionMD);
                        }

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
                scaleTrack.offQueries.add(query);

                if (scaleTrack.offQueries.size() >= abortQuestionnaireThreshold) {
                    this.service.getExecutor().execute(new Runnable() {
                        @Override
                        public void run() {
                            ComplexContext complexContext = new ComplexContext(ComplexContext.Type.Lightweight);
                            complexContext.setSubtask(Subtask.Questionnaire);

                            GeneratingRecord record = new GeneratingRecord(query);
                            record.answer = polish(String.format(
                                    Resource.getInstance().getCorpus(CORPUS,
                                    "FORMAT_ANSWER_MANY_OFF_TOPICS_END"),
                                    scaleTrack.offQueries.size()));
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
                            duration.toHumanStringDHMS(),
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
        else if (roundSubtask == Subtask.Yes) {
            // 执行继续
            Logger.d(this.getClass(), "#execute - The roundSubtask is YES: " + channel.getCode());

            if (scaleTrack.questionCursor >= scaleTrack.scale.numQuestions() || scaleTrack.scale.isComplete()) {
                // 结束
                return this.processFinish(scaleTrack);
            }
            else {
                // 当前问题
                this.service.getExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        ComplexContext complexContext = new ComplexContext(ComplexContext.Type.Lightweight);
                        complexContext.setSubtask(Subtask.Questionnaire);

                        StringBuilder answer = new StringBuilder();

                        Question question = scaleTrack.getQuestion();
                        Question prevQuestion = null;
                        if (scaleTrack.questionCursor - 1 > 0) {
                            prevQuestion = scaleTrack.scale.getQuestion(scaleTrack.questionCursor - 1);
                        }

                        if (question.isDescriptive()) {
                            String questionMD = makeQuestion(scaleTrack.scale, scaleTrack.questionCursor);
                            GeneratingRecord result = service.syncGenerateText(ModelConfig.BAIZE_X_UNIT, questionMD,
                                    null, null, null);
                            if (null == result) {
                                result = service.syncGenerateText(ModelConfig.BAIZE_NEXT_UNIT, questionMD,
                                        null, null, null);
                            }
                            String resultAnswer = filterList((null != result) ? result.answer : question.content, 5);
                            answer.append(resultAnswer);
                            question.questionContent = resultAnswer;
                        }
                        else {
                            if (null != prevQuestion) {
                                answer.append(String.format(
                                        Resource.getInstance().getCorpus(CORPUS, "FORMAT_ANSWER_SCALE_LAST_ANSWER"),
                                        scaleTrack.questionCursor - 1,
                                        scaleTrack.scale.getAnswer(scaleTrack.questionCursor - 1).code,
                                        scaleTrack.scale.getAnswer(scaleTrack.questionCursor - 1).content
                                ));
                                answer.append("\n\n");
                            }
                            String questionMD = makeQuestion(scaleTrack.scale, scaleTrack.questionCursor);
                            answer.append(String.format(
                                    Resource.getInstance().getCorpus(CORPUS, "FORMAT_ANSWER_SCALE_QUESTION"),
                                    scaleTrack.questionCursor,
                                    questionMD));
                        }

                        GeneratingRecord record = new GeneratingRecord(query);
                        record.answer = answer.toString();
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

        // 先添加为无关问题
        scaleTrack.offQueries.add(query);

        Question question = scaleTrack.getQuestion();
        if (question.isDescriptive()) {
            // 判断 Query 内容的匹配信息
            String prompt = String.format(
                    Resource.getInstance().getCorpus(CORPUS_PROMPT, "FORMAT_ANALYSIS_QUESTION_POSSIBILITY"),
                    query.replaceAll("\n", ""),
                    question.questionContent.replaceAll("\n", ""));
            GeneratingRecord result = this.service.syncGenerateText(ModelConfig.BAIZE_X_UNIT, prompt,
                    null, null, null);
            if (null == result) {
                result = this.service.syncGenerateText(ModelConfig.BAIZE_UNIT, prompt,
                        null, null, null);
            }

            if (null == result) {
                // 错误
                ComplexContext complexContext = new ComplexContext(ComplexContext.Type.Lightweight);
                complexContext.setSubtask(Subtask.Questionnaire);

                GeneratingRecord record = new GeneratingRecord(query);
                record.answer = Resource.getInstance().getCorpus(CORPUS, "ANSWER_FAILED");
                record.context = complexContext;
                listener.onGenerated(channel, record);
                channel.setProcessing(false);

                SceneManager.getInstance().saveHistoryRecord(channel.getCode(), ModelConfig.AIXINLI,
                        convCtx, record);
                return AIGCStateCode.Ok;
            }

            if (result.answer.contains(Resource.getInstance().getCorpus(CORPUS_PROMPT, "NO")) &&
                    roundSubtask != Subtask.No) {
                // 不是
                Logger.d(this.getClass(), "#execute - The answer is NO - " + channel.getCode()
                        + " - " + scaleTrack.scale.getSN());
                return this.processOffQuery(scaleTrack);
            }
            else {
                // 是
                Logger.d(this.getClass(), "#execute - The answer is YES - " + channel.getCode()
                        + " - " + scaleTrack.scale.getSN());
                return this.processOnQuery(scaleTrack);
            }
        }
        else {
            // 判断 Query 里的选项
            String answerCode = this.matchSingleChoiceAnswer(
                    scaleTrack.scale.getQuestion(scaleTrack.questionCursor).answers);
            if (null == answerCode) {
                // 无关话题
                return this.processOffQueryForChoice(scaleTrack, true);
            }

            scaleTrack.offQueries.clear();
            // 记录答案
            scaleTrack.scale.chooseAnswer(scaleTrack.questionCursor, answerCode);
            // 移动游标
            scaleTrack.questionCursor += 1;

            if (scaleTrack.questionCursor > scaleTrack.scale.numQuestions()) {
                // 结束
                return this.processFinish(scaleTrack);
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
                return AIGCStateCode.Ok;
            }
        }
    }

    private AIGCStateCode processFinish(SceneManager.ScaleTrack scaleTrack) {
        this.service.getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                // 记录时间
                scaleTrack.scale.setEndTimestamp(System.currentTimeMillis());

                // 处理隐藏问题
                List<Question> hiddenQuestions = scaleTrack.scale.getHiddenQuestions();
                if (!hiddenQuestions.isEmpty()) {
                    for (Question question : hiddenQuestions) {
                        // 如果填写了推理则进行推理
                        if (question.inference.length() > 1) {
                            PsychologyScene.getInstance().inferScaleAnswer(scaleTrack.scale, question.sn);
                        }
                        else {
                            question.chooseAnswer(question.answers.get(0).code);
                        }
                    }
                }

                // 检查选项
                int countdown = 90;
                List<Question> questions = scaleTrack.scale.getQuestions();
                while (countdown > 0) {
                    --countdown;
                    int num = 0;
                    for (Question question : questions) {
                        if (question.hasChosen()) {
                            ++num;
                        }
                    }
                    if (num >= questions.size()) {
                        break;
                    }

                    Logger.d(this.getClass(), "#processFinish - Waiting for finish: " + num + "/" + questions.size());

                    // 等待
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                final Object mutex = new Object();
                final ScaleReport scaleReport = service.generateScaleReport(channel, scaleTrack.scale, new ScaleReportListener() {
                    @Override
                    public void onReportEvaluating(ScaleReport report) {
                        synchronized (mutex) {
                            mutex.notify();
                        }
                    }

                    @Override
                    public void onReportEvaluateCompleted(ScaleReport report) {
                        synchronized (mutex) {
                            mutex.notify();
                        }
                    }

                    @Override
                    public void onReportEvaluateFailed(ScaleReport report) {
                        synchronized (mutex) {
                            mutex.notify();
                        }
                    }
                });

                if (null == scaleReport) {
                    ComplexContext complexContext = new ComplexContext(ComplexContext.Type.Lightweight);
                    complexContext.setSubtask(Subtask.StopQuestionnaire);

                    GeneratingRecord record = new GeneratingRecord(query);
                    record.answer = Resource.getInstance().getCorpus(CORPUS, "ANSWER_FAILED");
                    record.context = complexContext;
                    listener.onGenerated(channel, record);
                    channel.setProcessing(false);

                    SceneManager.getInstance().saveHistoryRecord(channel.getCode(), ModelConfig.AIXINLI,
                            convCtx, record);

                    // 清空子任务
                    convCtx.cancelCurrentSubtask();
                    return;
                }

                synchronized (mutex) {
                    try {
                        mutex.wait(120 * 1000);
                    } catch (InterruptedException e) {
                        Logger.e(this.getClass(), "", e);
                    }
                }

                TimeDuration duration = TimeUtils.calcTimeDuration(
                        scaleTrack.scale.getEndTimestamp() - scaleTrack.scale.getTimestamp());

                ComplexContext complexContext = new ComplexContext(ComplexContext.Type.Lightweight);
                complexContext.setSubtask(Subtask.StopQuestionnaire);

                String prefix = polish(String.format(
                        Resource.getInstance().getCorpus(CORPUS, "FORMAT_ANSWER_SCALE_RESULT_PREFIX"),
                        scaleTrack.scale.numQuestions(),
                        duration.toHumanStringDHMS()));

                GeneratingRecord record = new GeneratingRecord(query);
                record.answer = prefix + "\n\n" + scaleReport.getResult().content;

                // 增加阅读说明
                record.answer = "\n\n" + Resource.getInstance().getCorpus("report", "READING_INSTRUCTION");

                record.context = complexContext;
                listener.onGenerated(channel, record);
                channel.setProcessing(false);

                SceneManager.getInstance().saveHistoryRecord(channel.getCode(), ModelConfig.AIXINLI,
                        convCtx, record);

                // 清空子任务
                convCtx.cancelCurrentSubtask();
            }
        });

        return AIGCStateCode.Ok;
    }

    private AIGCStateCode processOffQuery(SceneManager.ScaleTrack scaleTrack) {
        if (scaleTrack.offQueries.size() >= abortQuestionnaireThreshold) {
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
        else {
            ComplexContext complexContext = new ComplexContext(ComplexContext.Type.Lightweight);
            complexContext.setSubtask(Subtask.Questionnaire);

            String answer = String.format(
                    Resource.getInstance().getCorpus(CORPUS, "FORMAT_ANSWER_OFF_TOPICS"),
                    scaleTrack.scale.getQuestion(scaleTrack.questionCursor).content,
                    "");
            GeneratingRecord record = new GeneratingRecord(query);
            record.answer = filterSecondPerson(fastPolish(answer));
            record.context = complexContext;
            listener.onGenerated(channel, record);
            channel.setProcessing(false);

            SceneManager.getInstance().saveHistoryRecord(channel.getCode(), ModelConfig.AIXINLI,
                    convCtx, record);
        }
        return AIGCStateCode.Ok;
    }

    private AIGCStateCode processOnQuery(SceneManager.ScaleTrack scaleTrack) {
        // 清空之前的无关问题
        scaleTrack.offQueries.clear();

        Question question = scaleTrack.getQuestion();
        // 记录当前答案
        question.submitAnswerContent(query);

        // 在新线程里进行推理
        final Scale scale = scaleTrack.scale;
        final int sn = scaleTrack.questionCursor;
        this.service.getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                PsychologyScene.getInstance().inferScaleAnswer(scale, sn);
            }
        });

        if (!scaleTrack.scale.isComplete()) {
            // 下一个问题
            scaleTrack.questionCursor += 1;

            String prompt = makeQuestion(scaleTrack.scale, scaleTrack.questionCursor);
            this.service.getExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    ComplexContext complexContext = new ComplexContext(ComplexContext.Type.Lightweight);
                    complexContext.setSubtask(Subtask.Questionnaire);

                    GeneratingRecord result = service.syncGenerateText(ModelConfig.BAIZE_X_UNIT,
                            prompt, null, null, null);
                    if (null == result) {
                        result = service.syncGenerateText(ModelConfig.BAIZE_UNIT,
                                prompt, null, null, null);
                    }

                    GeneratingRecord record = new GeneratingRecord(query);

                    String resultAnswer = filterList(
                            (null != result) ? result.answer : scaleTrack.getQuestion().content, 5);

                    record.answer = fastPolish(Resource.getInstance().getCorpus(CORPUS, "PREFIX_NEXT_QUESTION"))
                            + "\n\n" + resultAnswer;
                    record.context = complexContext;

                    scaleTrack.getQuestion().questionContent = resultAnswer;

                    listener.onGenerated(channel, record);
                    channel.setProcessing(false);

                    SceneManager.getInstance().saveHistoryRecord(channel.getCode(), ModelConfig.AIXINLI,
                            convCtx, record);
                }
            });
            return AIGCStateCode.Ok;
        }
        else {
            return this.processFinish(scaleTrack);
        }
    }

    private AIGCStateCode processOffQueryForChoice(SceneManager.ScaleTrack scaleTrack, boolean analysis) {
        this.service.getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                if (scaleTrack.offQueries.size() >= abortQuestionnaireThreshold) {
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
                else if (scaleTrack.offQueries.size() > 1) {
                    // 提示并进行指导
                    ComplexContext complexContext = new ComplexContext(ComplexContext.Type.Lightweight);
                    complexContext.setSubtask(Subtask.Questionnaire);

                    if (analysis) {
                        // 先分析提问，看提问是否与当前问题匹配
                        String prompt = String.format(
                                Resource.getInstance().getCorpus(CORPUS_PROMPT, "FORMAT_ANSWER_ANALYSIS_FOR_SCALE_QUESTION"),
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
                                scaleTrack.offQueries.removeLast();
                            }
                        }
                        else {
                            String questionMD = makeQuestion(scaleTrack.scale, scaleTrack.questionCursor);
                            GeneratingRecord record = new GeneratingRecord(query);
                            record.answer = String.format(
                                    Resource.getInstance().getCorpus(CORPUS, "FORMAT_ANSWER_OFF_TOPICS"),
                                    scaleTrack.scale.getQuestion(scaleTrack.questionCursor).content,
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
                                Resource.getInstance().getCorpus(CORPUS_PROMPT, "FORMAT_TOPICS_POSSIBILITY_ANSWER"),
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
                                    scaleTrack.scale.getQuestion(scaleTrack.questionCursor).content,
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
                                Resource.getInstance().getCorpus(CORPUS_PROMPT, "FORMAT_ANSWER_ANALYSIS_FOR_SCALE_QUESTION"),
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
                                // Nothing
                            }
                            else {
                                // 相关，减少 off query 计数
                                scaleTrack.offQueries.removeLast();
                            }
                        }
                        else {
                            String questionMD = makeQuestion(scaleTrack.scale, scaleTrack.questionCursor);
                            GeneratingRecord record = new GeneratingRecord(query);
                            record.answer = String.format(
                                    Resource.getInstance().getCorpus(CORPUS, "FORMAT_ANSWER_OFF_TOPICS"),
                                    scaleTrack.scale.getQuestion(scaleTrack.questionCursor).content,
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
                                scaleTrack.scale.getQuestion(scaleTrack.questionCursor).content,
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
            if (question.isDescriptive()) {
                if (question.prompt.length() > 0) {
                    buf.append(question.prompt);
                }
                else {
                    buf.append(question.content);
                }
            }
            else {
                buf.append("问题").append(sn).append("：").append(question.content).append("\n\n");
                for (Answer answer : question.answers) {
                    buf.append("* [");
                    buf.append(answer.code).append(". ").append(answer.content);
                    buf.append("](");
                    buf.append(Link.ScaleAnswer)
                            .append(sn).append("/")
                            .append(answer.code).append("/")
                            .append(answer.content);
                    buf.append(")");
                    buf.append("\n\n");
                }
            }
        } catch (Exception e) {
            Logger.e(this.getClass(), "#makeQuestion", e);
        }
        return buf.toString();
    }

    private String matchSingleChoiceAnswer(List<Answer> answers) {
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

    /**
     * 将有序列表转分段。
     *
     * @param text
     * @param maxLines
     * @return
     */
    private String filterList(String text, int maxLines) {
        int count = 0;
        StringBuilder buf = new StringBuilder();
        String[] lines = text.split("\n");
        for (String line : lines) {
            if (line.length() == 0) {
                continue;
            }

            if (TextUtils.isNumeric(line.charAt(0))) {
                buf.append(line.substring(3));
            }
            else {
                buf.append(line);
            }
            buf.append("\n\n");
            ++count;
            if (count >= maxLines) {
                break;
            }
        }
        buf.delete(buf.length() - 2, buf.length());
        return buf.toString();
    }
}
