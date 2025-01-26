/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.riskmgmt.plugin;

import cube.common.entity.Contact;
import cube.plugin.HookResult;
import cube.plugin.Plugin;
import cube.plugin.PluginContext;
import cube.service.contact.ContactPluginContext;
import cube.service.riskmgmt.RiskManagement;

/**
 * 修改联系人名称插件。
 */
public class ModifyContactNamePlugin implements Plugin {

    private RiskManagement riskManagement;

    public ModifyContactNamePlugin(RiskManagement riskManagement) {
        this.riskManagement = riskManagement;
    }

    @Override
    public void setup() {

    }

    @Override
    public void teardown() {

    }

    @Override
    public HookResult launch(PluginContext context) {
        ContactPluginContext contactContext = (ContactPluginContext) context;
        String newName = contactContext.getNewName();
        if (null == newName) {
            return null;
        }

        Contact contact = contactContext.getContact();
        if (this.riskManagement.hasSensitiveWord(contact.getDomain().getName(), newName)) {
            // 包含敏感词，不允许修改
            contactContext.setNewName(null);
        }

        return null;
    }
}
