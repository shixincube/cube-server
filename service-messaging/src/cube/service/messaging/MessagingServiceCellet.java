/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
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
        else if (MessagingAction.Burn.name.equals(action)) {
            this.executor.execute(new BurnTask(this, talkContext, primitive, this.markResponseTime(action)));
        }
        else if (MessagingAction.Retract.name.equals(action)) {
            this.executor.execute(new RetractTask(this, talkContext, primitive, this.markResponseTime(action)));
        }
        else if (MessagingAction.Forward.name.equals(action)) {
            this.executor.execute(new ForwardTask(this, talkContext, primitive, this.markResponseTime(action)));
        }
        else if (MessagingAction.Delete.name.equals(action)) {
            this.executor.execute(new DeleteTask(this, talkContext, primitive, this.markResponseTime(action)));
        }
        else if (MessagingAction.RetractBoth.name.equals(action)) {
            this.executor.execute(new RetractBothTask(this, talkContext, primitive, this.markResponseTime(action)));
        }
        else if (MessagingAction.QueryState.name.equals(action)) {
            this.executor.execute(new QueryStateTask(this, talkContext, primitive, this.markResponseTime(action)));
        }
    }
}
