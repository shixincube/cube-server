/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 Cube Team.
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

package cube.service.riskmgmt.plugin;

import cube.common.entity.TraceEvent;
import cube.common.entity.FileAttachment;
import cube.common.entity.Message;
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
    public void onAction(PluginContext context) {
        MessagingPluginContext ctx = (MessagingPluginContext) context;
        Message message = ctx.getMessage();
        if (null != message) {
            FileAttachment fileAttachment = message.getAttachment();
            if (null != fileAttachment) {
                // 有文件附件的消息
                this.riskManagement.addFileChainNode(TraceEvent.Delete, message, ctx.getDevice());
            }
        }
    }
}
