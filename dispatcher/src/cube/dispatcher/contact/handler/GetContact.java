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

package cube.dispatcher.contact.handler;

import cell.core.talk.dialect.ActionDialect;
import cell.util.log.Logger;
import cube.common.Packet;
import cube.common.action.ContactAction;
import cube.common.state.ContactStateCode;
import cube.dispatcher.Performer;
import cube.dispatcher.contact.ContactCellet;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class GetContact extends ContextHandler {

    public GetContact(Performer performer) {
        super("/contact/get/");
        setHandler(new Handler(performer));
    }

    private class Handler extends ContactHandler {

        public Handler(Performer performer) {
            super(performer);
        }

        @Override
        public void doGet(HttpServletRequest request, HttpServletResponse response) {
            String tokenCode = getLastRequestPath(request);
            if (null == tokenCode) {
                this.respond(response, HttpStatus.FORBIDDEN_403);
                this.complete();
                return;
            }

            try {
                JSONObject data = new JSONObject();
                if (null != request.getParameter("code")) {
                    data.put("code", request.getParameter("code"));
                }
                else {
                    data.put("id", Long.parseLong(request.getParameter("id")));
                    data.put("domain", request.getParameter("domain"));
                }

                Packet requestPacket = new Packet(ContactAction.GetContact.name, data);
                ActionDialect dialect = requestPacket.toDialect();
                dialect.addParam("token", tokenCode);

                ActionDialect responseDialect = this.performer.syncTransmit(ContactCellet.NAME, dialect);
                if (null == responseDialect) {
                    this.respond(response, HttpStatus.NOT_ACCEPTABLE_406);
                    this.complete();
                    return;
                }

                Packet responsePacket = new Packet(responseDialect);
                if (Packet.extractCode(responsePacket) != ContactStateCode.Ok.code) {
                    this.respond(response, HttpStatus.NOT_FOUND_404);
                    this.complete();
                    return;
                }

                this.respondOk(response, Packet.extractDataPayload(responsePacket));
                this.complete();
            } catch (Exception e) {
                Logger.w(GetContact.class, "#doGet", e);
                this.respond(response, HttpStatus.BAD_REQUEST_400);
                this.complete();
            }
        }
    }
}
