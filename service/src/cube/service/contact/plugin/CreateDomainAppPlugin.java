/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.contact.plugin;

import cube.common.entity.AuthDomain;
import cube.plugin.HookResult;
import cube.plugin.Plugin;
import cube.plugin.PluginContext;
import cube.service.auth.AuthPluginContext;
import cube.service.contact.ContactManager;

/**
 * 创建域应用插件。
 */
public class CreateDomainAppPlugin implements Plugin {

    public CreateDomainAppPlugin() {
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
            AuthPluginContext apc = (AuthPluginContext) context;
            AuthDomain authDomain = apc.getDomain();
            if (null != authDomain) {
                ContactManager.getInstance().refreshAuthDomain(authDomain);
            }
        }

        return null;
    }
}
