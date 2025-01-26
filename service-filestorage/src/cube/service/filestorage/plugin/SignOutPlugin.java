/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.filestorage.plugin;

import cube.plugin.HookResult;
import cube.plugin.Plugin;
import cube.plugin.PluginContext;
import cube.service.contact.ContactPluginContext;
import cube.service.filestorage.FileStorageService;

/**
 * 联系人登出或离线。
 */
public class SignOutPlugin implements Plugin {

    private FileStorageService service;

    public SignOutPlugin(FileStorageService service) {
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
        if (context instanceof ContactPluginContext) {
            ContactPluginContext ctx = (ContactPluginContext) context;
            this.service.getDaemonTask().removeManagedContact(ctx.getContact(), ctx.getDevice());
        }
        return null;
    }
}
