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

public class Question {

    public final String sn;

    public final String constraint;

    public final String prefix;

    public final String question;

    public final boolean original;

    public final List<Answer> answers = new ArrayList<>();

    public final Router router;

    private String answerContent;

    public Question(JSONObject json) {
        this.sn = json.getString("sn");
        this.constraint = json.has("constraint") ? json.getString("constraint") : "";
        this.prefix = json.has("prefix") ? json.getString("prefix") : "";
        this.question = json.getString("question");
        this.original = json.has("original") && json.getBoolean("original");
        JSONArray array = json.getJSONArray("answers");
        for (int i = 0; i < array.length(); ++i) {
            this.answers.add(new Answer(array.getJSONObject(i)));
        }
        if (json.has("router")) {
            this.router = new Router(json.getJSONObject("router"));
        }
        else {
            this.router = null;
        }
    }

    public void setAnswerContent(String content) {
        this.answerContent = content;
    }

    public String getAnswerContent() {
        return this.answerContent;
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("sn", this.sn);
        json.put("constraint", this.constraint);
        json.put("prefix", this.prefix);
        json.put("question", this.question);
        json.put("original", this.original);

        JSONArray array = new JSONArray();
        for (Answer answer : this.answers) {
            array.put(answer.toJSON());
        }
        json.put("answers", array);

        if (null != this.router) {
            json.put("router", this.router.toJSON());
        }

        return json;
    }
}
