/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
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
            this.score = Double.parseDouble(json.get("score").toString());
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
