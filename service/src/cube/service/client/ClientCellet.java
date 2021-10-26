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
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.core.talk.dialect.DialectFactory;
import cell.util.CachedQueueExecutor;
import cube.core.AbstractCellet;
import cube.core.Kernel;
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
    }

    public Kernel getKernel() {
        return this.kernel;
    }

    @Override
    public void onListened(TalkContext talkContext, Primitive primitive) {
        super.onListened(talkContext, primitive);

        ActionDialect actionDialect = DialectFactory.getInstance().createActionDialect(primitive);
        String action = actionDialect.getName();

        if (Actions.LOGIN.name.equals(action)) {
            this.executor.execute(new Runnable() {
                @Override
                public void run() {
                    ClientManager.getInstance().login(actionDialect.getParamAsLong("id"), talkContext);
                }
            });
        }
        else if (Actions.PushMessage.name.equals(action)) {
            PushMessageTask task = new PushMessageTask(this, talkContext, actionDialect);
            this.executor.execute(new Runnable() {
                @Override
                public void run() {
                    task.run();
                }
            });
        }
        else if (Actions.GetContact.name.equals(action)) {
            GetContactTask task = new GetContactTask(this, talkContext, actionDialect);
            this.executor.execute(new Runnable() {
                @Override
                public void run() {
                    task.run();
                }
            });
        }
        else if (Actions.GetGroup.name.equals(action)) {
            GetGroupTask task = new GetGroupTask(this, talkContext, actionDialect);
            this.executor.execute(new Runnable() {
                @Override
                public void run() {
                    task.run();
                }
            });
        }
        else if (Actions.AddEventListener.name.equals(action)) {
            this.executor.execute(new Runnable() {
                @Override
                public void run() {
                    ClientManager.getInstance().addEventListener(actionDialect.getParamAsLong("id"),
                            actionDialect.getParamAsString("event"),
                            actionDialect.containsParam("param") ? actionDialect.getParamAsJson("param") : null);
                }
            });
        }
        else if (Actions.RemoveEventListener.name.equals(action)) {
            this.executor.execute(new Runnable() {
                @Override
                public void run() {
                    ClientManager.getInstance().removeEventListener(actionDialect.getParamAsLong("id"),
                            actionDialect.getParamAsString("event"),
                            actionDialect.containsParam("param") ? actionDialect.getParamAsJson("param") : null);
                }
            });
        }
        else if (Actions.ListOnlineContacts.name.equals(action)) {
            ListOnlineContactsTask task = new ListOnlineContactsTask(this, talkContext, actionDialect);
            this.executor.execute(new Runnable() {
                @Override
                public void run() {
                    task.run();
                }
            });
        }
        else if (Actions.QueryMessages.name.equals(action)) {
            QueryMessagesTask task = new QueryMessagesTask(this, talkContext, actionDialect);
            this.executor.execute(new Runnable() {
                @Override
                public void run() {
                    task.run();
                }
            });
        }
        else if (Actions.UpdateContact.name.equals(action)) {
            UpdateContactTask task = new UpdateContactTask(this, talkContext, actionDialect);
            this.executor.execute(new Runnable() {
                @Override
                public void run() {
                    task.run();
                }
            });
        }
        else if (Actions.ApplyToken.name.equals(action)) {
            ApplyTokenTask task = new ApplyTokenTask(this, talkContext, actionDialect);
            this.executor.execute(new Runnable() {
                @Override
                public void run() {
                    task.run();
                }
            });
        }
        else if (Actions.CreateContact.name.equals(action)) {
            CreateContactTask task = new CreateContactTask(this, talkContext, actionDialect);
            this.executor.execute(new Runnable() {
                @Override
                public void run() {
                    task.run();
                }
            });
        }
        else if (Actions.CreateDomainApp.name.equals(action)) {
            CreateDomainAppTask task = new CreateDomainAppTask(this, talkContext, actionDialect);
            this.executor.execute(new Runnable() {
                @Override
                public void run() {
                    task.run();
                }
            });
        }
    }

    @Override
    public void onQuitted(TalkContext talkContext, Servable servable) {
        super.onQuitted(talkContext, servable);

        ClientManager.getInstance().quit(talkContext);
    }
}
