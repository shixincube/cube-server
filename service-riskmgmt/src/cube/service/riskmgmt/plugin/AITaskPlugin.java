/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.service.riskmgmt.plugin;

import cell.util.log.Logger;
import cube.aigc.TaskDescriptor;
import cube.common.entity.AIGCUnit;
import cube.plugin.HookResult;
import cube.plugin.Plugin;
import cube.plugin.PluginContext;
import cube.service.aigc.AIGCPluginContext;
import cube.service.riskmgmt.RiskManagement;

public class AITaskPlugin implements Plugin {

    private final RiskManagement service;

    public AITaskPlugin(RiskManagement service) {
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
        AIGCPluginContext ctx = (AIGCPluginContext) context;
        TaskDescriptor descriptor = ctx.makeTaskDescriptor();
        if (null != descriptor) {
            this.service.getStorage().writeTaskDescriptor(descriptor);
        }
        else {
            Logger.w(this.getClass(), "#launch - The task descriptor is NULL");
        }
        return null;
    }
}
