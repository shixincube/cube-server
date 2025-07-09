/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc;

import cube.plugin.PluginSystem;

/**
 * AIGC 插件系统。
 */
public class AIGCPluginSystem extends PluginSystem<AIGCHook> {

    public AIGCPluginSystem() {
        this.build();
    }

    public AIGCHook getAppEventHook() {
        return this.getHook(AIGCHook.AppEvent);
    }

    public AIGCHook getImportKnowledgeDocHook() {
        return this.getHook(AIGCHook.ImportKnowledgeDoc);
    }

    public AIGCHook getRemoveKnowledgeDoc() {
        return this.getHook(AIGCHook.RemoveKnowledgeDoc);
    }

    private void build() {
        AIGCHook hook = new AIGCHook(AIGCHook.AppEvent);
        this.addHook(hook);

        hook = new AIGCHook(AIGCHook.ImportKnowledgeDoc);
        this.addHook(hook);

        hook = new AIGCHook(AIGCHook.RemoveKnowledgeDoc);
        this.addHook(hook);
    }
}
