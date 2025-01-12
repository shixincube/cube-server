/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 Ambrose Xu.
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

package cube.dispatcher.cv.handler;

import cell.core.talk.dialect.ActionDialect;
import cell.util.log.Logger;
import cube.common.Packet;
import cube.common.action.CVAction;
import cube.common.state.CVStateCode;
import cube.dispatcher.cv.CVCellet;
import cube.dispatcher.util.FileLabelHelper;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 制作条形码。
 */
public class MakeBarCode extends ContextHandler {

    public MakeBarCode() {
        super("/cv/barcode/make");
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

                Packet packet = new Packet(CVAction.MakeBarCode.name, data);
                ActionDialect requestAction = packet.toDialect();
                requestAction.addParam("token", token);

                ActionDialect responseAction = CVCellet.getPerformer().syncTransmit(CVCellet.NAME,
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
                JSONArray list = responseData.getJSONArray("list");
                for (int i = 0; i < list.length(); ++i) {
                    JSONObject json = list.getJSONObject(i);
                    FileLabelHelper.reviseFileLabel(json, token,
                            CVCellet.getPerformer().getExternalHttpEndpoint(),
                            CVCellet.getPerformer().getExternalHttpsEndpoint());
                }
                this.respondOk(response, responseData);
                this.complete();
            } catch (Exception e) {
                this.respond(response, HttpStatus.FORBIDDEN_403);
                this.complete();
            }
        }
    }
}
