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

public class Question {

    public final int sn;

    public final String content;

    public final List<Answer> answers = new ArrayList<>();

    public final String choice;

    public Question(JSONObject structure) {
        this.sn = structure.getInt("sn");
        this.content = structure.getString("content");
        this.choice = structure.getString("choice");
        JSONArray array = structure.getJSONArray("answers");
        for (int i = 0; i < array.length(); ++i) {
            Answer answer = new Answer(this.sn, array.getJSONObject(i));
            this.answers.add(answer);
        }
    }

    public boolean isSingleChoice() {
        return this.choice.equalsIgnoreCase("single");
    }

    public void chooseAnswer(String code) {
        if (this.isSingleChoice()) {
            for (Answer answer : this.answers) {
                if (answer.code.equals(code)) {
                    answer.chosen = true;
                }
                else {
                    answer.chosen = false;
                }
            }
        }
        else {
            for (Answer answer : this.answers) {
                if (answer.code.equals(code)) {
                    answer.chosen = !answer.chosen;
                    break;
                }
            }
        }
    }

    public Answer getChosenAnswer() {
        for (Answer answer : this.answers) {
            if (answer.chosen) {
                return answer;
            }
        }
        return null;
    }

    public boolean hasAnswered() {
        for (Answer answer : this.answers) {
            if (answer.chosen) {
                return true;
            }
        }
        return false;
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("sn", this.sn);
        json.put("content", this.content);
        json.put("choice", this.choice);

        JSONArray array = new JSONArray();
        for (Answer answer : this.answers) {
            array.put(answer.toJSON());
        }
        json.put("answers", array);

        return json;
    }
}
