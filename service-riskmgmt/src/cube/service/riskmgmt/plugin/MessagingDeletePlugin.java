/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.riskmgmt.plugin;

import cube.common.entity.TraceEvent;
import cube.common.entity.FileAttachment;
import cube.common.entity.Message;
import cube.plugin.HookResult;
import cube.plugin.Plugin;
import cube.plugin.PluginContext;
import cube.service.messaging.MessagingPluginContext;
import cube.service.riskmgmt.RiskManagement;

/**
 * 消息删除插件。
 */
public class MessagingDeletePlugin implements Plugin {

    private RiskManagement riskManagement;

    public MessagingDeletePlugin(RiskManagement riskManagement) {
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
        MessagingPluginContext ctx = (MessagingPluginContext) context;
        Message message = ctx.getMessage();
        if (null != message) {
            FileAttachment fileAttachment = message.getAttachment();
            if (null != fileAttachment) {
                // 有文件附件的消息
                this.riskManagement.addFileChainNode(TraceEvent.Delete, message, ctx.getDevice());
            }
        }

        return null;
    }
}
