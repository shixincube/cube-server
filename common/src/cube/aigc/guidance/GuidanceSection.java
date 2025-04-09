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

    public final String evaluation;

    private List<Question> questionList;

    public GuidanceSection(JSONObject json) {
        this.evaluation = json.getString("evaluation");

        this.questionList = new ArrayList<>();
        JSONArray array = json.getJSONArray("questions");
        for (int i = 0; i < array.length(); ++i) {
            this.questionList.add(new Question(array.getJSONObject(i)));
        }
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

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("evaluation", this.evaluation);

        JSONArray array = new JSONArray();
        for (Question question : this.questionList) {
            array.put(question.toJSON());
        }
        json.put("questions", array);
        return json;
    }
}
