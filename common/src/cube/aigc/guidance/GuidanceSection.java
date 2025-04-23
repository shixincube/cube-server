/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.guidance;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class GuidanceSection {

    public final String name;

    private final List<Question> questionList;

    public final String evaluation;

    public long startTimestamp;

    public long endTimestamp;

    public GuidanceSection(String path, JSONObject json) {
        this.name = json.getString("name");
        this.questionList = new ArrayList<>();
        JSONArray array = json.getJSONArray("questions");
        for (int i = 0; i < array.length(); ++i) {
            this.questionList.add(new Question(path, array.getJSONObject(i)));
        }
        this.evaluation = json.getString("evaluation");
    }

    public List<Question> getAllQuestions() {
        return this.questionList;
    }

    public Question getQuestion(int index) {
        return this.questionList.get(index);
    }

    public Question getQuestion(String sn) {
        for (Question question : this.questionList) {
            if (question.sn.equals(sn)) {
                return question;
            }
        }
        return null;
    }

    public boolean hasCompleted() {
        int count = 0;
        for (Question question : this.questionList) {
            if (question.isAnswerGroup() && question.hasAnswerGroupCompleted()) {
                ++count;
            }
            else if (question.hasAnswer()) {
                ++count;
            }
        }
        return (count == this.questionList.size());
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("name", this.name);

        JSONArray array = new JSONArray();
        for (Question question : this.questionList) {
            array.put(question.toJSON());
        }
        json.put("questions", array);

        json.put("evaluation", this.evaluation);

        return json;
    }
}
