/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 Cube Team.
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

package cube.aigc;

import java.util.ArrayList;
import java.util.List;

/**
 * 提示词链。
 */
public class PromptChaining {

    private String system;

    private List<Prompt> promptList;

    public PromptChaining() {
        this.promptList = new ArrayList<>();
    }

    public PromptChaining(String system) {
        this.system = system;
        this.promptList = new ArrayList<>();
    }

    public void setSystemMetadata(String system) {
        this.system = system;
    }

    public String getSystemMetadata() {
        if (null == this.system) {
            return "你是智能AI助手。";
        }

        return this.system;
    }

    public List<Prompt> getPrompts() {
        return this.promptList;
    }

    public boolean addPrompt(Prompt prompt) {
        if (null == prompt.query || null == prompt.answer) {
            return false;
        }

        return this.promptList.add(prompt);
    }

    public boolean removePrompt(Prompt prompt) {
        return this.promptList.remove(prompt);
    }

    public boolean addPrompts(List<Prompt> prompts) {
        return this.promptList.addAll(prompts);
    }

    public int getWordNum() {
        int num = 0;
        for (Prompt prompt : this.promptList) {
            if (null != prompt.query) {
                num += prompt.query.length();
            }
            if (null != prompt.answer) {
                num += prompt.answer.length();
            }
        }
        return num;
    }
}
