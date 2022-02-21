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

package cube.service.client;

import cell.api.Servable;
import cell.core.talk.Primitive;
import cell.core.talk.PrimitiveInputStream;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.core.talk.dialect.DialectFactory;
import cell.util.CachedQueueExecutor;
import cube.common.action.ClientAction;
import cube.core.AbstractCellet;
import cube.core.Kernel;
import cube.service.Daemon;
import cube.service.client.task.*;

import java.util.concurrent.ExecutorService;

/**
 * 服务器客户端的 Cellet 单元。
 */
public class ClientCellet extends AbstractCellet {

    private ExecutorService executor;

    private Kernel kernel;

    public ClientCellet() {
        super("Client");
    }

    @Override
    public boolean install() {
        this.executor = CachedQueueExecutor.newCachedQueueThreadPool(4);

        Kernel kernel = (Kernel) this.getNucleus().getParameter("kernel");
        ClientManager.getInstance().start(this, kernel);

        this.kernel = kernel;

        return true;
    }

    @Override
    public void uninstall() {
        this.executor.shutdown();

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

        ActionDialect actionDialect = DialectFactory.getInstance().createActionDialect(primitive);
        String action = actionDialect.getName();

        if (ClientAction.LOGIN.name.equals(action)) {
            this.executor.execute(() -> {
                boolean result = ClientManager.getInstance().login(actionDialect, talkContext);
                if (!result) {
                    // 关闭不合法的客户端
                    hangup(talkContext, false);
                }
            });
        }
        else if (ClientAction.GetAuthToken.name.equals(action)) {
            this.executor.execute(new GetAuthTokenTask(this, talkContext, actionDialect));
        }
        else if (ClientAction.PushMessage.name.equals(action)) {
            this.executor.execute(new PushMessageTask(this, talkContext, actionDialect));
        }
        else if (ClientAction.GetContact.name.equals(action)) {
            this.executor.execute(new GetContactTask(this, talkContext, actionDialect));
        }
        else if (ClientAction.GetGroup.name.equals(action)) {
            this.executor.execute(new GetGroupTask(this, talkContext, actionDialect));
        }
        else if (ClientAction.AddEventListener.name.equals(action)) {
            this.executor.execute(() -> {
                ClientManager.getInstance().addEventListener(actionDialect.getParamAsLong("id"),
                        actionDialect.getParamAsString("event"),
                        actionDialect.containsParam("param") ? actionDialect.getParamAsJson("param") : null);
            });
        }
        else if (ClientAction.RemoveEventListener.name.equals(action)) {
            this.executor.execute(() -> {
                ClientManager.getInstance().removeEventListener(actionDialect.getParamAsLong("id"),
                        actionDialect.getParamAsString("event"),
                        actionDialect.containsParam("param") ? actionDialect.getParamAsJson("param") : null);
            });
        }
        else if (ClientAction.ListOnlineContacts.name.equals(action)) {
            this.executor.execute(new ListOnlineContactsTask(this, talkContext, actionDialect));
        }
        else if (ClientAction.QueryMessages.name.equals(action)) {
            this.executor.execute(new QueryMessagesTask(this, talkContext, actionDialect));
        }
        else if (ClientAction.MarkReadMessages.name.equals(action)) {
            this.executor.execute(new MarkReadTask(this, talkContext, actionDialect));
        }
        else if (ClientAction.GetFile.name.equals(action)) {
            this.executor.execute(new GetFileTask(this, talkContext, actionDialect));
        }
        else if (ClientAction.PutFile.name.equals(action)) {
            this.executor.execute(new PutFileTask(this, talkContext, actionDialect));
        }
        else if (ClientAction.FindFile.name.equals(action)) {
            this.executor.execute(new FindFileTask(this, talkContext, actionDialect));
        }
        else if (ClientAction.UpdateContact.name.equals(action)) {
            this.executor.execute(new UpdateContactTask(this, talkContext, actionDialect));
        }
        else if (ClientAction.ModifyContactZone.name.equals(action)) {
            this.executor.execute(new ModifyContactZoneTask(this, talkContext, actionDialect));
        }
        else if (ClientAction.ApplyToken.name.equals(action)) {
            this.executor.execute(new ApplyTokenTask(this, talkContext, actionDialect));
        }
        else if (ClientAction.CreateContact.name.equals(action)) {
            this.executor.execute(new CreateContactTask(this, talkContext, actionDialect));
        }
        else if (ClientAction.CreateDomainApp.name.equals(action)) {
            this.executor.execute(new CreateDomainAppTask(this, talkContext, actionDialect));
        }
        else if (ClientAction.ProcessFile.name.equals(action)) {
            this.executor.execute(new ProcessFileTask(this, talkContext, actionDialect));
        }
        else if (ClientAction.GetLog.name.equals(action)) {
            this.executor.execute(new GetLogTask(this, talkContext, actionDialect));
        }
    }

    @Override
    public void onListened(TalkContext talkContext, PrimitiveInputStream inputStream) {
        this.executor.execute(new StreamTask(this, talkContext, inputStream));
    }

    @Override
    public void onQuitted(TalkContext talkContext, Servable servable) {
        super.onQuitted(talkContext, servable);

        ClientManager.getInstance().quit(talkContext);
    }
}
