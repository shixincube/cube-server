/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.guidance;

import cell.util.log.Logger;
import cube.common.state.AIGCStateCode;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public abstract class GuideFlowable {

    protected String name;

    protected String displayName;

    protected String instruction;

    protected List<GuidanceSection> sectionList;

    protected GuideListener listener;

    public GuideFlowable() {
        this.sectionList = new ArrayList<>();
    }

    public GuideFlowable(JSONObject json) {
        this.name = json.getString("name");
        this.displayName = json.getString("displayName");
        this.instruction = json.getString("instruction");
        this.sectionList = new ArrayList<>();
        JSONArray array = json.getJSONArray("sections");
        for (int i = 0; i < array.length(); ++i) {
            this.sectionList.add(new GuidanceSection(array.getJSONObject(i)));
        }
    }

    public String getName() {
        return this.name;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public String getInstruction() {
        return this.instruction;
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

    public abstract Question getCurrentQuestion();

    public abstract AIGCStateCode input(String currentQuestionAnswer, Answer candidate);

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("name", this.name);
        json.put("displayName", this.displayName);
        json.put("instruction", this.instruction);
        JSONArray array = new JSONArray();
        for (GuidanceSection section : this.sectionList) {
            array.put(section.toJSON());
        }
        json.put("sections", array);
        return json;
    }
}
