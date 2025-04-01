/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology.composition;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Question {

    /**
     * 单选。
     */
    public final static String CHOICE_SINGLE = "single";

    /**
     * 多选。
     */
    public final static String CHOICE_MULTIPLE = "multiple";

    /**
     * 叙述。
     */
    public final static String CHOICE_DESCRIPTIVE = "descriptive";

    public final int sn;

    public final boolean hidden;

    public final String content;

    public final String prompt;

    public final String inference;

    public final List<Answer> answers = new ArrayList<>();

    public final String choice;

    public String questionContent;

    public String answerContent;

    public String inferenceResult;

    public Question(JSONObject structure) {
        this.sn = structure.getInt("sn");
        this.hidden = structure.has("hidden") && structure.getBoolean("hidden");
        this.content = structure.getString("content");
        this.prompt = structure.has("prompt") ? structure.getString("prompt") : "";
        this.inference = structure.has("inference") ? structure.getString("inference") : "";
        this.inferenceResult = structure.has("inferenceResult") ? structure.getString("inferenceResult") : "";
        this.choice = structure.getString("choice");
        this.questionContent = structure.has("questionContent") ? structure.getString("questionContent") : this.content;
        this.answerContent = structure.has("answerContent") ? structure.getString("answerContent") : "";
        JSONArray array = structure.getJSONArray("answers");
        for (int i = 0; i < array.length(); ++i) {
            Answer answer = new Answer(this.sn, array.getJSONObject(i));
            this.answers.add(answer);
        }
    }

    public boolean isSingleChoice() {
        return this.choice.equalsIgnoreCase(CHOICE_SINGLE);
    }

    public boolean isMultipleChoice() {
        return this.choice.equalsIgnoreCase(CHOICE_MULTIPLE);
    }

    public boolean isDescriptive() {
        return this.choice.equalsIgnoreCase(CHOICE_DESCRIPTIVE);
    }

    public void submitAnswerContent(String content) {
        this.answerContent = content;
    }

    public String getAnswerContent() {
        return this.answerContent;
    }

    public String makeInferencePrompt() {
        return this.inference.replace("${content}", this.answerContent);
    }

    public void setInferenceResult(String result) {
        this.inferenceResult = result;
    }

    public void chooseAnswer(String code) {
        if (this.isSingleChoice()) {
            for (Answer answer : this.answers) {
                if (answer.code.equalsIgnoreCase(code)) {
                    answer.chosen = true;
                }
                else {
                    answer.chosen = false;
                }
            }
        }
        else {
            for (Answer answer : this.answers) {
                if (answer.code.equalsIgnoreCase(code)) {
                    answer.chosen = !answer.chosen;
                    break;
                }
            }
        }
    }

    public Answer getChosenAnswer() {
        for (Answer answer : this.answers) {
            if (answer.chosen) {
                return answer;
            }
        }
        return null;
    }

    /**
     * 是否已答题。
     *
     * @return
     */
    public boolean hasAnswered() {
        for (Answer answer : this.answers) {
            if (answer.chosen) {
                return true;
            }
        }

        if (this.answerContent.length() > 0) {
            return true;
        }

        return false;
    }

    /**
     * 是否已完成答案选择。
     *
     * @return
     */
    public boolean hasChosen() {
        for (Answer answer : this.answers) {
            if (answer.chosen) {
                return true;
            }
        }
        return false;
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("sn", this.sn);
        json.put("hidden", this.hidden);
        json.put("content", this.content);
        json.put("prompt", this.prompt);
        json.put("inference", this.inference);
        json.put("choice", this.choice);
        json.put("questionContent", this.questionContent);
        json.put("answerContent", this.answerContent);
        json.put("inferenceResult", this.inferenceResult);

        JSONArray array = new JSONArray();
        for (Answer answer : this.answers) {
            array.put(answer.toJSON());
        }
        json.put("answers", array);

        return json;
    }
}
