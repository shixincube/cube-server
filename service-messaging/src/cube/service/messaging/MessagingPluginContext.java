/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.messaging;

import cube.common.entity.Device;
import cube.common.entity.Message;
import cube.common.state.MessagingStateCode;
import cube.plugin.PluginContext;

/**
 * 消息插件上下文。
 */
public class MessagingPluginContext extends PluginContext {

    public final static String MESSAGE = "message";

    public final static String STATE = "state";

    private Message message;

    private Device device;

    private MessagingStateCode stateCode;

    public MessagingPluginContext(Message message, Device device) {
        super();
        this.message = message;
        this.device = device;
        this.stateCode = MessagingStateCode.Ok;
    }

    public Message getMessage() {
        return this.message;
    }

    public Device getDevice() {
        return this.device;
    }

    public MessagingStateCode getStateCode() {
        return this.stateCode;
    }

    public void setStateCode(MessagingStateCode stateCode) {
        this.stateCode = stateCode;
    }

    @Override
    public Object get(String name) {
        if (name.equals(MESSAGE)) {
            return this.message;
        }
        else if (name.equals(STATE)) {
            return this.stateCode;
        }
        else {
            return null;
        }
    }

    @Override
    public void set(String name, Object value) {
        if (name.equals(MESSAGE)) {
            this.message = (value instanceof Message) ? (Message) value : null;
        }
        else if (name.equals(STATE)) {
            this.stateCode = (value instanceof MessagingStateCode) ? (MessagingStateCode) value : null;
        }
    }
}
