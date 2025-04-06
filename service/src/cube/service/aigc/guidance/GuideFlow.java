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
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;

public class GuideFlow extends GuideFlowable {

    private long loadTimestamp;

    private long startTimestamp;

    private long endTimestamp;

    private AIGCService service;

    private Question current;

    public GuideFlow(File file) {
        super();
        this.load(file);
    }

    public GuideFlow(JSONObject json) {
        super(json);
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

    @Override
    public void input(String currentQuestionAnswer, Answer candidate) {
        this.current.setAnswerContent(currentQuestionAnswer);

        Router router = this.current.router;
        if (null != router) {
            if (Router.RULE_TRUE_FALSE.equalsIgnoreCase(router.getRule())) {
                if (null == candidate) {
                    // 答案判断
                    String prompt = String.format(
                            Prompts.getPrompt("FORMAT_TRUE_OR_FALSE"),
                            currentQuestionAnswer);
                    GeneratingRecord result = service.syncGenerateText(ModelConfig.BAIZE_UNIT,
                            prompt, null, null, null);
                    if (result.answer.trim().equalsIgnoreCase(Prompts.getPrompt("YES"))) {
                        Router.RouteExport export = router.getRouteExport("true");
                        jumpQuestion(export.jump);
                    }
                    else if (result.answer.trim().equalsIgnoreCase(Prompts.getPrompt("NO"))) {
                        Router.RouteExport export = router.getRouteExport("false");
                        jumpQuestion(export.jump);
                    }
                    else {
                        // 解释提问
                        answerQuestion(currentQuestionAnswer);
                    }
                }
            }
        }
        else {
            // 下一个

        }
    }

    private void jumpQuestion(String sn) {
        Question question = getQuestion(sn);
        this.current = question;
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
}
