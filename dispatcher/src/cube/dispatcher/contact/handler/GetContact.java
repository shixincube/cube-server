/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
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
            String tokenCode = getApiToken(request);
            if (null == tokenCode) {
                this.respond(response, HttpStatus.FORBIDDEN_403, this.makeError(HttpStatus.FORBIDDEN_403));
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
                    this.respond(response, HttpStatus.NOT_ACCEPTABLE_406, this.makeError(HttpStatus.NOT_ACCEPTABLE_406));
                    this.complete();
                    return;
                }

                Packet responsePacket = new Packet(responseDialect);
                if (Packet.extractCode(responsePacket) != ContactStateCode.Ok.code) {
                    this.respond(response, HttpStatus.NOT_FOUND_404, this.makeError(HttpStatus.NOT_FOUND_404));
                    this.complete();
                    return;
                }

                this.respondOk(response, Packet.extractDataPayload(responsePacket));
                this.complete();
            } catch (Exception e) {
                Logger.w(GetContact.class, "#doGet", e);
                this.respond(response, HttpStatus.BAD_REQUEST_400, this.makeError(HttpStatus.BAD_REQUEST_400));
                this.complete();
            }
        }
    }
}
