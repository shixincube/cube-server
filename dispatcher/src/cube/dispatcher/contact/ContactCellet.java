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

package cube.dispatcher.contact;

import cell.core.cellet.Cellet;
import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.core.talk.dialect.DialectFactory;
import cell.util.CachedQueueExecutor;
import cube.common.action.ContactActions;
import cube.dispatcher.Performer;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;

/**
 * 联系人模块网关的 Cellet 服务单元。
 */
public class ContactCellet extends Cellet {

    /**
     * Cellet 名称。
     */
    public final static String NAME = "Contact";

    /**
     * 线程池执行器。
     */
    private ExecutorService executor;

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

    public ContactCellet() {
        super(NAME);
        this.signInTaskQueue = new ConcurrentLinkedQueue<>();
        this.signOutTaskQueue = new ConcurrentLinkedQueue<>();
        this.comebackTaskQueue = new ConcurrentLinkedQueue<>();
        this.passTaskQueue = new ConcurrentLinkedQueue<>();
    }

    @Override
    public boolean install() {
        this.executor = CachedQueueExecutor.newCachedQueueThreadPool(16);
        this.performer = (Performer) this.getNucleus().getParameter("performer");
        return true;
    }

    @Override
    public void uninstall() {
        this.executor.shutdown();
    }

    @Override
    public void onListened(TalkContext talkContext, Primitive primitive) {
        ActionDialect actionDialect = DialectFactory.getInstance().createActionDialect(primitive);
        String action = actionDialect.getName();

        if (ContactActions.Comeback.name.equals(action)) {
            this.executor.execute(this.borrowComebackTask(talkContext, primitive));
        }
        else if (ContactActions.SignIn.name.equals(action)) {
            this.executor.execute(this.borrowSignInTask(talkContext, primitive));
        }
        else if (ContactActions.SignOut.name.equals(action)) {
            this.executor.execute(this.borrowSignOutTask(talkContext, primitive));
        }
        else if (ContactActions.ListGroups.name.equals(action)) {
            this.executor.execute(this.borrowPassTask(talkContext, primitive, false));
        }
        else {
            this.executor.execute(this.borrowPassTask(talkContext, primitive, true));
        }
    }

    protected SignInTask borrowSignInTask(TalkContext talkContext, Primitive primitive) {
        SignInTask task = this.signInTaskQueue.poll();
        if (null == task) {
            return new SignInTask(this, talkContext, primitive, this.performer);
        }

        task.reset(talkContext, primitive);
        return task;
    }

    protected void returnSignInTask(SignInTask task) {
        this.signInTaskQueue.offer(task);
    }

    protected SignOutTask borrowSignOutTask(TalkContext talkContext, Primitive primitive) {
        SignOutTask task = this.signOutTaskQueue.poll();
        if (null == task) {
            return new SignOutTask(this, talkContext, primitive, this.performer);
        }

        task.reset(talkContext, primitive);
        return task;
    }

    protected void returnSignOutTask(SignOutTask task) {
        this.signOutTaskQueue.offer(task);
    }

    protected ComebackTask borrowComebackTask(TalkContext talkContext, Primitive primitive) {
        ComebackTask task = this.comebackTaskQueue.poll();
        if (null == task) {
            return new ComebackTask(this, talkContext, primitive, this.performer);
        }

        task.reset(talkContext, primitive);
        return task;
    }

    protected void returnComebackTask(ComebackTask task) {
        this.comebackTaskQueue.offer(task);
    }

    protected PassThroughTask borrowPassTask(TalkContext talkContext, Primitive primitive, boolean sync) {
        PassThroughTask task = this.passTaskQueue.poll();
        if (null == task) {
            return new PassThroughTask(this, talkContext, primitive, this.performer, sync);
        }

        task.reset(talkContext, primitive, sync);
        return task;
    }

    protected void returnPassTask(PassThroughTask task) {
        this.passTaskQueue.offer(task);
    }
}
