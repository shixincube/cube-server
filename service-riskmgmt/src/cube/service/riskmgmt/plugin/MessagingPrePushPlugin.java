/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.riskmgmt.plugin;

import cube.common.entity.Message;
import cube.common.state.MessagingStateCode;
import cube.plugin.HookResult;
import cube.plugin.Plugin;
import cube.plugin.PluginContext;
import cube.service.messaging.MessagingPluginContext;
import cube.service.riskmgmt.RiskManagement;
import org.json.JSONObject;

/**
 * 消息模块内容分析插件。
 */
public class MessagingPrePushPlugin implements Plugin {

    private RiskManagement riskManagement;

    public MessagingPrePushPlugin(RiskManagement riskManagement) {
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
            JSONObject json = message.getPayload();
            if (json.has("type")) {
                String type = json.getString("type");
                if (type.equalsIgnoreCase("text") || type.equalsIgnoreCase("hypertext")) {
                    String content = json.getString("content");
                    if (this.riskManagement.hasSensitiveWord(message.getDomain().getName(), content)) {
                        // 包含敏感词，不允许发送
                        ctx.setStateCode(MessagingStateCode.SensitiveData);
                    }
                }
            }
        }
        return null;
    }
}
