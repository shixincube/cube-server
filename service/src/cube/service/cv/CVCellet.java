/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2020-2024 Ambrose Xu.
 */

package cube.service.cv;

import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.util.Utils;
import cell.util.log.Logger;
import cube.common.action.CVAction;
import cube.core.AbstractCellet;
import cube.core.Kernel;
import cube.service.cv.task.*;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * CV 服务的 Cellet 接口。
 */
public class CVCellet extends AbstractCellet {

    private CVService service;

    private ConcurrentLinkedQueue<Responder> responderList;

    public CVCellet() {
        super(CVService.NAME);
        this.responderList = new ConcurrentLinkedQueue<>();
    }

    @Override
    public boolean install() {
        this.service = new CVService(this);

        Kernel kernel = (Kernel) this.getNucleus().getParameter("kernel");
        kernel.installModule(CVService.NAME, this.service);

        return true;
    }

    @Override
    public void uninstall() {
        Kernel kernel = (Kernel) this.getNucleus().getParameter("kernel");
        kernel.uninstallModule(CVService.NAME);
    }

    public CVService getService() {
        return this.service;
    }

    public ActionDialect transmit(TalkContext talkContext, ActionDialect dialect) {
        return this.transmit(talkContext, dialect, 3 * 60 * 1000);
    }

    public ActionDialect transmit(TalkContext talkContext, ActionDialect dialect, long timeout) {
        return this.transmit(talkContext, dialect, timeout, Utils.generateSerialNumber());
    }

    public ActionDialect transmit(TalkContext talkContext, ActionDialect dialect, long timeout, long sn) {
        Responder responder = new Responder(sn, dialect);
        this.responderList.add(responder);

        if (!this.speak(talkContext, dialect)) {
            Logger.w(this.getClass(), "Speak session error: " + talkContext.getSessionHost());
            this.responderList.remove(responder);
            return null;
        }

        ActionDialect response = responder.waitingFor(timeout);
        if (null == response) {
            Logger.w(this.getClass(), "Response is null: " + talkContext.getSessionHost());
            this.responderList.remove(responder);
            return null;
        }

        return response;
    }

    public void interrupt(long sn) {
        Responder responder = null;
        for (Responder r : this.responderList) {
            if (r.getSN() == sn) {
                responder = r;
                break;
            }
        }

        if (null == responder) {
            return;
        }

        Logger.d(this.getClass(), "Response (" + sn + ") interrupt");
        this.responderList.remove(responder);
        responder.notifyResponse(new ActionDialect("interrupt"));
    }

    public boolean isInterruption(ActionDialect actionDialect) {
        return actionDialect.getName().equalsIgnoreCase("interrupt");
    }

    @Override
    public void onListened(TalkContext talkContext, Primitive primitive) {
        super.onListened(talkContext, primitive);

        ActionDialect dialect = new ActionDialect(primitive);
        String action = dialect.getName();

        if (dialect.containsParam(Responder.NotifierKey)) {
            // 应答阻塞访问
            for (Responder responder : this.responderList) {
                if (responder.isResponse(dialect)) {
                    responder.notifyResponse(dialect);
                    this.responderList.remove(responder);
                    break;
                }
            }
        }
        else if (CVAction.ClipPaper.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new ClipPaperTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (CVAction.MakeBarCode.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new MakeBarCodeTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (CVAction.DetectBarCode.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new DetectBarCodeTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (CVAction.ObjectDetection.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new ObjectDetectionTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (CVAction.PoseEstimation.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new PoseEstimationTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (CVAction.Setup.name.equals(action)) {
            // 来自 Endpoint 的请求
            this.execute(new SetupTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (CVAction.Teardown.name.equals(action)) {
            // 来自 Endpoint 的请求
            this.execute(new TeardownTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
    }
}
