/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.plugin;

import cube.common.entity.Contact;
import cube.common.entity.KnowledgeBaseInfo;
import cube.common.entity.VerificationCode;
import cube.plugin.HookResult;
import cube.plugin.Plugin;
import cube.plugin.PluginContext;
import cube.service.aigc.AIGCService;
import cube.service.contact.ContactHook;
import cube.service.contact.ContactPluginContext;

import java.util.List;

public class ContactEventPlugin implements Plugin {

    private final AIGCService service;

    public ContactEventPlugin(AIGCService service) {
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
        ContactPluginContext ctx = (ContactPluginContext) context;
        String hook = ctx.getHookName();

        if (ContactHook.SignIn.equals(hook)) {
            Contact contact = ctx.getContact();
            List<KnowledgeBaseInfo> list = this.service.getKnowledgeFramework().getKnowledgeBaseInfos(contact.getId());
            if (null != list) {
                for (KnowledgeBaseInfo info : list) {
                    this.service.getKnowledgeFramework().getKnowledgeBase(contact.getId(), info.name);
                }
            }
        }
        else if (ContactHook.SignOut.equals(hook) || ContactHook.DeviceTimeout.equals(hook)) {
            Contact contact = ctx.getContact();
            this.service.getKnowledgeFramework().freeBase(contact.getDomain().getName(),
                    contact.getId());
        }
        else if (ContactHook.VerifyVerificationCode.equals(hook)) {
            if (ctx.hasParameter()) {
                VerificationCode verificationCode = (VerificationCode) ctx.getParameter();
                if (0 == ctx.getContact().numDevices()) {
                    // 如果没有设备，指定设备
                    ctx.getContact().addDevice(ctx.getDevice());
                }
                this.service.checkInUser(ctx.getContact(), verificationCode);
            }
        }

        return null;
    }
}
