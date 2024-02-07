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

package cube.aigc.psychology;

import cube.aigc.PromptBuilder;
import cube.aigc.PromptChaining;
import cube.common.entity.AIGCGenerationRecord;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Workflow {

    /**
     * 所有关键词逐一的描述。
     */
    private LinkedList<PromptGroup> phase1Groups = new LinkedList<>();

    /**
     * 所有关键词逐一的建议。
     */
    private LinkedList<PromptGroup> phase2Groups = new LinkedList<>();

    public Workflow() {
    }

    public void addPhase1(AIGCGenerationRecord record, PromptChaining chaining) {
        this.phase1Groups.add(new PromptGroup(record, chaining));
    }

    public List<PromptGroup> getPhase1Groups() {
        return this.phase1Groups;
    }

    public void fillPhase1Result(int index, String result) {
        if (index >= this.phase1Groups.size()) {
            return;
        }

        this.phase1Groups.get(index).result = result;
    }

    public List<String> getPhase1Results() {
        List<String> list = new ArrayList<>();
        for (PromptGroup group : this.phase1Groups) {
            list.add(group.result);
        }
        return list;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        PromptBuilder builder = new PromptBuilder();
        for (PromptGroup group : this.phase1Groups) {
            buf.append("[R] user: ").append(group.record.query).append('\n');
            buf.append("[R] assistant: ").append(group.record.answer).append('\n');
            buf.append("[Prompt]\n");
            buf.append(builder.serializePromptChaining(group.chaining));
            buf.append("--------------------------------------------------------------------------------\n");
        }
        return buf.toString();
    }

    public class PromptGroup {

        public AIGCGenerationRecord record;

        public PromptChaining chaining;

        public String result;

        private PromptGroup(AIGCGenerationRecord record, PromptChaining chaining) {
            this.record = record;
            this.chaining = chaining;
            this.result = "";
        }
    }
}
