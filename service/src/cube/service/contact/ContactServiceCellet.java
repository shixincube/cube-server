/**
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 Shixin Cube Team.
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

import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.core.talk.dialect.DialectFactory;
import cell.util.CachedQueueExecutor;
import cell.util.log.Logger;
import cube.common.action.ContactAction;
import cube.core.AbstractCellet;
import cube.core.Kernel;
import cube.service.contact.task.*;

import java.util.concurrent.ExecutorService;

/**
 * 联系人服务 Cellet 。
 */
public class ContactServiceCellet extends AbstractCellet {

    private ExecutorService executor = null;

    public ContactServiceCellet() {
        super(ContactManager.NAME);
    }

    @Override
    public boolean install() {
        this.executor = CachedQueueExecutor.newCachedQueueThreadPool(8);

        Kernel kernel = (Kernel) this.getNucleus().getParameter("kernel");
        kernel.installModule(ContactManager.NAME, ContactManager.getInstance());

        ContactManager.getInstance().setCellet(this);

        return true;
    }

    @Override
    public void uninstall() {
        this.executor.shutdown();

        Kernel kernel = (Kernel) this.getNucleus().getParameter("kernel");
        kernel.uninstallModule(ContactManager.NAME);
    }

    @Override
    public void onListened(TalkContext talkContext, Primitive primitive) {
        super.onListened(talkContext, primitive);

        ActionDialect dialect = DialectFactory.getInstance().createActionDialect(primitive);
        String action = dialect.getName();

        if (ContactAction.Comeback.name.equals(action)) {
            this.executor.execute(new ComebackTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (ContactAction.SignIn.name.equals(action)) {
            this.executor.execute(new SignInTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (ContactAction.GetContact.name.equals(action)) {
            this.executor.execute(new GetContactTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (ContactAction.GetGroup.name.equals(action)) {
            this.executor.execute(new GetGroupTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (ContactAction.ListGroups.name.equals(action)) {
            this.executor.execute(new ListGroupsTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (ContactAction.ModifyGroup.name.equals(action)) {
            this.executor.execute(new ModifyGroupTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (ContactAction.ModifyGroupMember.name.equals(action)) {
            this.executor.execute(new ModifyGroupMemberTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (ContactAction.CreateGroup.name.equals(action)) {
            this.executor.execute(new CreateGroupTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (ContactAction.DissolveGroup.name.equals(action)) {
            this.executor.execute(new DissolveGroupTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (ContactAction.AddGroupMember.name.equals(action)) {
            this.executor.execute(new AddGroupMemberTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (ContactAction.RemoveGroupMember.name.equals(action)) {
            this.executor.execute(new RemoveGroupMemberTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (ContactAction.GetAppendix.name.equals(action)) {
            this.executor.execute(new GetAppendixTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (ContactAction.UpdateAppendix.name.equals(action)) {
            this.executor.execute(new UpdateAppendixTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (ContactAction.DeviceTimeout.name.equals(action)) {
            this.executor.execute(new DeviceTimeoutTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (ContactAction.SignOut.name.equals(action)) {
            this.executor.execute(new SignOutTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (ContactAction.Disconnect.name.equals(action)) {
            this.executor.execute(new DisconnectTask(this, talkContext, primitive));
        }
        else {
            Logger.w(this.getClass(), "Unsupported action: " + action);
        }
    }
}
