/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.auth;

import cube.plugin.PluginSystem;

/**
 * Auth 服务插件系统。
 */
public class AuthServicePluginSystem extends PluginSystem<AuthServiceHook> {

    public AuthServicePluginSystem() {
        super();
        this.build();
    }

    public AuthServiceHook getCreateDomainAppHook() {
        return this.getHook(AuthServiceHook.CreateDomainApp);
    }

    public AuthServiceHook getInjectTokenHook() {
        return this.getHook(AuthServiceHook.InjectToken);
    }

    private void build() {
        AuthServiceHook hook = new AuthServiceHook(AuthServiceHook.CreateDomainApp);
        this.addHook(hook);

        hook = new AuthServiceHook(AuthServiceHook.InjectToken);
        this.addHook(hook);
    }
}
