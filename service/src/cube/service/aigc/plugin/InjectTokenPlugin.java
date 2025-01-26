/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.plugin;

import cell.util.log.Logger;
import cube.auth.AuthToken;
import cube.plugin.HookResult;
import cube.plugin.Plugin;
import cube.plugin.PluginContext;
import cube.service.aigc.AIGCService;
import cube.service.auth.AuthPluginContext;

/**
 * 注入令牌时处理插件。
 */
public class InjectTokenPlugin implements Plugin {

    private final AIGCService service;

    public InjectTokenPlugin(AIGCService service) {
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
            AuthPluginContext ctx = (AuthPluginContext) context;
            AuthToken token = ctx.getToken();
            this.service.newInvitationForToken(token.getCode());
            Logger.i(this.getClass(), "Insert invitation for token \"" + token.getCode() + "\"");
        } catch (Exception e) {
            Logger.e(this.getClass(), "#launch", e);
        }

        return null;
    }
}
