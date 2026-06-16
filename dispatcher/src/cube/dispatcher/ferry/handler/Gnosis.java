/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2026 Ambrose Xu.
 */

package cube.dispatcher.ferry.handler;

import cell.core.talk.dialect.ActionDialect;
import cell.util.log.Logger;
import cube.common.Packet;
import cube.common.state.AIGCStateCode;
import cube.dispatcher.Performer;
import cube.dispatcher.aigc.Manager;
import cube.dispatcher.ferry.FerryCellet;
import cube.ferry.FerryAction;
import cube.ferry.GnosisAgent;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Gnosis extends ContextHandler {

    private Performer performer;

    public Gnosis(Performer performer) {
        super("/ferry/gnosis/");
        setHandler(new Handler());
        this.performer = performer;
    }

    private class Handler extends FerryHandler {

        public Handler() {
            super();
        }

        @Override
        public void doGet(HttpServletRequest request, HttpServletResponse response) {
            String agent = request.getParameter("agent");
            String token = this.getApiToken(request);
            if (null == agent || null == token) {
                this.respond(response, HttpStatus.BAD_REQUEST_400);
                this.complete();
                return;
            }

            boolean mock = (null != request.getParameter("mock")) &&
                    request.getParameter("mock").equalsIgnoreCase("true");

            GnosisAgent gnosisAgent = new GnosisAgent(agent, mock);

            JSONObject payload = gnosisAgent.toJSON();
            Packet packet = new Packet(FerryAction.GnosisAgent.name, payload);
            ActionDialect dialect = packet.toDialect();
            dialect.addParam("token", token);

            ActionDialect responseDialect = performer.syncTransmit(FerryCellet.NAME, dialect);
            if (null == responseDialect) {
                Logger.w(Manager.class, "#doGet - Response is null : " + token);
                this.respond(response, HttpStatus.UNAUTHORIZED_401);
                this.complete();
                return;
            }

            Packet responsePacket = new Packet(responseDialect);
            if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
                Logger.d(Manager.class, "#doGet - Response state is NOT ok : " + Packet.extractCode(responsePacket));
                this.respond(response, HttpStatus.SERVICE_UNAVAILABLE_503);
                this.complete();
                return;
            }

            this.respondOk(response, Packet.extractDataPayload(responsePacket));
            this.complete();
        }
    }
}
