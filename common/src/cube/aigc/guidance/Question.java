/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.guidance;

import cell.util.log.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Question {

    private final String path;

    public final String sn;

    public final Precondition precondition;

    public final String prefix;

    public final String question;

    public final boolean original;

    public final String constraint;

    public final List<Answer> answers;

    public final List<AnswerGroup> answerGroups;

    public final Router router;

    private List<String> answerContents;

    private Answer answer;

    private List<AnswerGroup> enabledGroups;

    private Map<String, Answer> groupAnswerMap;

    public Question(String path, JSONObject json) {
        this.path = path;
        this.sn = json.getString("sn");
        this.precondition = json.has("precondition") ? new Precondition(json.getJSONObject("precondition")): null;
        this.prefix = json.has("prefix") ? json.getString("prefix") : "";
        this.question = json.getString("question").trim().startsWith("file://") ?
                readQuestionFromFile(json.getString("question").trim()) : json.getString("question").trim();
        this.original = json.has("original") && json.getBoolean("original");
        this.constraint = json.has("constraint") ? json.getString("constraint") : null;
        JSONArray array = json.getJSONArray("answers");
        if (array.getJSONObject(0).has("group")) {
            this.answerGroups = new ArrayList<>();
            this.answers = null;
            for (int i = 0; i < array.length(); ++i) {
                this.answerGroups.add(new AnswerGroup(array.getJSONObject(i)));
            }
        }
        else {
            this.answers = new ArrayList<>();
            this.answerGroups = null;
            for (int i = 0; i < array.length(); ++i) {
                this.answers.add(new Answer(array.getJSONObject(i)));
            }
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

    public void appendAnswerContent(String content) {
        if (null == this.answerContents) {
            this.answerContents = new ArrayList<>();
        }
        this.answerContents.add(content);
    }

    public void setAnswer(Answer answer) {
        this.answer = answer;
    }

    public Answer getAnswer() {
        return this.answer;
    }

    public boolean hasAnswer() {
        return (null != this.answer);
    }

    public List<AnswerGroup> enabledGroups(List<String> groups) {
        this.enabledGroups = new ArrayList<>();
        if (null == this.groupAnswerMap) {
            this.groupAnswerMap = new HashMap<>();
        }

        for (AnswerGroup answerGroup : this.answerGroups) {
            if (groups.contains(answerGroup.group)) {
                // 启用组
                this.enabledGroups.add(answerGroup);
            }
            else {
                // 未启用的使用默认答案
                answerGroup.state = AnswerGroup.STATE_ANSWERED;
                this.groupAnswerMap.put(answerGroup.group, answerGroup.getAnswer("false"));
            }
        }
        return this.enabledGroups;
    }

    public void setGroupAnswer(String group, Answer answer) {
        if (null == this.groupAnswerMap) {
            this.groupAnswerMap = new HashMap<>();
        }
        this.getAnswerGroup(group).state = AnswerGroup.STATE_ANSWERED;
        this.groupAnswerMap.put(group, answer);
    }

    public Answer getGroupAnswer(String group) {
        return this.groupAnswerMap.get(group);
    }

    public AnswerGroup getAnswerGroupByState(int state) {
        for (AnswerGroup answerGroup : this.enabledGroups) {
            if (answerGroup.state == state) {
                return answerGroup;
            }
        }
        return null;
    }

    public AnswerGroup getAnswerGroup(String group) {
        if (null == this.answerGroups) {
            return null;
        }
        for (AnswerGroup answerGroup : this.answerGroups) {
            if (answerGroup.group.equalsIgnoreCase(group)) {
                return answerGroup;
            }
        }
        return null;
    }

    public boolean isAnswerGroup() {
        return (null != this.answerGroups);
    }

    public boolean hasAnswerGroupCompleted() {
        if (null == this.answerGroups) {
            return false;
        }

        int count = 0;
        for (AnswerGroup answerGroup : this.answerGroups) {
            if (answerGroup.state == AnswerGroup.STATE_ANSWERED) {
                ++count;
            }
        }
        return count == this.answerGroups.size();
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
        if (null != this.constraint) {
            json.put("constraint", this.constraint);
        }

        JSONArray array = new JSONArray();
        if (null != this.answers) {
            for (Answer answer : this.answers) {
                array.put(answer.toJSON());
            }
        }
        else {
            for (AnswerGroup answerGroup : this.answerGroups) {
                array.put(answerGroup.toJSON());
            }
        }
        json.put("answers", array);

        if (null != this.router) {
            json.put("router", this.router.toJSON());
        }

        return json;
    }

    private String readQuestionFromFile(String fileLink) {
        String link = fileLink.replace("file://", "");
        Path filepath = Paths.get(this.path, link);
        try {
            byte[] data = Files.readAllBytes(filepath);
            return new String(data, StandardCharsets.UTF_8);
        } catch (Exception e) {
            Logger.e(this.getClass(), "#readQuestionFromFile", e);
            return null;
        }
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
