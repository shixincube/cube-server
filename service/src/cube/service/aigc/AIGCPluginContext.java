/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc;

import cube.aigc.AppEvent;
import cube.common.entity.KnowledgeDocument;
import cube.plugin.PluginContext;
import cube.service.aigc.knowledge.KnowledgeBase;

/**
 * AIGC 插件上下文。
 */
public class AIGCPluginContext extends PluginContext {

    private KnowledgeBase knowledgeBase;

    private KnowledgeDocument knowledgeDocument;

    private AppEvent appEvent;

    public AIGCPluginContext(KnowledgeBase knowledgeBase, KnowledgeDocument knowledgeDocument) {
        this.knowledgeBase = knowledgeBase;
        this.knowledgeDocument = knowledgeDocument;
    }

    public AIGCPluginContext(AppEvent appEvent) {
        this.appEvent = appEvent;
    }

    public void setKnowledgeBase(KnowledgeBase knowledgeBase) {
        this.knowledgeBase = knowledgeBase;
    }

    public KnowledgeBase getKnowledgeBase() {
        return this.knowledgeBase;
    }

    public KnowledgeDocument getKnowledgeDoc() {
        return this.knowledgeDocument;
    }

    public AppEvent getAppEvent() {
        return this.appEvent;
    }

    @Override
    public Object get(String name) {
        if (name.equalsIgnoreCase("appEvent")) {
            return this.appEvent;
        }
        else if (name.equalsIgnoreCase("knowledgeBase")) {
            return this.knowledgeBase;
        }
        else if (name.equalsIgnoreCase("knowledgeDoc")) {
            return this.knowledgeDocument;
        }

        return null;
    }

    @Override
    public void set(String name, Object value) {
    }
}
