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

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class RetrieveReRankResult extends Entity {

    private String query;

    private List<Answer> answerList;

    public RetrieveReRankResult(JSONObject json) {
        super(json);
        this.query = json.getString("query");
        this.answerList = new ArrayList<>();
        JSONArray list = json.getJSONArray("list");
        for (int i = 0; i < list.length(); ++i) {
            this.answerList.add(new Answer(list.getJSONObject(i)));
        }
    }

    public String getQuery() {
        return this.query;
    }

    public List<Answer> getAnswerList() {
        return this.answerList;
    }

    public boolean hasAnswer() {
        return !this.answerList.isEmpty();
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("query", this.query);
        JSONArray list = new JSONArray();
        for (Answer answer : this.answerList) {
            list.put(answer.toJSON());
        }
        json.put("list", list);
        return json;
    }


    public class Answer {

        public long id;

        public String content;

        public double score;

        public Answer(JSONObject json) {
            this.id = json.getLong("id");
            this.content = json.getString("content");
            this.score = json.getDouble("score");
        }

        public JSONObject toJSON() {
            JSONObject json = new JSONObject();
            json.put("id", this.id);
            json.put("content", this.content);
            json.put("score", this.score);
            return json;
        }
    }
}
