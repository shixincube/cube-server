/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.riskmgmt.plugin;

import cube.common.entity.ContactBehavior;
import cube.common.entity.ContactRisk;
import cube.plugin.HookResult;
import cube.plugin.HookResultKeys;
import cube.plugin.Plugin;
import cube.plugin.PluginContext;
import cube.service.contact.ContactHook;
import cube.service.contact.ContactPluginContext;
import cube.service.riskmgmt.RiskManagement;

/**
 * 联系人插件。
 */
public class ContactPlugin implements Plugin {

    private RiskManagement service;

    public ContactPlugin(RiskManagement service) {
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
        if (context instanceof ContactPluginContext) {
            ContactPluginContext ctx = (ContactPluginContext) context;
            String hook = ctx.getHookName();
            if (ContactHook.VerifyIdentity.equals(hook)) {
                int mask = this.service.getContactRiskMask(ctx.getContact().getDomain().getName(), ctx.getContact().getId());
                if (ContactRisk.hasForbiddenSignIn(mask)) {
                    // 联系人被禁止签入
                    HookResult result = new HookResult();
                    result.set(HookResultKeys.NOT_ALLOWED, true);
                    return result;
                }
            }
            else if (ContactHook.SignIn.equals(hook)) {
                ContactBehavior behavior = new ContactBehavior(ctx.getContact(), ContactBehavior.SIGN_IN);
                behavior.setDevice(ctx.getDevice());
                // 添加记录
                this.service.recordContactBehavior(behavior);
            }
            else if (ContactHook.DeviceTimeout.equals(hook)) {
                ContactBehavior behavior = new ContactBehavior(ctx.getContact(), ContactBehavior.SIGN_OUT);
                behavior.setDevice(ctx.getDevice());
                // 添加记录
                this.service.recordContactBehavior(behavior);
            }
            else if (ContactHook.Comeback.equals(hook)) {
                ContactBehavior behavior = new ContactBehavior(ctx.getContact(), ContactBehavior.SIGN_IN);
                behavior.setDevice(ctx.getDevice());
                // 添加记录
                this.service.recordContactBehavior(behavior);
            }
            else if (ContactHook.SignOut.equals(hook)) {
                ContactBehavior behavior = new ContactBehavior(ctx.getContact(), ContactBehavior.SIGN_OUT);
                behavior.setDevice(ctx.getDevice());
                // 添加记录
                this.service.recordContactBehavior(behavior);
            }
        }

        return null;
    }
}
