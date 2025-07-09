/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.plugin;

import cube.aigc.AppEvent;
import cube.plugin.HookResult;
import cube.plugin.Plugin;
import cube.plugin.PluginContext;
import cube.service.aigc.AIGCPluginContext;
import cube.service.aigc.AIGCService;

public class AppEventPlugin implements Plugin {

    private final AIGCService service;

    public AppEventPlugin(AIGCService service) {
        this.service = service;
    }

    @Override
    public void setup() {

    }

    @Override
    public void teardown() {

    }

    @Override
    public HookResult launch(PluginContext context) {
        AppEvent event = ((AIGCPluginContext) context).getAppEvent();
        return null;
    }
}
