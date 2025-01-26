/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.plugin;

import cell.util.log.Logger;
import cube.auth.AuthConsts;
import cube.common.entity.Contact;
import cube.common.entity.KnowledgeProfile;
import cube.common.entity.KnowledgeScope;
import cube.plugin.HookResult;
import cube.plugin.Plugin;
import cube.plugin.PluginContext;
import cube.service.aigc.AIGCService;
import cube.service.contact.ContactPluginContext;

/**
 * 当创建新联系人时激活知识库。
 */
public class ActivateKnowledgeBasePlugin implements Plugin {

    private final AIGCService service;

    public ActivateKnowledgeBasePlugin(AIGCService service) {
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
            ContactPluginContext ctx = (ContactPluginContext) context;
            Contact contact = ctx.getContact();

            Logger.i(ActivateKnowledgeBasePlugin.class, "Activate knowledge base: " + contact.getId());

            // 更新
            this.service.getStorage().updateKnowledgeProfile(contact.getId(), AuthConsts.DEFAULT_DOMAIN,
                    KnowledgeProfile.STATE_NORMAL, 549755813888L, KnowledgeScope.Private);
        } catch (Exception e) {
            Logger.e(this.getClass(), "#launch", e);
        }

        return null;
    }
}
