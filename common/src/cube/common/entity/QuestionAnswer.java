/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 Ambrose Xu.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
