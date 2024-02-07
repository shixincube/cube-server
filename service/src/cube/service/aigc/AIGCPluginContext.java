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

package cube.service.aigc;

import cube.common.entity.KnowledgeDoc;
import cube.plugin.PluginContext;
import cube.service.aigc.knowledge.KnowledgeBase;

/**
 * AIGC 插件上下文。
 */
public class AIGCPluginContext extends PluginContext {

    private KnowledgeBase knowledgeBase;

    private KnowledgeDoc knowledgeDoc;

    public AIGCPluginContext(KnowledgeBase knowledgeBase, KnowledgeDoc knowledgeDoc) {
        this.knowledgeBase = knowledgeBase;
        this.knowledgeDoc = knowledgeDoc;
    }

    public void setKnowledgeBase(KnowledgeBase knowledgeBase) {
        this.knowledgeBase = knowledgeBase;
    }

    public KnowledgeBase getKnowledgeBase() {
        return this.knowledgeBase;
    }

    public KnowledgeDoc getKnowledgeDoc() {
        return this.knowledgeDoc;
    }

    @Override
    public Object get(String name) {
        if (name.equalsIgnoreCase("knowledgeBase")) {
            return this.knowledgeBase;
        }
        else if (name.equalsIgnoreCase("knowledgeDoc")) {
            return this.knowledgeDoc;
        }

        return null;
    }

    @Override
    public void set(String name, Object value) {
    }
}
