/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.contact.plugin;

import cube.plugin.HookResult;
import cube.plugin.Plugin;
import cube.plugin.PluginContext;

/**
 * 过滤联系人名插件。
 */
public class FilterContactNamePlugin implements Plugin {

    public FilterContactNamePlugin() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setup() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void teardown() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HookResult launch(PluginContext context) {
//        ContactPluginContext ctx = (ContactPluginContext) context;
//        Contact contact = ctx.getContact();
//        String name = contact.getName();
//        name = name.replace("时信", "**");
//        contact.setName(name);
        return null;
    }
}
