/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.riskmgmt.plugin;

import cube.common.entity.AuthDomain;
import cube.plugin.HookResult;
import cube.plugin.Plugin;
import cube.plugin.PluginContext;
import cube.service.auth.AuthPluginContext;
import cube.service.riskmgmt.RiskManagement;

/**
 * 创建域应用插件。
 */
public class CreateDomainAppPlugin implements Plugin {

    private RiskManagement service;

    public CreateDomainAppPlugin(RiskManagement service) {
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
        if (context instanceof AuthPluginContext) {
            AuthPluginContext authPluginContext = (AuthPluginContext) context;
            AuthDomain authDomain = authPluginContext.getDomain();
            if (null != authDomain) {
                this.service.refreshDomain(authDomain);
            }
        }

        return null;
    }
}
