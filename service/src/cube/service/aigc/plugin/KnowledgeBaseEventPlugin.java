/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.plugin;

import cell.util.log.Logger;
import cube.aigc.AppEvent;
import cube.plugin.HookResult;
import cube.plugin.Plugin;
import cube.plugin.PluginContext;
import cube.service.aigc.AIGCHook;
import cube.service.aigc.AIGCPluginContext;
import cube.service.aigc.AIGCService;

/**
 * 知识库事件插件。
 */
public class KnowledgeBaseEventPlugin implements Plugin {

    private final AIGCService service;

    public KnowledgeBaseEventPlugin(AIGCService service) {
        this.service = service;
    }

    @Override
    public void setup() {
        // Nothing
    }

    @Override
    public void teardown() {
        // Nothing
    }

    @Override
    public HookResult launch(PluginContext context) {
        try {
            AIGCPluginContext ctx = (AIGCPluginContext) context;

            if (AIGCHook.ImportKnowledgeDoc.equals(ctx.getKey())) {
                AppEvent appEvent = new AppEvent(AppEvent.ImportKnowledgeDoc, System.currentTimeMillis(),
                        ctx.getKnowledgeBase().getAuthToken().getContactId(),
                        ctx.getKnowledgeDoc().toJSON());
                this.service.getStorage().writeAppEvent(appEvent);
            }
            else if (AIGCHook.RemoveKnowledgeDoc.equals(ctx.getKey())) {
                AppEvent appEvent = new AppEvent(AppEvent.RemoveKnowledgeDoc, System.currentTimeMillis(),
                        ctx.getKnowledgeBase().getAuthToken().getContactId(),
                        ctx.getKnowledgeDoc().toJSON());
                this.service.getStorage().writeAppEvent(appEvent);
            }
        } catch (Exception e) {
            Logger.e(this.getClass(), "#launch", e);
        }

        return null;
    }
}
