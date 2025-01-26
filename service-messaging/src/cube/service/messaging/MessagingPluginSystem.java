/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.messaging;

import cube.plugin.PluginSystem;

/**
 * 消息插件系统。
 */
public class MessagingPluginSystem extends PluginSystem<MessagingHook> {

    public MessagingPluginSystem() {
        this.build();
    }

    public MessagingHook getPrePushHook() {
        return this.getHook(MessagingHook.PrePush);
    }

    public MessagingHook getPostPushHook() {
        return this.getHook(MessagingHook.PostPush);
    }

    public MessagingHook getNotifyHook() {
        return this.getHook(MessagingHook.Notify);
    }

    public MessagingHook getSendMessageHook() {
        return this.getHook(MessagingHook.SendMessage);
    }

    public MessagingHook getForwardMessageHook() {
        return this.getHook(MessagingHook.ForwardMessage);
    }

    public MessagingHook getWriteMessageHook() {
        return this.getHook(MessagingHook.WriteMessage);
    }

    public MessagingHook getUpdateMessageHook() {
        return this.getHook(MessagingHook.UpdateMessage);
    }

    public MessagingHook getDeleteMessageHook() {
        return this.getHook(MessagingHook.DeleteMessage);
    }

    public MessagingHook getBurnMessageHook() {
        return this.getHook(MessagingHook.BurnMessage);
    }

    private void build() {
        MessagingHook hook = new MessagingHook(MessagingHook.PrePush);
        this.addHook(hook);

        hook = new MessagingHook(MessagingHook.PostPush);
        this.addHook(hook);

        hook = new MessagingHook(MessagingHook.Notify);
        this.addHook(hook);

        hook = new MessagingHook(MessagingHook.SendMessage);
        this.addHook(hook);

        hook = new MessagingHook(MessagingHook.ForwardMessage);
        this.addHook(hook);

        hook = new MessagingHook(MessagingHook.WriteMessage);
        this.addHook(hook);

        hook = new MessagingHook(MessagingHook.UpdateMessage);
        this.addHook(hook);

        hook = new MessagingHook(MessagingHook.DeleteMessage);
        this.addHook(hook);

        hook = new MessagingHook(MessagingHook.BurnMessage);
        this.addHook(hook);
    }
}
