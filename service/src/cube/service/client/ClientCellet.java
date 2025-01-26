/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.client;

import cell.api.Servable;
import cell.core.talk.Primitive;
import cell.core.talk.PrimitiveInputStream;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cube.common.action.ClientAction;
import cube.core.AbstractCellet;
import cube.core.Kernel;
import cube.service.Daemon;
import cube.service.client.task.*;

/**
 * 服务器客户端的 Cellet 单元。
 */
public class ClientCellet extends AbstractCellet {

    private Kernel kernel;

    public ClientCellet() {
        super("Client");
    }

    @Override
    public boolean install() {
        Kernel kernel = (Kernel) this.getNucleus().getParameter("kernel");
        ClientManager.getInstance().start(this, kernel);

        this.kernel = kernel;

        return true;
    }

    @Override
    public void uninstall() {
        ClientManager.getInstance().stop();
    }

    public Kernel getKernel() {
        return this.kernel;
    }

    public Daemon getDaemon() {
        return (Daemon) this.getNucleus().getParameter("daemon");
    }

    @Override
    public void onListened(TalkContext talkContext, Primitive primitive) {
        super.onListened(talkContext, primitive);

        ActionDialect actionDialect = new ActionDialect(primitive);
        String action = actionDialect.getName();

        if (ClientAction.Login.name.equals(action)) {
            this.execute(() -> {
                login(actionDialect, talkContext);
            });
        }
        else if (ClientAction.GetAuthToken.name.equals(action)) {
            this.execute(new GetAuthTokenTask(this, talkContext, actionDialect));
        }
        else if (ClientAction.InjectAuthToken.name.equals(action)) {
            this.execute(new InjectAuthTokenTask(this, talkContext, actionDialect));
        }
        else if (ClientAction.PushMessage.name.equals(action)) {
            this.execute(new PushMessageTask(this, talkContext, actionDialect));
        }
        else if (ClientAction.GetContact.name.equals(action)) {
            this.execute(new GetContactTask(this, talkContext, actionDialect));
        }
        else if (ClientAction.GetGroup.name.equals(action)) {
            this.execute(new GetGroupTask(this, talkContext, actionDialect));
        }
        else if (ClientAction.NewContact.name.equals(action)) {
            this.execute(new NewContactTask(this, talkContext, actionDialect));
        }
        else if (ClientAction.AddEventListener.name.equals(action)) {
            this.execute(() -> {
                ClientManager.getInstance().addEventListener(actionDialect.getParamAsLong("id"),
                        actionDialect.getParamAsString("event"),
                        actionDialect.containsParam("param") ? actionDialect.getParamAsJson("param") : null);
            });
        }
        else if (ClientAction.RemoveEventListener.name.equals(action)) {
            this.execute(() -> {
                ClientManager.getInstance().removeEventListener(actionDialect.getParamAsLong("id"),
                        actionDialect.getParamAsString("event"),
                        actionDialect.containsParam("param") ? actionDialect.getParamAsJson("param") : null);
            });
        }
        else if (ClientAction.ListOnlineContacts.name.equals(action)) {
            this.execute(new ListOnlineContactsTask(this, talkContext, actionDialect));
        }
        else if (ClientAction.QueryMessages.name.equals(action)) {
            this.execute(new QueryMessagesTask(this, talkContext, actionDialect));
        }
        else if (ClientAction.MarkReadMessages.name.equals(action)) {
            this.execute(new MarkReadTask(this, talkContext, actionDialect));
        }
        else if (ClientAction.GetFile.name.equals(action)) {
            this.execute(new GetFileTask(this, talkContext, actionDialect));
        }
        else if (ClientAction.PutFile.name.equals(action)) {
            this.execute(new PutFileTask(this, talkContext, actionDialect));
        }
        else if (ClientAction.DeleteFile.name.equals(action)) {
            this.execute(new DeleteFileTask(this, talkContext, actionDialect));
        }
        else if (ClientAction.FindFile.name.equals(action)) {
            this.execute(new FindFileTask(this, talkContext, actionDialect));
        }
        else if (ClientAction.ListFiles.name.equals(action)) {
            this.execute(new ListFilesTask(this, talkContext, actionDialect));
        }
        else if (ClientAction.GetSharingTag.name.equals(action)) {
            this.execute(new GetSharingTagTask(this, talkContext, actionDialect));
        }
        else if (ClientAction.ListSharingTags.name.equals(action)) {
            this.execute(new ListSharingTagsTask(this, talkContext, actionDialect));
        }
        else if (ClientAction.ListSharingTraces.name.equals(action)) {
            this.execute(new ListSharingVisitTracesTask(this, talkContext, actionDialect));
        }
        else if (ClientAction.TraverseVisitTrace.name.equals(action)) {
            this.execute(new TraverseVisitTraceTask(this, talkContext, actionDialect));
        }
        else if (ClientAction.GetFilePerf.name.equals(action)) {
            this.execute(new GetFileStoragePrefTask(this, talkContext, actionDialect));
        }
        else if (ClientAction.UpdateFilePerf.name.equals(action)) {
            this.execute(new UpdateFileStoragePrefTask(this, talkContext, actionDialect));
        }
        else if (ClientAction.UpdateContact.name.equals(action)) {
            this.execute(new UpdateContactTask(this, talkContext, actionDialect));
        }
        else if (ClientAction.ModifyContactZone.name.equals(action)) {
            this.execute(new ModifyContactZoneTask(this, talkContext, actionDialect));
        }
        else if (ClientAction.ApplyToken.name.equals(action)) {
            this.execute(new ApplyTokenTask(this, talkContext, actionDialect));
        }
        else if (ClientAction.CreateContact.name.equals(action)) {
            this.execute(new CreateContactTask(this, talkContext, actionDialect));
        }
        else if (ClientAction.ProcessFile.name.equals(action)) {
            this.execute(new ProcessFileTask(this, talkContext, actionDialect));
        }
        else if (ClientAction.SubmitWorkflow.name.equals(action)) {
            this.execute(new SubmitWorkflowTask(this, talkContext, actionDialect));
        }
        else if (ClientAction.ListContactBehaviors.name.equals(action)) {
            this.execute(new ListContactBehaviorsTask(this, talkContext, actionDialect));
        }
        else if (ClientAction.GetDomain.name.equals(action)) {
            this.execute(new GetDomainTask(this, talkContext, actionDialect));
        }
        else if (ClientAction.GetLog.name.equals(action)) {
            this.execute(new GetLogTask(this, talkContext, actionDialect));
        }
        else if (ClientAction.UpdateDomain.name.equals(action)) {
            this.execute(new UpdateDomainInfoTask(this, talkContext, actionDialect));
        }
        else if (ClientAction.CreateDomainApp.name.equals(action)) {
            this.execute(new CreateDomainAppTask(this, talkContext, actionDialect));
        }
        else if (ClientAction.AIGCGetServiceInfo.name.equals(action)) {
            this.execute(new AIGCGetServiceInfoTask(this, talkContext, actionDialect));
        }
        else {
            this.execute(new UnsupportedActionTask(this, talkContext, actionDialect));
        }
    }

    @Override
    public void onListened(TalkContext talkContext, PrimitiveInputStream inputStream) {
        this.execute(new StreamTask(this, talkContext, inputStream));
    }

    @Override
    public void onQuitted(TalkContext talkContext, Servable servable) {
        super.onQuitted(talkContext, servable);
        ClientManager.getInstance().quit(talkContext);
    }

    private void login(ActionDialect actionDialect, TalkContext talkContext) {
        boolean result = ClientManager.getInstance().login(actionDialect, talkContext);
        if (!result) {
            // 不合法的客户端，Session ID 返回 0
            ActionDialect response = new ActionDialect(ClientAction.Login.name);
            response.addParam("sessionId", (long) 0);
            this.speak(talkContext, response);
        }
        else {
            ActionDialect response = new ActionDialect(ClientAction.Login.name);
            response.addParam("sessionId", talkContext.getSessionId());
            this.speak(talkContext, response);
        }
    }
}
