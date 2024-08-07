/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 Ambrose Xu.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
