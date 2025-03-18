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
import cube.dispatcher.cv.CVCellet;
import cube.util.FileLabels;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 制作条形码。
 */
public class ObjectDetection extends ContextHandler {

    public ObjectDetection() {
        super("/cv/object/detect");
        setHandler(new Handler());
    }

    private class Handler extends CVHandler {

        public Handler() {
            super();
        }

        @Override
        public void doPost(HttpServletRequest request, HttpServletResponse response) {
            String token = this.getApiToken(request);
            if (null == token) {
                this.respond(response, HttpStatus.UNAUTHORIZED_401, this.makeError(HttpStatus.UNAUTHORIZED_401));
                this.complete();
                return;
            }

            try {
                JSONObject data = this.readBodyAsJSONObject(request);

                Packet packet = new Packet(CVAction.ObjectDetection.name, data);
                ActionDialect requestAction = packet.toDialect();
                requestAction.addParam("token", token);

                ActionDialect responseAction = CVCellet.getPerformer().syncTransmit(CVCellet.NAME,
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
                JSONArray result = responseData.getJSONArray("result");
                for (int i = 0; i < result.length(); ++i) {
                    JSONObject json = result.getJSONObject(i);
                    JSONObject fileLabelJson = json.getJSONObject("fileLabel");
                    FileLabels.reviseFileLabel(fileLabelJson, token,
                            CVCellet.getPerformer().getExternalHttpEndpoint(),
                            CVCellet.getPerformer().getExternalHttpsEndpoint());
                }
                this.respondOk(response, responseData);
                this.complete();
            } catch (Exception e) {
                Logger.e(this.getClass(), "", e);
                this.respond(response, HttpStatus.FORBIDDEN_403, this.makeError(HttpStatus.FORBIDDEN_403));
                this.complete();
            }
        }
    }
}
