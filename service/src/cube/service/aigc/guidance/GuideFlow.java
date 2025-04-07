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
import cube.service.aigc.AIGCService;
import cube.util.ConfigUtils;
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

    private Question current;

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
        this.current = this.sectionList.get(0).getQuestion(0);
    }

    public void stop() {
        this.endTimestamp = System.currentTimeMillis();
    }

    @Override
    public Question getCurrentQuestion() {
        return this.current;
    }

    public String makeQuestion(boolean containsQuestion) {
        try {
            Question question = this.current;
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

            if (!question.answers.isEmpty()) {
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

    public String makeQuestionGroup(List<String> groups) {
        StringBuilder buf = new StringBuilder();
        for (String group : groups) {
            List<Answer> answers = this.current.getGroupAnswers(group);
            String content = this.current.getGroupContent(group);
            buf.append(content);
            buf.append("\n\n");
            for (Answer answer : answers) {
                buf.append("- [").append(answer.content).append("](");
                buf.append("aixinli://guide.answer/")
                        .append(current.sn).append("/")
                        .append(answer.code).append("/")
                        .append(answer.content);
                buf.append(")\n\n");
            }
        }
        return buf.toString();
    }

    @Override
    public void input(String currentQuestionAnswer, Answer candidate) {
        this.current.setAnswerContent(currentQuestionAnswer);

        Router router = this.current.router;
        if (null != router) {
            if (Router.RULE_TRUE_FALSE.equalsIgnoreCase(router.getRule())) {
                // 答案判断
                String prompt = String.format(
                        Prompts.getPrompt("FORMAT_TRUE_OR_FALSE"),
                        currentQuestionAnswer);
                GeneratingRecord result = service.syncGenerateText(ModelConfig.BAIZE_UNIT,
                        prompt, null, null, null);

                if (result.answer.trim().equalsIgnoreCase(Prompts.getPrompt("YES"))) {
                    // 设置答案
                    this.current.setAnswer(this.current.getAnswer("true"));
                    Router.RouteExport export = router.getRouteExport("true");

                    if (!export.answerMap.isEmpty()) {
                        for (Map.Entry<String, String> entry : export.answerMap.entrySet()) {
                            setQuestionAnswer(entry.getKey(), entry.getValue());
                        }
                    }

                    jumpQuestion(export.jump, currentQuestionAnswer);
                }
                else if (result.answer.trim().equalsIgnoreCase(Prompts.getPrompt("NO"))) {
                    // 设置答案
                    this.current.setAnswer(this.current.getAnswer("false"));
                    Router.RouteExport export = router.getRouteExport("false");

                    if (!export.answerMap.isEmpty()) {
                        for (Map.Entry<String, String> entry : export.answerMap.entrySet()) {
                            setQuestionAnswer(entry.getKey(), entry.getValue());
                        }
                    }

                    jumpQuestion(export.jump, currentQuestionAnswer);
                }
                else {
                    // 解释提问
                    answerQuestion(currentQuestionAnswer);
                }
            }
        }
        else {
            // 下一个
            nextQuestion();
        }
    }

    private void jumpQuestion(String sn, String query) {
        // 切换当前问题
        Question question = getQuestion(sn);
        this.current = question;

        Question.Precondition precondition = this.current.precondition;
        if (null != precondition) {
            List<Question> questionList = new ArrayList<>();
            for (String item : precondition.items) {
                questionList.add(getQuestion(item));
            }

            StringBuilder buf = new StringBuilder();
            List<String> groups = execPrecondition(precondition.condition, questionList);
            for (String group : groups) {
                String c = this.current.getGroupContent(group);
                buf.append(c).append("和");
            }
            buf.delete(buf.length() - 1, buf.length());

            String prompt = String.format(
                    Prompts.getPrompt("FORMAT_NEXT_QUESTION_WITH_PRECONDITION"),
                    String.format(current.prefix, buf.toString()),
                    current.question,
                    buf.toString() + "的情况");

            this.service.getExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    ComplexContext complexContext = new ComplexContext(ComplexContext.Type.Lightweight);
                    complexContext.setSubtask(Subtask.GuideFlow);

                    String answer = current.question;
                    GeneratingRecord result = service.syncGenerateText(ModelConfig.BAIZE_UNIT, prompt.toString(),
                            null, null, null);
                    if (null != result) {
                        answer = result.answer;
                    }

                    answer += "\n\n" + makeQuestionGroup(groups).trim();

                    GeneratingRecord record = new GeneratingRecord(query);
                    record.answer = answer;
                    record.context = complexContext;

                    // 回调
                    listener.onResponse(current, record);
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
                    listener.onResponse(current, record);
                }
            });
        }
    }

    private void nextQuestion() {

    }

    private void answerQuestion(String query) {
        ComplexContext complexContext = new ComplexContext(ComplexContext.Type.Lightweight);
        complexContext.setSubtask(Subtask.GuideFlow);

        this.service.getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                String prompt = String.format(
                        Prompts.getPrompt("FORMAT_EXPLAIN_QUESTION"),
                        current.question,
                        query);
                GeneratingRecord result = service.syncGenerateText(ModelConfig.BAIZE_X_UNIT,
                        prompt, null, null, null);

                GeneratingRecord record = new GeneratingRecord(query);
                record.answer = result.answer + makeQuestion(false);
                record.context = complexContext;

                // 回调
                listener.onResponse(current, record);
            }
        });
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
            Logger.w(this.getClass(), "#calc - Script file error: "
                    + mainScriptFile.toFile().getAbsolutePath());
            return null;
        }

        StringBuilder script = new StringBuilder();
        try {
            script.append(new String(Files.readAllBytes(mainScriptFile), StandardCharsets.UTF_8));
        } catch (Exception e) {
            Logger.e(this.getClass(), "#calc", e);
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
            Logger.e(this.getClass(), "#calc", e);
            return null;
        }
    }
}
