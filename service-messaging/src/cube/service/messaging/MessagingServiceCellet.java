/**
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Shixin Cube Team.
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

import cell.core.cellet.Cellet;
import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.core.talk.dialect.DialectFactory;
import cell.util.CachedQueueExecutor;
import cube.common.action.MessagingActions;
import cube.core.Kernel;
import cube.service.messaging.task.PullTask;
import cube.service.messaging.task.PushTask;

import java.util.concurrent.ExecutorService;

/**
 * 消息服务 Cellet 。
 */
public class MessagingServiceCellet extends Cellet {

    public final static String NAME = "Messaging";

    private ExecutorService executor = null;

    public MessagingServiceCellet() {
        super(NAME);
    }

    @Override
    public boolean install() {
        Kernel kernel = (Kernel) this.nucleus.getParameter("kernel");
        kernel.installModule(MessagingServiceCellet.NAME, new MessagingService(this));

        this.executor = CachedQueueExecutor.newCachedQueueThreadPool(16);
        return true;
    }

    @Override
    public void uninstall() {
        Kernel kernel = (Kernel) this.nucleus.getParameter("kernel");
        kernel.uninstallModule(MessagingServiceCellet.NAME);

        this.executor.shutdown();
    }

    @Override
    public void onListened(TalkContext talkContext, Primitive primitive) {
        ActionDialect dialect = DialectFactory.getInstance().createActionDialect(primitive);
        String action = dialect.getName();

        if (MessagingActions.Push.name.equals(action)) {
            this.executor.execute(new PushTask(this, talkContext, primitive));
        }
        else if (MessagingActions.Pull.name.equals(action)) {
            this.executor.execute(new PullTask(this, talkContext, primitive));
        }
    }
}
