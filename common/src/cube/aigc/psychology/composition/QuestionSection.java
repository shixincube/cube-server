/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology.composition;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class QuestionSection {

    public final String title;

    public final String content;

    private final List<Question> questions;

    public QuestionSection(JSONObject structure) {
        this.title = structure.getString("title");
        this.content = structure.getString("content");
        this.questions = new ArrayList<>();
        JSONArray array = structure.getJSONArray("questions");
        for (int i = 0; i < array.length(); ++i) {
            this.questions.add(new Question(array.getJSONObject(i)));
        }
    }

    public List<Question> getQuestions() {
        List<Question> list = new ArrayList<>();
        for (Question question : this.questions) {
            if (question.hidden) {
                continue;
            }
            list.add(question);
        }
        return list;
    }

    public int numQuestions() {
        int num = 0;
        for (Question question : this.questions) {
            if (question.hidden) {
                continue;
            }
            ++num;
        }
        return num;
    }

    public List<Question> getHiddenQuestions() {
        List<Question> list = new ArrayList<>();
        for (Question question : this.questions) {
            if (question.hidden) {
                list.add(question);
            }
        }
        return list;
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("title", this.title);
        json.put("content", this.content);

        JSONArray array = new JSONArray();
        for (Question question : this.questions) {
            array.put(question.toJSON());
        }
        json.put("questions", array);

        return json;
    }
}
