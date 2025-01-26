/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
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
