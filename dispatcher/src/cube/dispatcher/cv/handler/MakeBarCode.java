/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.cv.handler;

import cell.core.talk.dialect.ActionDialect;
import cell.util.log.Logger;
import cube.common.Packet;
import cube.common.action.CVAction;
import cube.common.state.CVStateCode;
import cube.dispatcher.Performer;
import cube.dispatcher.cv.CVCellet;
import cube.util.FileLabels;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 制作条形码。
 */
public class MakeBarCode extends ContextHandler {

    private Performer performer;

    public MakeBarCode(Performer performer) {
        super("/cv/barcode/make");
        this.performer = performer;
        setHandler(new Handler(performer.getCVConcurrencyLimit()));
    }

    private class Handler extends CVHandler {

        protected final int maxConcurrency;

        protected final AtomicInteger concurrency = new AtomicInteger(0);

        public Handler(int maxConcurrency) {
            super();
            this.maxConcurrency = maxConcurrency;
        }

        @Override
        public void doPost(HttpServletRequest request, HttpServletResponse response) {
            if (this.concurrency.get() >= this.maxConcurrency) {
                Logger.w(this.getClass(), "#doPost - The connection reaches the maximum concurrent number : " +
                        concurrency.get() + "/" + maxConcurrency);
                this.respond(response, HttpStatus.NOT_ACCEPTABLE_406, this.makeError(HttpStatus.NOT_ACCEPTABLE_406));
                this.complete();
                return;
            }

            this.concurrency.incrementAndGet();

            try {
                String token = this.getLastRequestPath(request);

                JSONObject data = this.readBodyAsJSONObject(request);

                Packet packet = new Packet(CVAction.MakeBarCode.name, data);
                ActionDialect requestAction = packet.toDialect();
                requestAction.addParam("token", token);

                ActionDialect responseAction = performer.syncTransmit(CVCellet.NAME,
                        requestAction, 2 * 60 * 1000);
                if (null == responseAction) {
                    Logger.w(this.getClass(), "#doPost - Response is null");
                    this.respond(response, HttpStatus.REQUEST_TIMEOUT_408, this.makeError(HttpStatus.REQUEST_TIMEOUT_408));
                    this.complete();
                    return;
                }

                Packet responsePacket = new Packet(responseAction);
                if (Packet.extractCode(responsePacket) != CVStateCode.Ok.code) {
                    Logger.w(this.getClass(), "#doPost - Response state is " + Packet.extractCode(responsePacket));
                    this.respond(response, HttpStatus.BAD_REQUEST_400, this.makeError(HttpStatus.BAD_REQUEST_400));
                    this.complete();
                    return;
                }

                JSONObject responseData = Packet.extractDataPayload(responsePacket);
                JSONArray list = responseData.getJSONArray("list");
                for (int i = 0; i < list.length(); ++i) {
                    JSONObject json = list.getJSONObject(i);
                    FileLabels.reviseFileLabel(json, token,
                            performer.getExternalHttpEndpoint(),
                            performer.getExternalHttpsEndpoint());
                }
                this.respondOk(response, responseData);
                this.complete();
            } catch (Exception e) {
                this.respond(response, HttpStatus.FORBIDDEN_403, this.makeError(HttpStatus.FORBIDDEN_403));
                this.complete();
            } finally {
                this.concurrency.decrementAndGet();
            }
        }
    }
}
