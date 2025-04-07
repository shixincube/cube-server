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

public class Answer {

    public final String code;

    public final String content;

    public final String group;

    public final List<Answer> groupAnswers;

    public Answer(JSONObject json) {
        this.code = json.has("code") ? json.getString("code") : null;
        this.content = json.has("content") ? json.getString("content") : null;
        this.group = json.has("group") ? json.getString("group") : null;
        if (json.has("answers")) {
            this.groupAnswers = new ArrayList<>();
            JSONArray array = json.getJSONArray("answers");
            for (int i = 0; i < array.length(); ++i) {
                this.groupAnswers.add(new Answer(array.getJSONObject(i)));
            }
        }
        else {
            this.groupAnswers = null;
        }
    }

    public boolean isGroup() {
        return (null != this.group);
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        if (null != this.code) {
            json.put("code", this.code);
        }
        if (null != this.content) {
            json.put("content", this.content);
        }
        if (null != this.group) {
            json.put("group", this.group);
        }

        if (null != this.groupAnswers) {
            JSONArray array = new JSONArray();
            for (Answer answer : this.groupAnswers) {
                array.put(answer.toJSON());
            }
            json.put("answers", array);
        }
        return json;
    }
}
