/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
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
        if (null == prompt.query && null == prompt.answer) {
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

    public PromptChaining copy() {
        PromptChaining copy = new PromptChaining(this.system);
        copy.promptList.addAll(this.promptList);
        return copy;
    }
}
