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

/**
 * 识别条形码。
 */
public class DetectBarCode extends ContextHandler {

    private Performer performer;

    public DetectBarCode(Performer performer) {
        super("/cv/barcode/detect");
        this.performer = performer;
        setHandler(new Handler());
    }

    private class Handler extends CVHandler {

        public Handler() {
            super();
        }

        @Override
        public void doPost(HttpServletRequest request, HttpServletResponse response) {
            String token = this.getLastRequestPath(request);

            try {
                JSONObject data = this.readBodyAsJSONObject(request);

                Packet packet = new Packet(CVAction.DetectBarCode.name, data);
                ActionDialect requestAction = packet.toDialect();
                requestAction.addParam("token", token);

                ActionDialect responseAction = performer.syncTransmit(CVCellet.NAME,
                        requestAction, 60 * 1000);
                if (null == responseAction) {
                    Logger.w(this.getClass(), "#doPost - Response is null");
                    this.respond(response, HttpStatus.REQUEST_TIMEOUT_408);
                    this.complete();
                    return;
                }

                Packet responsePacket = new Packet(responseAction);
                if (Packet.extractCode(responsePacket) != CVStateCode.Ok.code) {
                    Logger.w(this.getClass(), "#doPost - Response state is " + Packet.extractCode(responsePacket));
                    this.respond(response, HttpStatus.BAD_REQUEST_400);
                    this.complete();
                    return;
                }

                JSONObject responseData = Packet.extractDataPayload(responsePacket);
                JSONArray result = responseData.getJSONArray("result");
                for (int i = 0; i < result.length(); ++i) {
                    JSONObject json = result.getJSONObject(i);
                    JSONObject fileLabelJson = json.getJSONObject("file");
                    FileLabels.reviseFileLabel(fileLabelJson, token,
                            performer.getExternalHttpEndpoint(),
                            performer.getExternalHttpsEndpoint());
                }
                this.respondOk(response, responseData);
                this.complete();
            } catch (Exception e) {
                Logger.e(this.getClass(), "", e);
                this.respond(response, HttpStatus.FORBIDDEN_403);
                this.complete();
            }
        }
    }
}
