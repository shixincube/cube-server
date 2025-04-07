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

    public final Precondition precondition;

    public final String prefix;

    public final String question;

    public final boolean original;

    public final List<Answer> answers = new ArrayList<>();

    public final Router router;

    private String answerContent;

    private Answer answer;

    public Question(JSONObject json) {
        this.sn = json.getString("sn");
        this.precondition = json.has("precondition") ? new Precondition(json.getJSONObject("precondition")): null;
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

    public Answer getAnswer(String code) {
        for (Answer answer : this.answers) {
            if (answer.code.equalsIgnoreCase(code)) {
                return answer;
            }
        }
        return null;
    }

    public void setAnswerContent(String content) {
        this.answerContent = content;
    }

    public void setAnswer(Answer answer) {
        this.answer = answer;
    }

    public String getAnswerContent() {
        return this.answerContent;
    }

    public Answer getAnswer() {
        return this.answer;
    }

    public List<Answer> getGroupAnswers(String group) {
        for (Answer answer : this.answers) {
            if (answer.isGroup()) {
                if (answer.group.equalsIgnoreCase(group)) {
                    return answer.groupAnswers;
                }
            }
        }
        return null;
    }

    public String getGroupContent(String group) {
        for (Answer answer : this.answers) {
            if (answer.isGroup() && answer.group.equalsIgnoreCase(group)) {
                return answer.content;
            }
        }
        return null;
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("sn", this.sn);
        if (null != this.precondition) {
            json.put("precondition", this.precondition.toJSON());
        }
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

    public class Precondition {

        public final List<String> items;

        public final String condition;

        public Precondition(JSONObject json) {
            this.items = new ArrayList<>();
            JSONArray array = json.getJSONArray("items");
            for (int i = 0; i < array.length(); ++i) {
                this.items.add(array.getString(i));
            }
            this.condition = json.getString("condition");
        }

        public JSONObject toJSON() {
            JSONObject json = new JSONObject();
            JSONArray array = new JSONArray();
            for (String item : this.items) {
                array.put(item);
            }
            json.put("items", array);
            json.put("condition", this.condition);
            return json;
        }
    }
}
