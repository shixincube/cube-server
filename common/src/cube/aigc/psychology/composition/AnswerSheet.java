/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology.composition;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 问卷答题卡。
 */
public class AnswerSheet {

    public final long scaleSn;

    public final List<Answer> answers;

    public AnswerSheet(long scaleSn) {
        this.scaleSn = scaleSn;
        this.answers = new ArrayList<>();
    }

    public AnswerSheet(JSONObject json) {
        this.scaleSn = json.getLong("sn");
        this.answers = new ArrayList<>();
        JSONArray array = json.getJSONArray("answers");
        for (int i = 0; i < array.length(); ++i) {
            Answer answer = new Answer(array.getJSONObject(i));
            this.answers.add(answer);
        }

        Collections.sort(this.answers, new Comparator<Answer>() {
            @Override
            public int compare(Answer a1, Answer a2) {
                return a1.sn - a2.sn;
            }
        });
    }

    public int numAnswers() {
        return this.answers.size();
    }

    public void submit(int sn, String choice) {
        this.answers.add(new Answer(sn, choice));

        Collections.sort(this.answers, new Comparator<Answer>() {
            @Override
            public int compare(Answer a1, Answer a2) {
                return a1.sn - a2.sn;
            }
        });
    }

    public List<Answer> getAnswers() {
        return this.answers;
    }

    public boolean isValid() {
        if (this.answers.isEmpty()) {
            return false;
        }

        HashMap<String, AtomicInteger> countMap = new HashMap<>();
        for (Answer answer : this.answers) {
            AtomicInteger value = countMap.get(answer.choice);
            if (null == value) {
                value = new AtomicInteger(0);
                countMap.put(answer.choice, value);
            }
            value.incrementAndGet();
        }

        if (countMap.size() == 1) {
            // 所有答案都是同一个选项
            return false;
        }

        return true;
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("sn", this.scaleSn);

        JSONArray array = new JSONArray();
        for (Answer answer : this.answers) {
            array.put(answer.toJSON());
        }
        json.put("answers", array);
        return json;
    }

    public class Answer {

        public final int sn;

        public final String choice;

        public Answer(int sn, String choice) {
            this.sn = sn;
            this.choice = choice;
        }

        public Answer(JSONObject json) {
            this.sn = json.getInt("sn");
            this.choice = json.getString("choice");
        }

        public JSONObject toJSON() {
            JSONObject json = new JSONObject();
            json.put("sn", this.sn);
            json.put("choice", this.choice);
            return json;
        }
    }
}
