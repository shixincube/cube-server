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

package cube.dispatcher.riskmgmt;

import cell.core.talk.dialect.ActionDialect;
import cell.util.log.Logger;
import cube.common.Packet;
import cube.common.action.RiskManagementAction;
import cube.common.entity.ContactRisk;
import cube.common.state.FileStorageStateCode;
import cube.dispatcher.Performer;
import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ContactRiskHandler extends RiskManagementHandler {

    public final static String PATH = "/riskmgmt/contact/risk/";

    private Performer performer;

    public ContactRiskHandler(Performer performer) {
        super();
        this.performer = performer;
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        String token = this.getLastRequestPath(request);

        long contactId = 0;
        try {
            contactId = Long.parseLong(request.getParameter("cid"));
        } catch (Exception e) {
            this.respond(response, HttpStatus.FORBIDDEN_403, new JSONObject());
            this.complete();
            return;
        }

        JSONObject payload = new JSONObject();
        payload.put("contactId", contactId);
        Packet packet = new Packet(RiskManagementAction.GetContactRisk.name, payload);
        ActionDialect packetDialect = packet.toDialect();
        packetDialect.addParam("token", token);

        ActionDialect responseDialect = this.performer.syncTransmit(RiskManagementCellet.NAME, packetDialect);
        if (null == responseDialect) {
            this.respond(response, HttpStatus.BAD_REQUEST_400, packet.toJSON());
            this.complete();
            return;
        }

        Packet responsePacket = new Packet(responseDialect);
        int stateCode = Packet.extractCode(responsePacket);
        if (stateCode != FileStorageStateCode.Ok.code) {
            Logger.w(this.getClass(), "#doGet - Service state code : " + stateCode);
            this.respond(response, HttpStatus.NOT_FOUND_404, responsePacket.toJSON());
            this.complete();
            return;
        }

        int mask = Packet.extractDataPayload(responsePacket).getInt("mask");
        JSONObject responseData = ContactRisk.toJSON(mask);
        responseData.put("mask", mask);
        responseData.put("contactId", contactId);

        this.respondOk(response, responseData);
        this.complete();
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        String token = this.getLastRequestPath(request);

        long contactId = 0;
        int originalMask = 0;
        int mask = 0;

        try {
            JSONObject data = this.readBodyAsJSONObject(request);
            contactId = data.getLong("contactId");
            originalMask = data.getInt("originalMask");

            if (data.has("mask")) {
                // 如果设置了 mask 则直接操作 mask
                mask = data.getInt("mask");
            }
            else {
                mask = ContactRisk.toMask(originalMask, data);
            }
        } catch (Exception e) {
            this.respond(response, HttpStatus.FORBIDDEN_403, new JSONObject());
            this.complete();
            return;
        }

        JSONObject payload = new JSONObject();
        payload.put("contactId", contactId);
        payload.put("mask", mask);
        Packet packet = new Packet(RiskManagementAction.ModifyContactRisk.name, payload);
        ActionDialect packetDialect = packet.toDialect();
        packetDialect.addParam("token", token);

        ActionDialect responseDialect = this.performer.syncTransmit(RiskManagementCellet.NAME, packetDialect);
        if (null == responseDialect) {
            this.respond(response, HttpStatus.BAD_REQUEST_400, packet.toJSON());
            this.complete();
            return;
        }

        Packet responsePacket = new Packet(responseDialect);
        int stateCode = Packet.extractCode(responsePacket);
        if (stateCode != FileStorageStateCode.Ok.code) {
            Logger.w(this.getClass(), "#doPost - Service state code : " + stateCode);
            this.respond(response, HttpStatus.NOT_FOUND_404, responsePacket.toJSON());
            this.complete();
            return;
        }

        mask = Packet.extractDataPayload(responsePacket).getInt("mask");
        JSONObject responseData = ContactRisk.toJSON(mask);
        responseData.put("mask", mask);
        responseData.put("contactId", contactId);

        this.respondOk(response, responseData);
        this.complete();
    }
}
