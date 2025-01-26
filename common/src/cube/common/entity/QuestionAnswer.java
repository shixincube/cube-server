/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

import cube.util.JSONUtils;
import org.json.JSONObject;

import java.util.List;

/**
 * 问答对。
 */
public class QuestionAnswer extends Entity {

    private List<String> questions;

    private List<String> answers;

    private double score;

    public QuestionAnswer(JSONObject json) {
        super(json);
        this.questions = JSONUtils.toStringList(json.getJSONArray("questions"));
        this.answers = JSONUtils.toStringList(json.getJSONArray("answers"));
        this.score = json.getDouble("score");
    }

    public List<String> getQuestions() {
        return this.questions;
    }

    public List<String> getAnswers() {
        return this.answers;
    }

    public double getScore() {
        return this.score;
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
