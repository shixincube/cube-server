/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.contact;

import cell.api.Servable;
import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.core.talk.dialect.DialectFactory;
import cube.common.action.ContactAction;
import cube.core.AbstractCellet;
import cube.dispatcher.Performer;
import cube.dispatcher.contact.handler.DeleteContact;
import cube.dispatcher.contact.handler.GetContact;
import cube.dispatcher.contact.handler.NewContact;
import cube.dispatcher.contact.handler.VerificationCode;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 联系人模块网关的 Cellet 服务单元。
 */
public class ContactCellet extends AbstractCellet {

    /**
     * Cellet 名称。
     */
    public final static String NAME = "Contact";

    /**
     * 执行机。
     */
    private Performer performer;

    /**
     * Sign in 任务对象的缓存队列。
     */
    private ConcurrentLinkedQueue<SignInTask> signInTaskQueue;

    /**
     * Sign out 任务对象的缓存队列。
     */
    private ConcurrentLinkedQueue<SignOutTask> signOutTaskQueue;

    /**
     * Comeback 任务对象的缓存队列。
     */
    private ConcurrentLinkedQueue<ComebackTask> comebackTaskQueue;

    /**
     * Pass through 任务对象的缓存队列。
     */
    private ConcurrentLinkedQueue<PassThroughTask> passTaskQueue;

    /**
     * Disconnect 任务对象的缓存队列。
     */
    private ConcurrentLinkedQueue<DisconnectTask> disconnTaskQueue;

    public ContactCellet() {
        super(NAME);
        this.signInTaskQueue = new ConcurrentLinkedQueue<>();
        this.signOutTaskQueue = new ConcurrentLinkedQueue<>();
        this.comebackTaskQueue = new ConcurrentLinkedQueue<>();
        this.passTaskQueue = new ConcurrentLinkedQueue<>();
        this.disconnTaskQueue = new ConcurrentLinkedQueue<>();
    }

    @Override
    public boolean install() {
        this.performer = (Performer) this.getNucleus().getParameter("performer");
        this.setupHandlers();
        return true;
    }

    @Override
    public void uninstall() {
    }

    @Override
    public void onListened(TalkContext talkContext, Primitive primitive) {
        super.onListened(talkContext, primitive);

        ActionDialect actionDialect = DialectFactory.getInstance().createActionDialect(primitive);
        String action = actionDialect.getName();

        if (ContactAction.Comeback.name.equals(action)) {
            this.performer.execute(this.borrowComebackTask(talkContext, primitive));
        }
        else if (ContactAction.SignIn.name.equals(action)) {
            this.performer.execute(this.borrowSignInTask(talkContext, primitive));
        }
        else if (ContactAction.SignOut.name.equals(action)) {
            this.performer.execute(this.borrowSignOutTask(talkContext, primitive));
        }
        else if (ContactAction.ListGroups.name.equals(action)) {
            this.performer.execute(this.borrowPassTask(talkContext, primitive, false));
        }
        else {
            this.performer.execute(this.borrowPassTask(talkContext, primitive, true));
        }
    }

    @Override
    public void onQuitted(TalkContext context, Servable server) {
        this.performer.execute(this.borrowDisconnectTask(context));
    }

    private void setupHandlers() {
        this.performer.getHttpServer().addContextHandler(new GetContact(this.performer));
        this.performer.getHttpServer().addContextHandler(new NewContact(this.performer));
        this.performer.getHttpServer().addContextHandler(new DeleteContact(this.performer));
        this.performer.getHttpServer().addContextHandler(new VerificationCode(this.performer));
    }

    protected SignInTask borrowSignInTask(TalkContext talkContext, Primitive primitive) {
        SignInTask task = this.signInTaskQueue.poll();
        if (null == task) {
            task = new SignInTask(this, talkContext, primitive, this.performer);
            task.responseTime = this.markResponseTime(task.getAction().getName());
            return task;
        }

        task.reset(talkContext, primitive);
        task.responseTime = this.markResponseTime(task.getAction().getName());
        return task;
    }

    protected void returnSignInTask(SignInTask task) {
        task.markResponseTime();

        this.signInTaskQueue.offer(task);
    }

    protected SignOutTask borrowSignOutTask(TalkContext talkContext, Primitive primitive) {
        SignOutTask task = this.signOutTaskQueue.poll();
        if (null == task) {
            task = new SignOutTask(this, talkContext, primitive, this.performer);
            task.responseTime = this.markResponseTime(task.getAction().getName());
            return task;
        }

        task.reset(talkContext, primitive);
        task.responseTime = this.markResponseTime(task.getAction().getName());
        return task;
    }

    protected void returnSignOutTask(SignOutTask task) {
        task.markResponseTime();

        this.signOutTaskQueue.offer(task);
    }

    protected ComebackTask borrowComebackTask(TalkContext talkContext, Primitive primitive) {
        ComebackTask task = this.comebackTaskQueue.poll();
        if (null == task) {
            task = new ComebackTask(this, talkContext, primitive, this.performer);
            task.responseTime = this.markResponseTime(task.getAction().getName());
            return task;
        }

        task.reset(talkContext, primitive);
        task.responseTime = this.markResponseTime(task.getAction().getName());
        return task;
    }

    protected void returnComebackTask(ComebackTask task) {
        task.markResponseTime();

        this.comebackTaskQueue.offer(task);
    }

    protected PassThroughTask borrowPassTask(TalkContext talkContext, Primitive primitive, boolean sync) {
        PassThroughTask task = this.passTaskQueue.poll();
        if (null == task) {
            task = new PassThroughTask(this, talkContext, primitive, this.performer, sync);
            task.responseTime = this.markResponseTime(task.getAction().getName());
            return task;
        }

        task.reset(talkContext, primitive, sync);
        task.responseTime = this.markResponseTime(task.getAction().getName());
        return task;
    }

    protected void returnPassTask(PassThroughTask task) {
        task.markResponseTime();

        this.passTaskQueue.offer(task);
    }

    protected DisconnectTask borrowDisconnectTask(TalkContext talkContext) {
        DisconnectTask task = this.disconnTaskQueue.poll();
        if (null == task) {
            return new DisconnectTask(this, talkContext, this.performer);
        }

        task.reset(talkContext);
        return task;
    }

    protected void returnDisconnectTask(DisconnectTask task) {
        this.disconnTaskQueue.offer(task);
    }
}
