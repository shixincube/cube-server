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

package cube.service.contact;

import cell.core.cellet.Cellet;
import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.core.talk.dialect.DialectFactory;
import cell.util.CachedQueueExecutor;
import cell.util.log.Logger;
import cube.common.action.ContactActions;
import cube.service.contact.task.*;

import java.util.concurrent.ExecutorService;

/**
 * 联系人服务 Cellet 。
 */
public class ContactServiceCellet extends Cellet {

    public final static String NAME = "Contact";

    private ExecutorService executor = null;

    public ContactServiceCellet() {
        super(NAME);
    }

    @Override
    public boolean install() {
        this.executor = CachedQueueExecutor.newCachedQueueThreadPool(8);
        ContactManager.getInstance().setCellet(this);
        return true;
    }

    @Override
    public void uninstall() {
        this.executor.shutdown();
    }

    @Override
    public void onListened(TalkContext talkContext, Primitive primitive) {
        ActionDialect dialect = DialectFactory.getInstance().createActionDialect(primitive);
        String action = dialect.getName();

        if (ContactActions.Comeback.name.equals(action)) {
            this.executor.execute(new ComebackTask(this, talkContext, primitive));
        }
        else if (ContactActions.SignIn.name.equals(action)) {
            this.executor.execute(new SignInTask(this, talkContext, primitive));
        }
        else if (ContactActions.GetContact.name.equals(action)) {
            this.executor.execute(new GetContactTask(this, talkContext, primitive));
        }
        else if (ContactActions.GetGroup.name.equals(action)) {
            this.executor.execute(new GetGroupTask(this, talkContext, primitive));
        }
        else if (ContactActions.ListGroups.name.equals(action)) {
            this.executor.execute(new ListGroupsTask(this, talkContext, primitive));
        }
        else if (ContactActions.CreateGroup.name.equals(action)) {
            this.executor.execute(new CreateGroupTask(this, talkContext, primitive));
        }
        else if (ContactActions.DissolveGroup.name.equals(action)) {
            this.executor.execute(new DissolveGroupTask(this, talkContext, primitive));
        }
        else if (ContactActions.AddGroupMember.name.equals(action)) {
            this.executor.execute(new AddGroupMemberTask(this, talkContext, primitive));
        }
        else if (ContactActions.RemoveGroupMember.name.equals(action)) {
            this.executor.execute(new RemoveGroupMemberTask(this, talkContext, primitive));
        }
        else if (ContactActions.DeviceTimeout.name.equals(action)) {
            this.executor.execute(new DeviceTimeoutTask(this, talkContext, primitive));
        }
        else if (ContactActions.SignOut.name.equals(action)) {
            this.executor.execute(new SignOutTask(this, talkContext, primitive));
        }
        else {
            Logger.w(this.getClass(), "Unsupported action: " + action);
        }
    }
}
