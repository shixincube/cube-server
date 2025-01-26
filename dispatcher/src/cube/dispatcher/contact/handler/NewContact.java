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

public class NewContact extends ContextHandler {

    public NewContact(Performer performer) {
        super("/contact/new/");
        setHandler(new Handler(performer));
    }

    private class Handler extends ContactHandler {

        public Handler(Performer performer) {
            super(performer);
        }

        @Override
        public void doPost(HttpServletRequest request, HttpServletResponse response) {
            String tokenCode = getLastRequestPath(request);
            if (null == tokenCode) {
                this.respond(response, HttpStatus.FORBIDDEN_403);
                this.complete();
                return;
            }

            try {
                JSONObject data = this.readBodyAsJSONObject(request);

                Packet requestPacket = new Packet(ContactAction.NewContact.name, data);
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
                Logger.w(NewContact.class, "#doPost", e);
                this.respond(response, HttpStatus.BAD_REQUEST_400);
                this.complete();
            }
        }
    }
}
