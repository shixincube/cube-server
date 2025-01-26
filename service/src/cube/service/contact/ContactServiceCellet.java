/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.contact;

import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.core.talk.dialect.DialectFactory;
import cell.util.log.Logger;
import cube.common.action.ContactAction;
import cube.core.AbstractCellet;
import cube.core.Kernel;
import cube.service.contact.task.*;

/**
 * 联系人服务 Cellet 。
 */
public class ContactServiceCellet extends AbstractCellet {

    public ContactServiceCellet() {
        super(ContactManager.NAME);
    }

    @Override
    public boolean install() {
        Kernel kernel = (Kernel) this.getNucleus().getParameter("kernel");
        kernel.installModule(ContactManager.NAME, ContactManager.getInstance());

        ContactManager.getInstance().setCellet(this);

        return true;
    }

    @Override
    public void uninstall() {
        Kernel kernel = (Kernel) this.getNucleus().getParameter("kernel");
        kernel.uninstallModule(ContactManager.NAME);
    }

    @Override
    public void onListened(TalkContext talkContext, Primitive primitive) {
        super.onListened(talkContext, primitive);

        if (!ContactManager.getInstance().isStarted()) {
            // 服务未就绪
            this.execute(new NotReadyTask(this, talkContext, primitive));
            return;
        }

        ActionDialect dialect = DialectFactory.getInstance().createActionDialect(primitive);
        String action = dialect.getName();

        if (ContactAction.Comeback.name.equals(action)) {
            this.execute(new ComebackTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (ContactAction.SignIn.name.equals(action)) {
            this.execute(new SignInTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (ContactAction.GetContact.name.equals(action)) {
            this.execute(new GetContactTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (ContactAction.ModifyContact.name.equals(action)) {
            this.execute(new ModifyContactTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (ContactAction.NewContact.name.equals(action)) {
            this.execute(new NewContactTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (ContactAction.DeleteContact.name.equals(action)) {
            this.execute(new DeleteContactTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (ContactAction.GetContactZone.name.equals(action)) {
            this.execute(new GetContactZoneTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (ContactAction.AddParticipantToZone.name.equals(action)) {
            this.execute(new AddParticipantToZoneTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (ContactAction.RemoveParticipantFromZone.name.equals(action)) {
            this.execute(new RemoveParticipantFromZoneTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (ContactAction.ContainsParticipantInZone.name.equals(action)) {
            this.execute(new ContainsParticipantInZoneTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (ContactAction.CreateContactZone.name.equals(action)) {
            this.execute(new CreateContactZoneTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (ContactAction.DeleteContactZone.name.equals(action)) {
            this.execute(new DeleteContactZoneTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (ContactAction.ModifyZoneParticipant.name.equals(action)) {
            this.execute(new ModifyZoneParticipantTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (ContactAction.Search.name.equals(action)) {
            this.execute(new SearchTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (ContactAction.GetGroup.name.equals(action)) {
            this.execute(new GetGroupTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (ContactAction.ListGroups.name.equals(action)) {
            this.execute(new ListGroupsTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (ContactAction.ModifyGroup.name.equals(action)) {
            this.execute(new ModifyGroupTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (ContactAction.CreateGroup.name.equals(action)) {
            this.execute(new CreateGroupTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (ContactAction.DismissGroup.name.equals(action)) {
            this.execute(new DismissGroupTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (ContactAction.AddGroupMember.name.equals(action)) {
            this.execute(new AddGroupMemberTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (ContactAction.RemoveGroupMember.name.equals(action)) {
            this.execute(new RemoveGroupMemberTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (ContactAction.GetAppendix.name.equals(action)) {
            this.execute(new GetAppendixTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (ContactAction.UpdateAppendix.name.equals(action)) {
            this.execute(new UpdateAppendixTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (ContactAction.ListContactZones.name.equals(action)) {
            this.execute(new ListContactZonesTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (ContactAction.TopList.name.equals(action)) {
            this.execute(new TopListTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (ContactAction.BlockList.name.equals(action)) {
            this.execute(new BlockListTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (ContactAction.DeviceTimeout.name.equals(action)) {
            this.execute(new DeviceTimeoutTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (ContactAction.SignOut.name.equals(action)) {
            this.execute(new SignOutTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (ContactAction.Disconnect.name.equals(action)) {
            this.execute(new DisconnectTask(this, talkContext, primitive));
        }
        else {
            Logger.w(this.getClass(), "Unsupported action: " + action);
        }
    }
}
