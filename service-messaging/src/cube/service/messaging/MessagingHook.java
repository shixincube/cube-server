/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.messaging;

import cube.plugin.Hook;

/**
 * 消息模块钩子。
 */
public class MessagingHook extends Hook {

    public final static String PrePush = "PrePush";

    public final static String PostPush = "PostPush";

    public final static String Notify = "Notify";

    public final static String SendMessage = "SendMessage";

    public final static String ForwardMessage = "ForwardMessage";

    public final static String WriteMessage = "WriteMessage";

    public final static String UpdateMessage = "UpdateMessage";

    public final static String DeleteMessage = "DeleteMessage";

    public final static String BurnMessage = "BurnMessage";

    public MessagingHook(String key) {
        super(key);
    }
}
