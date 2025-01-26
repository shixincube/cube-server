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

/**
 * 问卷。
 */
public class Questionnaire {

    public String name;

    public String displayName;

    public String instruction;

    public List<QuestionSection> questionSections;

    protected String scoringScript;

    public Questionnaire() {
    }

    protected void build(JSONObject structure) {
        this.name = structure.getString("name");
        this.displayName = structure.getString("displayName");
        this.instruction = structure.getString("instruction");
        this.questionSections = new ArrayList<>();
        if (structure.has("sections")) {
            JSONArray array = structure.getJSONArray("sections");
            for (int i = 0; i < array.length(); ++i) {
                QuestionSection section = new QuestionSection(array.getJSONObject(i));
                this.questionSections.add(section);
            }
        }
        if (structure.has("scoringScript")) {
            this.scoringScript = structure.getString("scoringScript");
        }
    }

    public boolean chooseAnswer(int sn, String code) {
        Question question = this.getQuestion(sn);
        if (null == question) {
            return false;
        }

        if (question.isSingleChoice()) {
            for (Answer answer : question.answers) {
                if (answer.code.equals(code)) {
                    answer.chosen = true;
                }
                else {
                    answer.chosen = false;
                }
            }
        }
        else {
            for (Answer answer : question.answers) {
                if (answer.code.equals(code)) {
                    answer.chosen = !answer.chosen;
                    break;
                }
            }
        }

        return true;
    }

    public boolean isComplete() {
        int num = 0;
        int count = 0;
        for (QuestionSection section : this.questionSections) {
            for (Question question : section.questions) {
                num += 1;
                if (question.hasAnswered()) {
                    count += 1;
                }
            }
        }
        return num == count;
    }

    public List<Answer> getAllChosenAnswers() {
        List<Answer> answers = new ArrayList<>();
        for (QuestionSection section : this.questionSections) {
            for (Question question : section.questions) {
                Answer answer = question.getChosenAnswer();
                if (null == answer) {
                    continue;
                }
                answers.add(answer);
            }
        }
        return answers;
    }

    public Question getQuestion(int sn) {
        for (QuestionSection section : this.questionSections) {
            for (Question question : section.questions) {
                if (question.sn == sn) {
                    return question;
                }
            }
        }
        return null;
    }

    public List<Question> getQuestions() {
        List<Question> result = new ArrayList<>();
        for (QuestionSection section : this.questionSections) {
            for (Question question : section.questions) {
                result.add(question);
            }
        }
        return result;
    }

    public int numQuestions() {
        int num = 0;
        for (QuestionSection section : this.questionSections) {
            num += section.questions.size();
        }
        return num;
    }

    public String toMarkdown() {
        StringBuilder buf = new StringBuilder();
        buf.append("# ").append(this.name).append("\n\n");
        buf.append(this.instruction).append("\n\n");
        buf.append("\n");
        for (QuestionSection section : this.questionSections) {
            buf.append("## ").append(section.title).append("\n\n");
            buf.append(section.content).append("\n\n");

            for (Question question : section.questions) {
                if (question.content.length() > 0) {
                    buf.append("* ").append(question.sn).append("、").append(question.content).append("  \n");
                    for (Answer answer : question.answers) {
                        buf.append(answer.content).append("  \n");
                    }
                    buf.delete(buf.length() - 3, buf.length());
                    buf.append("\n");
                }
                else {
                    buf.append("* ").append(question.sn).append("、");
                    for (Answer answer : question.answers) {
                        buf.append(answer.content).append("    ");
                    }
                    buf.delete(buf.length() - 4, buf.length());
                    buf.append("\n");
                }

                buf.append("\n");
            }
            buf.append("\n");
        }
        return buf.toString();
    }
}
