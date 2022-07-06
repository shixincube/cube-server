/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 Cube Team.
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

    private MessagingStateCode stateCode;

    public MessagingPluginContext(Message message) {
        super();
        this.message = message;
        this.stateCode = MessagingStateCode.Ok;
    }

    public Message getMessage() {
        return this.message;
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
