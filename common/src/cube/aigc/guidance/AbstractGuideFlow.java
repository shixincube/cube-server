/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.guidance;

import cell.util.log.Logger;
import cube.common.state.AIGCStateCode;
import cube.util.ConfigUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractGuideFlow implements GuideFlowable {

    protected final String path;

    protected String name;

    protected String displayName;

    /**
     * 匹配用关键词。第一个关键为主关键词，会用于最终评估时内容输出。
     */
    protected List<String> keywords;

    protected String instruction;

    /**
     * 当用户回答的答案偏离既定目标时，对用户进行解释时的提示词前缀。
     */
    protected String explainPrefix;

    /**
     * 没有结果时的话术。
     */
    protected String noResults;

    /**
     * 用户回答无法进行推理时的话术。
     */
    protected String questionableResults;

    protected List<GuidanceSection> sectionList;

    protected GuideListener listener;

    public AbstractGuideFlow(File file) {
        this.path = file.getParent();
        JSONObject jsonData = ConfigUtils.readJsonFile(file.getAbsolutePath());
        if (null == jsonData) {
            Logger.e(this.getClass(), "#load - Read file failed: " + file.getAbsolutePath());
            return;
        }
        this.parse(jsonData);
    }

    public AbstractGuideFlow(String path, JSONObject json) {
        this.path = path;
        this.parse(json);
    }

    private void parse(JSONObject json) {
        this.name = json.getString("name");
        this.displayName = json.getString("displayName");
        this.keywords = new ArrayList<>();
        JSONArray array = json.getJSONArray("keywords");
        for (int i = 0; i < array.length(); ++i) {
            this.keywords.add(array.getString(i));
        }
        this.instruction = json.getString("instruction");
        this.explainPrefix = json.getString("explainPrefix");
        this.noResults = json.getString("noResults");
        this.questionableResults = json.getString("questionableResults");
        this.sectionList = new ArrayList<>();
        array = json.getJSONArray("sections");
        for (int i = 0; i < array.length(); ++i) {
            this.sectionList.add(new GuidanceSection(path, array.getJSONObject(i)));
        }
    }

    public String getName() {
        return this.name;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public List<String> getKeywords() {
        return this.keywords;
    }

    public String getMainKeyword() {
        return this.keywords.get(0);
    }

    public String getInstruction() {
        return this.instruction;
    }

    public String getExplainPrefix() {
        return this.explainPrefix;
    }

    public void setListener(GuideListener listener) {
        this.listener = listener;
    }

    public Question getQuestion(String sn) {
        for (GuidanceSection section : this.sectionList) {
            Question question = section.getQuestion(sn);
            if (null != question) {
                return question;
            }
        }
        return null;
    }

    /**
     * 设置答案。
     *
     * @param sn
     * @param answerCode
     */
    public void setQuestionAnswer(String sn, String answerCode) {
        Question question = getQuestion(sn);
        if (null == question) {
            Logger.w(this.getClass(), "#setQuestionAnswer - Can NOT find question: " + sn);
            return;
        }

        for (Answer answer : question.answers) {
            if (answer.code.equalsIgnoreCase(answerCode)) {
                question.setAnswer(answer);
                break;
            }
        }
    }

    public int hitKeywords(List<String> list) {
        int hit = 0;
        for (String keyword : this.keywords) {
            if (list.contains(keyword)) {
                ++hit;
            }
        }
        return hit;
    }

    public boolean hasCompleted() {
        int completed = 0;
        for (GuidanceSection section : this.sectionList) {
            if (section.hasTerminated()) {
                // 已终止
                return true;
            }

            if (section.hasCompleted()) {
                ++completed;
            }
        }
        return (completed == this.sectionList.size());
    }

    public abstract void stop();

    public abstract AIGCStateCode input(String currentQuestionAnswer, Answer candidate);

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("path", this.path);
        json.put("name", this.name);
        json.put("displayName", this.displayName);
        JSONArray array = new JSONArray();
        for (String keyword : this.keywords) {
            array.put(keyword);
        }
        json.put("keywords", array);
        json.put("instruction", this.instruction);
        json.put("explainPrefix", this.explainPrefix);
        json.put("noResults", this.noResults);
        json.put("questionableResults", this.questionableResults);
        array = new JSONArray();
        for (GuidanceSection section : this.sectionList) {
            array.put(section.toJSON());
        }
        json.put("sections", array);
        return json;
    }
}
