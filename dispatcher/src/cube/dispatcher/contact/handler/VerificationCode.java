/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.contact.handler;

import cell.core.talk.dialect.ActionDialect;
import cube.common.Packet;
import cube.common.action.ContactAction;
import cube.common.state.ContactStateCode;
import cube.dispatcher.Performer;
import cube.dispatcher.aigc.Manager;
import cube.dispatcher.contact.ContactCellet;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class VerificationCode extends ContextHandler {

    public VerificationCode(Performer performer) {
        super("/contact/vc/");
        setHandler(new Handler(performer));
    }

    private class Handler extends ContactHandler {

        public Handler(Performer performer) {
            super(performer);
        }

        @Override
        public void doPost(HttpServletRequest request, HttpServletResponse response) {
            String token = this.getApiToken(request);
            if (!Manager.getInstance().checkToken(token, this.getDevice(request))) {
                this.respond(response, HttpStatus.UNAUTHORIZED_401, this.makeError(HttpStatus.UNAUTHORIZED_401));
                this.complete();
                return;
            }

            try {
                JSONObject data = this.readBodyAsJSONObject(request);
                data.put("device", this.getDevice(request).toJSON());
                Packet requestPacket = new Packet(ContactAction.RequestVerificationCode.name, data);
                ActionDialect dialect = requestPacket.toDialect();
                dialect.addParam("token", token);
                ActionDialect responseDialect = this.performer.syncTransmit(ContactCellet.NAME, dialect);
                if (null == responseDialect) {
                    this.respond(response, HttpStatus.FORBIDDEN_403, this.makeError(HttpStatus.FORBIDDEN_403));
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
                this.respond(response, HttpStatus.BAD_REQUEST_400, this.makeError(HttpStatus.BAD_REQUEST_400));
                this.complete();
            }
        }

        @Override
        public void doGet(HttpServletRequest request, HttpServletResponse response) {
            String token = this.getApiToken(request);
            if (!Manager.getInstance().checkToken(token, this.getDevice(request))) {
                this.respond(response, HttpStatus.UNAUTHORIZED_401, this.makeError(HttpStatus.UNAUTHORIZED_401));
                this.complete();
                return;
            }

            try {
                String phoneNumber = request.getParameter("number");
                String codeMD5 = request.getParameter("code");

                JSONObject data = new JSONObject();
                data.put("phoneNumber", phoneNumber);
                data.put("codeMD5", codeMD5);
                data.put("device", this.getDevice(request).toJSON());
                Packet requestPacket = new Packet(ContactAction.VerifyVerificationCode.name, data);
                ActionDialect dialect = requestPacket.toDialect();
                dialect.addParam("token", token);

                ActionDialect responseDialect = this.performer.syncTransmit(ContactCellet.NAME, dialect);
                if (null == responseDialect) {
                    this.respond(response, HttpStatus.FORBIDDEN_403, this.makeError(HttpStatus.FORBIDDEN_403));
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
                this.respond(response, HttpStatus.BAD_REQUEST_400, this.makeError(HttpStatus.BAD_REQUEST_400));
                this.complete();
            }
        }
    }
}
