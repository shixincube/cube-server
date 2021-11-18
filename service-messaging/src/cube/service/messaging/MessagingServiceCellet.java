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

import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.core.talk.dialect.DialectFactory;
import cell.util.CachedQueueExecutor;
import cube.common.action.MessagingAction;
import cube.core.AbstractCellet;
import cube.core.Kernel;
import cube.service.contact.task.GetContactZoneTask;
import cube.service.messaging.task.*;

import java.util.concurrent.ExecutorService;

/**
 * 消息服务 Cellet 。
 */
public class MessagingServiceCellet extends AbstractCellet {

    private ExecutorService executor = null;

    public MessagingServiceCellet() {
        super(MessagingService.NAME);
    }

    @Override
    public boolean install() {
        Kernel kernel = (Kernel) this.nucleus.getParameter("kernel");
        kernel.installModule(this.getName(), new MessagingService(this));

        this.executor = CachedQueueExecutor.newCachedQueueThreadPool(16);
        return true;
    }

    @Override
    public void uninstall() {
        Kernel kernel = (Kernel) this.nucleus.getParameter("kernel");
        kernel.uninstallModule(this.getName());

        this.executor.shutdown();
    }

    @Override
    public void onListened(TalkContext talkContext, Primitive primitive) {
        super.onListened(talkContext, primitive);

        ActionDialect dialect = DialectFactory.getInstance().createActionDialect(primitive);
        String action = dialect.getName();

        if (MessagingAction.Push.name.equals(action)) {
            this.executor.execute(new PushTask(this, talkContext, primitive, this.markResponseTime(action)));
        }
        else if (MessagingAction.Pull.name.equals(action)) {
            this.executor.execute(new PullTask(this, talkContext, primitive, this.markResponseTime(action)));
        }
        else if (MessagingAction.UpdateConversation.name.equals(action)) {
            this.executor.execute(new UpdateConversationTask(this, talkContext, primitive, this.markResponseTime(action)));
        }
        else if (MessagingAction.GetConversations.name.equals(action)) {
            this.executor.execute(new GetConversationsTask(this, talkContext, primitive, this.markResponseTime(action)));
        }
        else if (MessagingAction.Read.name.equals(action)) {
            this.executor.execute(new ReadTask(this, talkContext, primitive, this.markResponseTime(action)));
        }
        else if (MessagingAction.Recall.name.equals(action)) {
            this.executor.execute(new RecallTask(this, talkContext, primitive, this.markResponseTime(action)));
        }
        else if (MessagingAction.Delete.name.equals(action)) {
            this.executor.execute(new DeleteTask(this, talkContext, primitive, this.markResponseTime(action)));
        }
    }
}
