/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.hub;

import cell.api.Servable;
import cell.core.talk.Primitive;
import cell.core.talk.PrimitiveInputStream;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.core.talk.dialect.DialectFactory;
import cell.util.CachedQueueExecutor;
import cell.util.log.Logger;
import cube.core.AbstractCellet;
import cube.core.AbstractModule;
import cube.core.Kernel;
import cube.hub.HubAction;

import java.util.concurrent.ExecutorService;

/**
 * Hub 服务的 Cellet 单元。
 */
public class HubCellet extends AbstractCellet {

    private HubService service;

    public HubCellet() {
        super(HubService.NAME);
    }

    @Override
    public boolean install() {
        this.service = new HubService(this, this.getExecutor());
        Kernel kernel = (Kernel) this.getNucleus().getParameter("kernel");
        kernel.installModule(HubService.NAME, this.service);

        return true;
    }

    @Override
    public void uninstall() {
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
        else if (HubAction.PutFile.name.equals(action)) {
            this.service.processPutFile(dialect, new Responder(dialect, this, talkContext));
        }
        else {
            Logger.w(this.getClass(), "No support action : " + action);
        }
    }

    @Override
    public void onListened(TalkContext talkContext, PrimitiveInputStream inputStream) {
        this.execute(new StreamProcessor(talkContext, inputStream));
    }

    @Override
    public void onQuitted(TalkContext talkContext, Servable servable) {
        super.onQuitted(talkContext, servable);
        this.service.quit(talkContext);
    }

    /**
     * 流处理器。
     */
    protected class StreamProcessor implements Runnable {

        private TalkContext talkContext;

        private PrimitiveInputStream inputStream;

        public StreamProcessor(TalkContext talkContext, PrimitiveInputStream inputStream) {
            this.talkContext = talkContext;
            this.inputStream = inputStream;
        }

        @Override
        public void run() {
            // 将文件写入文件存储
            AbstractModule module = service.getKernel().getModule("FileStorage");
            module.notify(this.inputStream);
        }
    }
}
