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

package cube.service.hub;

import cell.api.Servable;
import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.core.talk.dialect.DialectFactory;
import cell.util.CachedQueueExecutor;
import cell.util.log.Logger;
import cube.core.AbstractCellet;
import cube.core.Kernel;
import cube.hub.HubAction;

import java.util.concurrent.ExecutorService;

/**
 * Hub 服务的 Cellet 单元。
 */
public class HubCellet extends AbstractCellet {

    private ExecutorService executor;

    private HubService service;

    public HubCellet() {
        super(HubService.NAME);
    }

    @Override
    public boolean install() {
        this.executor = CachedQueueExecutor.newCachedQueueThreadPool(8);

        this.service = new HubService(this, this.executor);
        Kernel kernel = (Kernel) this.getNucleus().getParameter("kernel");
        kernel.installModule(HubService.NAME, this.service);

        return true;
    }

    @Override
    public void uninstall() {
        this.executor.shutdown();

        Kernel kernel = (Kernel) this.getNucleus().getParameter("kernel");
        kernel.uninstallModule(HubService.NAME);

        this.service = null;
    }

    public HubService getService() {
        return this.service;
    }

    @Override
    public void onListened(TalkContext talkContext, Primitive primitive) {
        super.onListened(talkContext, primitive);

        ActionDialect dialect = DialectFactory.getInstance().createActionDialect(primitive);
        String action = dialect.getName();

        if (HubAction.TriggerEvent.name.equals(action)) {
            this.service.triggerEvent(dialect.getParamAsJson("event"), new Responder(dialect, this, talkContext));
        }
        else if (HubAction.TransmitSignal.name.equals(action)) {
            this.service.transmitSignal(dialect.getParamAsJson("signal"), new Responder(dialect, this, talkContext));
        }
        else if (HubAction.Channel.name.equals(action)) {
            this.service.processChannel(dialect, new Responder(dialect, this, talkContext));
        }
        else {
            Logger.w(this.getClass(), "No support action : " + action);
        }
    }

    @Override
    public void onQuitted(TalkContext talkContext, Servable servable) {
        super.onQuitted(talkContext, servable);
        this.service.quit(talkContext);
    }
}
