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

    private List<Prompt> promptList;

    public PromptChaining() {
        this.promptList = new ArrayList<>();
    }

    public List<Prompt> getPrompts() {
        return this.promptList;
    }

    public String getSystemMetadata() {
        return this.promptList.get(0).system;
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
}
