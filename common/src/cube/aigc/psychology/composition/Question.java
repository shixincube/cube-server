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

package cube.aigc.psychology.composition;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Question {

    public final int sn;

    public final String content;

    public final List<Answer> answers = new ArrayList<>();

    public final String choice;

    public Question(JSONObject structure) {
        this.sn = structure.getInt("sn");
        this.content = structure.getString("content");
        this.choice = structure.getString("choice");
        JSONArray array = structure.getJSONArray("answers");
        for (int i = 0; i < array.length(); ++i) {
            Answer answer = new Answer(this.sn, array.getJSONObject(i));
            this.answers.add(answer);
        }
    }

    public boolean isSingleChoice() {
        return this.choice.equalsIgnoreCase("single");
    }

    public void chooseAnswer(String code) {
        if (this.isSingleChoice()) {
            for (Answer answer : this.answers) {
                if (answer.code.equals(code)) {
                    answer.chosen = true;
                }
                else {
                    answer.chosen = false;
                }
            }
        }
        else {
            for (Answer answer : this.answers) {
                if (answer.code.equals(code)) {
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

    public boolean hasAnswered() {
        for (Answer answer : this.answers) {
            if (answer.chosen) {
                return true;
            }
        }
        return false;
    }
}
