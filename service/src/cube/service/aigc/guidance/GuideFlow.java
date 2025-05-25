/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.guidance;

import cell.util.log.Logger;
import cube.aigc.ModelConfig;
import cube.aigc.guidance.*;
import cube.aigc.psychology.app.Link;
import cube.aigc.psychology.composition.Subtask;
import cube.common.entity.ComplexContext;
import cube.common.entity.GeneratingRecord;
import cube.common.state.AIGCStateCode;
import cube.service.aigc.AIGCService;
import cube.util.TimeDuration;
import cube.util.TimeUtils;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import org.json.JSONObject;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GuideFlow extends AbstractGuideFlow {

    private long loadTimestamp;

    private long startTimestamp;

    private long endTimestamp;

    private AIGCService service;

    private GuidanceSection currentSection;

    private Question currentQuestion;

    public GuideFlow(File file) {
        super(file);
        this.loadTimestamp = file.lastModified();
    }

    public GuideFlow(JSONObject json) {
        super(json.getString("path"), json);
        this.startTimestamp = json.getLong("startTimestamp");
        this.endTimestamp = json.getLong("endTimestamp");
    }

    public long getLoadTimestamp() {
        return this.loadTimestamp;
    }

    public long getEndTimestamp() {
        return this.endTimestamp;
    }

    public long getStartTimestamp() {
        return this.startTimestamp;
    }

    public void start(AIGCService service) {
        this.service = service;
        this.startTimestamp = System.currentTimeMillis();
        this.currentSection = this.sectionList.get(0);
        this.currentSection.startTimestamp = System.currentTimeMillis();
        this.currentQuestion = this.currentSection.getQuestion(0);
    }

    @Override
    public void stop() {
        this.endTimestamp = System.currentTimeMillis();
    }

    @Override
    public GuidanceSection getCurrentSection() {
        return this.currentSection;
    }

    @Override
    public Question getCurrentQuestion() {
        return this.currentQuestion;
    }

    public boolean hasNextSection() {
        if (null == this.currentSection) {
            return true;
        }

        int index = this.sectionList.indexOf(this.currentSection);
        return (index >= 0 && (index + 1) < this.sectionList.size());
    }

    public String makeQuestion(boolean containsQuestion) {
        try {
            Question question = this.currentQuestion;
            String content = "";

            if (containsQuestion) {
                if (question.question.startsWith("|")) {
                    content = question.question;
                }
                else {
                    content = "**" + question.question + "**";
                }
                if (!question.original) {
                    String prompt = String.format(
                            Prompts.getPrompt("FORMAT_POLISH"),
                            content);
                    GeneratingRecord result = service.syncGenerateText(ModelConfig.BAIZE_X_UNIT, prompt,
                            null, null, null);
                    content = "**" + result.answer + "**";
                }
            }

            if (null != question.answers && !question.answers.isEmpty()) {
                StringBuilder buf = new StringBuilder();
                buf.append("\n\n请使用“是”或“否”作答：");
                for (Answer answer : question.answers) {
                    buf.append("\n\n* [");
                    buf.append(answer.content);
                    buf.append("](");
                    buf.append(Link.GuideAnswer)
                            .append(question.sn).append("/")
                            .append(answer.code).append("/")
                            .append(answer.content);
                    buf.append(")");
                }
                content += buf.toString();
            }

            return content;
        } catch (Exception e) {
            Logger.w(this.getClass(), "#makeQuestion", e);
            return null;
        }
    }

    private String makeQuestionAnswer(AnswerGroup answerGroup) {
        StringBuilder buf = new StringBuilder();

        for (Answer answer : answerGroup.answers) {
            buf.append("- [");
            buf.append(answerGroup.content).append("：").append(answer.content);
            buf.append("](");
            buf.append(Link.GuideAnswer)
                    .append(currentQuestion.sn).append("/")
                    .append(answer.code).append("/")
                    .append(answerGroup.content).append("：").append(answer.content);
            buf.append(")\n\n");
        }
        return buf.toString();
    }

    @Override
    public AIGCStateCode input(String currentQuestionAnswer, Answer candidate) {
        // 添加回答内容
        this.currentQuestion.appendAnswerContent(currentQuestionAnswer);

        Router router = this.currentQuestion.router;
        if (null != router) {
            if (Router.RULE_TRUE_FALSE.equalsIgnoreCase(router.getRule())) {
                // 答案判断
                // -1 无，0 否，1 是
                int yesOrNo = -1;

                if (null != candidate) {
                    Logger.d(this.getClass(), "#input - candidate: " + candidate.code);

                    // 检测候选
                    if (candidate.code.equalsIgnoreCase("true")) {
                        yesOrNo = 1;
                    }
                    else if (candidate.code.equalsIgnoreCase("false")) {
                        yesOrNo = 0;
                    }
                }
                else {
                    String prompt = String.format(
                            Prompts.getPrompt("FORMAT_TRUE_OR_FALSE"),
                            this.currentQuestion.question,
                            currentQuestionAnswer);
                    GeneratingRecord result = service.syncGenerateText(ModelConfig.BAIZE_NEXT_UNIT,
                            prompt, null, null, null);

                    if (result.answer.trim().equalsIgnoreCase(Prompts.getPrompt("YES"))) {
                        yesOrNo = 1;
                    }
                    else if (result.answer.trim().equalsIgnoreCase(Prompts.getPrompt("NO"))) {
                        yesOrNo = 0;
                    }
                }

                if (1 == yesOrNo) {
                    // 处理答案组
                    if (this.currentQuestion.isAnswerGroup()) {
                        // 设置答案
                        AnswerGroup answerGroup = this.currentQuestion.getAnswerGroupByState(AnswerGroup.STATE_ANSWERING);
                        this.currentQuestion.setGroupAnswer(answerGroup.group, answerGroup.getAnswer("true"));

                        if (!this.currentQuestion.hasAnswerGroupCompleted()) {
                            return nextAnswerGroup(currentQuestionAnswer);
                        }
                        else {
                            Router.RouteExport export = router.getRouteExport(Router.EXPORT_TRUE);
                            if (null != export) {
                                return jumpQuestion(export.jump, currentQuestionAnswer);
                            }
                            else {
                                export = router.getRouteExport(Router.EXPORT_EVALUATION);
                                return evaluationSection(export.script, currentQuestionAnswer);
                            }
                        }
                    }
                    else {
                        // 设置答案
                        this.currentQuestion.setAnswer(this.currentQuestion.getAnswer("true"));
                        Router.RouteExport export = router.getRouteExport(Router.EXPORT_TRUE);
                        if (null != export) {
                            if (export.hasAnswer()) {
                                for (Map.Entry<String, String> entry : export.answerMap.entrySet()) {
                                    setQuestionAnswer(entry.getKey(), entry.getValue());
                                }
                            }
                            return jumpQuestion(export.jump, currentQuestionAnswer);
                        }
                        else {
                            export = router.getRouteExport(Router.EXPORT_EVALUATION);
                            return evaluationSection(export.script, currentQuestionAnswer);
                        }
                    }
                }
                else if (0 == yesOrNo) {
                    // 处理答案组
                    if (this.currentQuestion.isAnswerGroup()) {
                        // 设置答案
                        AnswerGroup answerGroup = this.currentQuestion.getAnswerGroupByState(AnswerGroup.STATE_ANSWERING);
                        this.currentQuestion.setGroupAnswer(answerGroup.group, answerGroup.getAnswer("false"));

                        if (!this.currentQuestion.hasAnswerGroupCompleted()) {
                            return nextAnswerGroup(currentQuestionAnswer);
                        }
                        else {
                            Router.RouteExport export = router.getRouteExport(Router.EXPORT_FALSE);
                            if (null != export) {
                                return jumpQuestion(export.jump, currentQuestionAnswer);
                            }
                            else {
                                export = router.getRouteExport(Router.EXPORT_EVALUATION);
                                return evaluationSection(export.script, currentQuestionAnswer);
                            }
                        }
                    }
                    else {
                        // 设置答案
                        this.currentQuestion.setAnswer(this.currentQuestion.getAnswer("false"));
                        Router.RouteExport export = router.getRouteExport(Router.EXPORT_FALSE);
                        if (null != export) {
                            if (export.hasAnswer()) {
                                for (Map.Entry<String, String> entry : export.answerMap.entrySet()) {
                                    setQuestionAnswer(entry.getKey(), entry.getValue());
                                }
                            }
                            return jumpQuestion(export.jump, currentQuestionAnswer);
                        }
                        else {
                            export = router.getRouteExport(Router.EXPORT_EVALUATION);
                            return evaluationSection(export.script, currentQuestionAnswer);
                        }
                    }
                }
                else {
                    Logger.w(this.getClass(), "#input - currentQuestionAnswer: " + currentQuestionAnswer);
                }

                // 解释提问
                return answerQuestion(currentQuestionAnswer);
            }
            else {
                // TODO 其他路由策略
                return AIGCStateCode.IllegalOperation;
            }
        }
        else {
            // TODO 没有路由时的策略
            return AIGCStateCode.InvalidParameter;
        }
    }

    private AIGCStateCode jumpQuestion(String sn, String query) {
        // 切换当前问题
        Question question = getQuestion(sn);
        this.currentQuestion = question;

        Question.Precondition precondition = this.currentQuestion.precondition;
        if (null != precondition) {
            List<Question> questionList = new ArrayList<>();
            for (String item : precondition.items) {
                questionList.add(getQuestion(item));
            }

            List<String> groups = execPrecondition(precondition.condition, questionList);
            // 启用组
            List<AnswerGroup> answerGroups = this.currentQuestion.enabledGroups(groups);
            AnswerGroup answerGroup = answerGroups.get(0);
            // 设置状态
            answerGroup.state = AnswerGroup.STATE_ANSWERING;

            String prompt = String.format(
                    Prompts.getPrompt("FORMAT_NEXT_QUESTION_WITH_PRECONDITION"),
                    answerGroup.content,
                    currentQuestion.prefix,
                    currentQuestion.question,
                    answerGroup.content);

            this.service.getExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    ComplexContext complexContext = new ComplexContext(ComplexContext.Type.Lightweight);
                    complexContext.setSubtask(Subtask.GuideFlow);

                    String answer = currentQuestion.question;
                    GeneratingRecord result = service.syncGenerateText(ModelConfig.BAIZE_UNIT, prompt.toString(),
                            null, null, null);
                    if (null != result) {
                        answer = result.answer;
                    }

                    answer += "\n\n" + makeQuestionAnswer(answerGroup).trim();

                    GeneratingRecord record = new GeneratingRecord(query);
                    record.answer = answer;
                    record.context = complexContext;

                    // 回调
                    listener.onResponse(GuideFlow.this, record);
                }
            });
        }
        else {
            this.service.getExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    ComplexContext complexContext = new ComplexContext(ComplexContext.Type.Lightweight);
                    complexContext.setSubtask(Subtask.GuideFlow);

                    String answer = String.format(Prompts.getPrompt("FORMAT_NEXT_QUESTION"),
                            makeQuestion(true));

                    GeneratingRecord record = new GeneratingRecord(query);
                    record.answer = answer;
                    record.context = complexContext;

                    // 回调
                    listener.onResponse(GuideFlow.this, record);
                }
            });
        }

        return AIGCStateCode.Ok;
    }

    private AIGCStateCode answerQuestion(String query) {
        ComplexContext complexContext = new ComplexContext(ComplexContext.Type.Lightweight);
        complexContext.setSubtask(Subtask.GuideFlow);

        this.service.getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                String prompt = String.format(
                        Prompts.getPrompt("FORMAT_EXPLAIN_QUESTION"),
                        getExplainPrefix(),
                        (null == currentQuestion.constraint) ? currentQuestion.question : currentQuestion.constraint,
                        query.replaceAll("\\*\\*", ""),
                        (null == currentQuestion.constraint) ? currentQuestion.question : currentQuestion.constraint);
                GeneratingRecord result = service.syncGenerateText(ModelConfig.BAIZE_X_UNIT,
                        prompt, null, null, null);

                GeneratingRecord record = new GeneratingRecord(query);
                record.answer = result.answer + makeQuestion(false);
                record.context = complexContext;

                // 回调
                listener.onResponse(GuideFlow.this, record);
            }
        });

        return AIGCStateCode.Ok;
    }

    private AIGCStateCode nextAnswerGroup(String query) {
        AnswerGroup answerGroup = null;
        for (AnswerGroup ag : this.currentQuestion.answerGroups) {
            if (ag.state != AnswerGroup.STATE_ANSWERED) {
                answerGroup = ag;
                break;
            }
        }

        if (null == answerGroup) {
            Logger.e(this.getClass(), "#nextAnswerGroup");
            return AIGCStateCode.Failure;
        }

        String prompt = String.format(
                Prompts.getPrompt("FORMAT_NEXT_QUESTION_WITH_PRECONDITION"),
                answerGroup.content,
                currentQuestion.prefix,
                currentQuestion.question,
                answerGroup.content);

        final AnswerGroup constAnswerGroup = answerGroup;
        this.service.getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                ComplexContext complexContext = new ComplexContext(ComplexContext.Type.Lightweight);
                complexContext.setSubtask(Subtask.GuideFlow);

                String answer = currentQuestion.question;
                GeneratingRecord result = service.syncGenerateText(ModelConfig.BAIZE_UNIT, prompt,
                        null, null, null);
                if (null != result) {
                    answer = result.answer;
                }

                answer += "\n\n" + makeQuestionAnswer(constAnswerGroup).trim();

                GeneratingRecord record = new GeneratingRecord(query);
                record.answer = answer;
                record.context = complexContext;

                // 回调
                listener.onResponse(GuideFlow.this, record);
            }
        });

        return AIGCStateCode.Ok;
    }

    private AIGCStateCode nextSection(String query) {
        int index = this.sectionList.indexOf(this.currentSection);
        if (index + 1 >= this.sectionList.size()) {
            // 判断是否正常结束
            if (hasCompleted()) {
                Logger.d(this.getClass(), "#nextSection - Guide flow has completed (NO result): " + this.getName());

                this.service.getExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        ComplexContext complexContext = new ComplexContext(ComplexContext.Type.Lightweight);
                        complexContext.setSubtask(Subtask.StopGuideFlow);

                        GeneratingRecord answer = service.syncGenerateText(ModelConfig.BAIZE_X_UNIT,
                                String.format(Prompts.getPrompt("FORMAT_POLISH"), noResults),
                                null, null, null);

                        GeneratingRecord record = new GeneratingRecord(query);
                        record.answer = answer.answer.replaceAll("\"", "");
                        record.context = complexContext;

                        // 回调
                        listener.onResponse(GuideFlow.this, record);
                    }
                });

                return AIGCStateCode.Ok;
            }
            else {
                Logger.w(this.getClass(), "#nextSection - No next question in " + this.getName());
                return AIGCStateCode.Failure;
            }
        }

        Logger.d(this.getClass(), "#nextSection - Next section: " + this.getName());
        this.currentSection = this.sectionList.get(index + 1);
        this.currentSection.startTimestamp = System.currentTimeMillis();
        this.currentQuestion = this.currentSection.getQuestion(0);

        return jumpQuestion(this.currentQuestion.sn, query);
    }

    private AIGCStateCode evaluationSection(String evaluation, String query) {
        this.currentSection.endTimestamp = System.currentTimeMillis();

        EvaluationResult result = execEvaluation(evaluation, this.currentSection.getAllQuestions());
        if (null == result) {
            this.service.getExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    ComplexContext complexContext = new ComplexContext(ComplexContext.Type.Lightweight);
                    complexContext.setSubtask(Subtask.GuideFlow);

                    GeneratingRecord record = new GeneratingRecord(query);
                    record.answer = Prompts.getPrompt("FAILED");
                    record.context = complexContext;

                    // 回调
                    listener.onResponse(GuideFlow.this, record);
                }
            });
            return AIGCStateCode.Ok;
        }
        else {
            // 记录结果
            this.currentSection.evaluationResult = result;
            this.endTimestamp = System.currentTimeMillis();

            if (result.hasResult()) {
                // 有结果，输出结果
                this.service.getExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        ComplexContext complexContext = new ComplexContext(ComplexContext.Type.Lightweight);
                        if (hasCompleted()) {
                            complexContext.setSubtask(Subtask.StopGuideFlow);
                        }
                        else {
                            complexContext.setSubtask(Subtask.GuideFlow);
                        }

                        TimeDuration duration = TimeUtils.calcTimeDuration(
                                endTimestamp - startTimestamp);

                        GeneratingRecord record = new GeneratingRecord(query);
                        record.answer = String.format(
                                Prompts.getPrompt("FORMAT_EVALUATION_RESULT"),
                                currentSection.name,
                                getMainKeyword(),
                                result.toMarkdown(),
                                getMainKeyword(),
                                duration.toHumanStringDHMS());
                        record.context = complexContext;

                        // 回调
                        listener.onResponse(GuideFlow.this, record);
                    }
                });
                return AIGCStateCode.Ok;
            }
            else {
                if (result.hasTerminated()) {
                    // 没有结果且被终止
                    this.service.getExecutor().execute(new Runnable() {
                        @Override
                        public void run() {
                            ComplexContext complexContext = new ComplexContext(ComplexContext.Type.Lightweight);
                            complexContext.setSubtask(Subtask.StopGuideFlow);

                            GeneratingRecord answer = service.syncGenerateText(ModelConfig.BAIZE_X_UNIT,
                                    String.format(Prompts.getPrompt("FORMAT_POLISH"), questionableResults),
                                    null, null, null);

                            GeneratingRecord record = new GeneratingRecord(query);
                            record.answer = answer.answer;
                            record.context = complexContext;

                            // 回调
                            listener.onResponse(GuideFlow.this, record);
                        }
                    });

                    return AIGCStateCode.Ok;
                }
                else {
                    // 没有输出结果数据，跳转到下一段
                    return this.nextSection(query);
                }
            }
        }
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("startTimestamp", this.startTimestamp);
        json.put("endTimestamp", this.endTimestamp);
        return json;
    }

    private List<String> execPrecondition(String scriptFile, List<Question> questionList) {
        Path mainScriptFile = Paths.get(this.path, scriptFile);
        if (!Files.exists(mainScriptFile)) {
            Logger.w(this.getClass(), "#execPrecondition - Script file error: "
                    + mainScriptFile.toFile().getAbsolutePath());
            return null;
        }

        StringBuilder script = new StringBuilder();
        try {
            script.append(new String(Files.readAllBytes(mainScriptFile), StandardCharsets.UTF_8));
        } catch (Exception e) {
            Logger.e(this.getClass(), "#execPrecondition", e);
            return null;
        }

        Object[] args = new Object[questionList.size()];
        int index = 0;
        for (Question question : questionList) {
            if (question.getAnswer().code.equals("true") || question.getAnswer().code.equals("false")) {
                args[index++] = Boolean.parseBoolean(question.getAnswer().code);
            }
            else {
                args[index++] = question.getAnswer().code;
            }
        }

        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("js");
        try {
            engine.eval(script.toString());
            Invocable invocable = (Invocable) engine;
            ScriptObjectMirror returnVal = (ScriptObjectMirror) invocable.invokeFunction("main", args);
            if (returnVal.isArray()) {
                List<String> resultList = new ArrayList<>();
                for (Map.Entry<String, Object> entry : returnVal.entrySet()) {
                    resultList.add(entry.getValue().toString());
                }
                return resultList;
            }
            else {
                return null;
            }
        } catch (ScriptException | NoSuchMethodException e) {
            Logger.e(this.getClass(), "#execPrecondition", e);
            return null;
        }
    }

    private EvaluationResult execEvaluation(String scriptFile, List<Question> questionList) {
        Path mainScriptFile = Paths.get(this.path, scriptFile);
        if (!Files.exists(mainScriptFile)) {
            Logger.w(this.getClass(), "#execEvaluation - Script file error: "
                    + mainScriptFile.toFile().getAbsolutePath());
            return null;
        }

        StringBuilder script = new StringBuilder();
        try {
            script.append("var Logger = Java.type('cell.util.log.Logger');\n");
            script.append("var Question = Java.type('cube.aigc.guidance.Question');\n");
            script.append("var Answer = Java.type('cube.aigc.guidance.Answer');\n");
            script.append("var EvaluationResult = Java.type('cube.aigc.guidance.EvaluationResult');\n");
            script.append(new String(Files.readAllBytes(mainScriptFile), StandardCharsets.UTF_8));
        } catch (Exception e) {
            Logger.e(this.getClass(), "#execEvaluation", e);
            return null;
        }

        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("js");
        try {
            engine.eval(script.toString());
            Invocable invocable = (Invocable) engine;
            return (EvaluationResult) invocable.invokeFunction("main", questionList);
        } catch (Exception e) {
            Logger.e(this.getClass(), "#execEvaluation", e);
            return null;
        }
    }
}
