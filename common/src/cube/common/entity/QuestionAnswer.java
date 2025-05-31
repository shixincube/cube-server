/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

import cube.util.JSONUtils;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 问答对。
 */
public class QuestionAnswer extends Entity {

    private List<String> questions;

    private List<String> answers;

    private double score;

    public QuestionAnswer(String question) {
        this.questions = new ArrayList<>();
        this.questions.add(question);
        this.answers = new ArrayList<>();
    }

    public QuestionAnswer(JSONObject json) {
        super(json);
        this.questions = JSONUtils.toStringList(json.getJSONArray("questions"));
        this.answers = JSONUtils.toStringList(json.getJSONArray("answers"));
        this.score = Double.parseDouble(json.get("score").toString());
    }

    public List<String> getQuestions() {
        return this.questions;
    }

    public List<String> getAnswers() {
        return this.answers;
    }

    public boolean hasAnswer() {
        return (!this.answers.isEmpty());
    }

    public double getScore() {
        return this.score;
    }

    public void addAnswers(String answer, double score) {
        if (!this.answers.contains(answer)) {
            this.answers.add(answer);
        }
        this.score = score;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("questions", JSONUtils.toStringArray(this.questions));
        json.put("answers", JSONUtils.toStringArray(this.answers));
        json.put("score", this.score);
        return json;
    }
}
