/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.guidance;

import cell.util.log.Logger;
import cube.aigc.ModelConfig;
import cube.aigc.guidance.*;
import cube.aigc.psychology.composition.Subtask;
import cube.common.entity.ComplexContext;
import cube.common.entity.GeneratingRecord;
import cube.common.state.AIGCStateCode;
import cube.service.aigc.AIGCService;
import cube.util.ConfigUtils;
import cube.util.TimeDuration;
import cube.util.TimeUtils;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import org.json.JSONArray;
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

public class GuideFlow extends GuideFlowable {

    private String path;

    private long loadTimestamp;

    private long startTimestamp;

    private long endTimestamp;

    private AIGCService service;

    private GuidanceSection currentSection;

    private Question currentQuestion;

    public GuideFlow(File file) {
        super();
        this.path = file.getParent();
        this.load(file);
    }

    public GuideFlow(JSONObject json) {
        super(json);
        this.path = json.getString("path");
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

    public String makeQuestion(boolean containsQuestion) {
        try {
            Question question = this.currentQuestion;
            String content = "";

            if (containsQuestion) {
                content = "**" + question.question + "**";
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
                    buf.append("aixinli://guide.answer/")
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
            buf.append("aixinli://guide.answer/")
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
                String prompt = String.format(
                        Prompts.getPrompt("FORMAT_TRUE_OR_FALSE"),
                        this.currentQuestion.question,
                        currentQuestionAnswer);
                GeneratingRecord result = service.syncGenerateText(ModelConfig.BAIZE_UNIT,
                        prompt, null, null, null);

                // -1 无，0 否，1 是
                int yesOrNo = -1;

                if (result.answer.trim().equalsIgnoreCase(Prompts.getPrompt("YES"))) {
                    if (null != candidate && candidate.code.equalsIgnoreCase("true")) {
                        yesOrNo = 1;
                    }
                }
                else if (result.answer.trim().equalsIgnoreCase(Prompts.getPrompt("NO"))) {
                    if (null != candidate && candidate.code.equalsIgnoreCase("false")) {
                        yesOrNo = 0;
                    }
                }
                else {
                    // 如果其他回复，则检测候选
                    if (null != candidate && candidate.code.equalsIgnoreCase("true")) {
                        yesOrNo = 1;
                    }
                    else if (null != candidate && candidate.code.equalsIgnoreCase("false")) {
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

                // 解释提问
                return answerQuestion(currentQuestionAnswer);
            }

            // 下一个
            return nextQuestion();
        }
        else {
            // 下一个
            return nextQuestion();
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
                        currentQuestion.question,
                        query,
                        currentQuestion.question);
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

    private AIGCStateCode nextQuestion() {
        return AIGCStateCode.Ok;
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
        }
        else {
            this.service.getExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    ComplexContext complexContext = new ComplexContext(ComplexContext.Type.Lightweight);
                    complexContext.setSubtask(Subtask.GuideFlow);

                    TimeDuration duration = TimeUtils.calcTimeDuration(
                            currentSection.endTimestamp - currentSection.startTimestamp);

                    GeneratingRecord record = new GeneratingRecord(query);
                    record.answer = String.format(
                            Prompts.getPrompt("FORMAT_EVALUATION_RESULT"),
                            currentSection.name,
                            result.toMarkdown(),
                            duration.toHumanStringDHMS());
                    record.context = complexContext;

                    // 回调
                    listener.onResponse(GuideFlow.this, record);
                }
            });
        }
        return AIGCStateCode.Ok;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("path", this.path);
        json.put("startTimestamp", this.startTimestamp);
        json.put("endTimestamp", this.endTimestamp);
        return json;
    }

    private void load(File file) {
        JSONObject jsonData = ConfigUtils.readJsonFile(file.getAbsolutePath());
        if (null == jsonData) {
            Logger.e(this.getClass(), "#load - Read file failed: " + file.getAbsolutePath());
            return;
        }

        this.loadTimestamp = file.lastModified();
        this.name = jsonData.getString("name");
        this.displayName = jsonData.getString("displayName");
        this.instruction = jsonData.getString("instruction");
        JSONArray array = jsonData.getJSONArray("sections");
        for (int i = 0; i < array.length(); ++i) {
            this.sectionList.add(new GuidanceSection(array.getJSONObject(i)));
        }
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
