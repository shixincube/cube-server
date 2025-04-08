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

public class AnswerGroup {

    public final static int STATE_NONE = 0;

    public final static int STATE_ANSWERING = 1;

    public final static int STATE_ANSWERED = 2;

    public int state = STATE_NONE;

    public final String group;

    public final String content;

    public final List<Answer> answers;

    public AnswerGroup(JSONObject json) {
        this.group = json.getString("group");
        this.content = json.getString("content");
        this.answers = new ArrayList<>();
        JSONArray array = json.getJSONArray("answers");
        for (int i = 0; i < array.length(); ++i) {
            this.answers.add(new Answer(array.getJSONObject(i)));
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

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("group", this.group);
        json.put("content", this.content);
        JSONArray array = new JSONArray();
        for (Answer answer : this.answers) {
            array.put(answer.toJSON());
        }
        json.put("answers", array);
        return json;
    }
}
