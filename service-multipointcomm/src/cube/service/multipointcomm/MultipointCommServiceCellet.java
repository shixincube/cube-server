/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.multipointcomm;

import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.core.talk.dialect.DialectFactory;
import cell.util.CachedQueueExecutor;
import cell.util.log.Logger;
import cube.common.action.MultipointCommAction;
import cube.core.AbstractCellet;
import cube.core.Kernel;
import cube.service.multipointcomm.task.*;

import java.util.concurrent.ExecutorService;

/**
 * 多方通讯服务的 Cellet 。
 */
public class MultipointCommServiceCellet extends AbstractCellet {

    private ExecutorService executor = null;

    public MultipointCommServiceCellet() {
        super(MultipointCommService.NAME);
    }

    @Override
    public boolean install() {
        Kernel kernel = (Kernel) this.nucleus.getParameter("kernel");
        kernel.installModule(this.getName(), new MultipointCommService(this));

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

        if (MultipointCommAction.Offer.name.equals(action)) {
            this.executor.execute(new OfferTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (MultipointCommAction.Answer.name.equals(action)) {
            this.executor.execute(new AnswerTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (MultipointCommAction.Candidate.name.equals(action)) {
            this.executor.execute(new CandidateTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (MultipointCommAction.Broadcast.name.equals(action)) {
            this.executor.execute(new BroadcastTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (MultipointCommAction.Bye.name.equals(action)) {
            this.executor.execute(new ByeTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (MultipointCommAction.Busy.name.equals(action)) {
            this.executor.execute(new BusyTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (MultipointCommAction.Invite.name.equals(action)) {
            this.executor.execute(new InviteTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (MultipointCommAction.ApplyCall.name.equals(action)) {
            this.executor.execute(new ApplyCallTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (MultipointCommAction.ApplyJoin.name.equals(action)) {
            this.executor.execute(new ApplyJoinTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (MultipointCommAction.ApplyTerminate.name.equals(action)) {
            this.executor.execute(new ApplyTerminateTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (MultipointCommAction.GetField.name.equals(action)) {
            this.executor.execute(new GetFieldTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (MultipointCommAction.CreateField.name.equals(action)) {
            this.executor.execute(new CreateFieldTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (MultipointCommAction.DestroyField.name.equals(action)) {
            this.executor.execute(new DestroyFieldTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else {
            Logger.w(this.getClass(), "Unsupported action: " + action);
        }
    }
}
