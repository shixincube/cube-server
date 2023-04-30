/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 Cube Team.
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

package cube.dispatcher.filestorage;

import cell.core.talk.dialect.ActionDialect;
import cell.util.log.Logger;
import cube.common.Packet;
import cube.common.action.FileStorageAction;
import cube.common.state.FileStorageStateCode;
import cube.dispatcher.Performer;
import cube.util.CrossDomainHandler;
import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 文件操作处理。
 */
public class FileOperationHandler extends CrossDomainHandler {

    public final static String PATH = "/filestorage/operation/";

    private Performer performer;

    public FileOperationHandler(Performer performer) {
        super();
        this.performer = performer;
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        JSONObject data = null;
        try {
            data = this.readBodyAsJSONObject(request);
        } catch (IOException e) {
            response.setStatus(HttpStatus.UNAUTHORIZED_401);
            this.complete();
            return;
        }

        String pathInfo = request.getPathInfo();
        if (pathInfo.startsWith("/find")) {
            this.findFile(data, request, response);
        }
        else {
            response.setStatus(HttpStatus.NOT_ACCEPTABLE_406);
            this.complete();
        }
    }

    private void findFile(JSONObject data, HttpServletRequest request, HttpServletResponse response) {
        String token = data.has("token") ? data.getString("token") : null;
        String md5 = data.has("md5") ? data.getString("md5") : null;
        String fileName = data.has("fileName") ? data.getString("fileName") : null;

        if (null == token) {
            response.setStatus(HttpStatus.FORBIDDEN_403);
            this.complete();
            return;
        }

        if (null == md5 && null == fileName) {
            response.setStatus(HttpStatus.FORBIDDEN_403);
            this.complete();
            return;
        }

        JSONObject payload = new JSONObject();
        if (null != md5) {
            payload.put("md5", md5.toLowerCase());
        }
        if (null != fileName) {
            payload.put("fileName", fileName);
        }

        Packet packet = new Packet(FileStorageAction.FindFile.name, payload);
        ActionDialect packetDialect = packet.toDialect();
        packetDialect.addParam("token", token);

        ActionDialect responseDialect = this.performer.syncTransmit(FileStorageCellet.NAME, packetDialect);
        if (null == responseDialect) {
            this.respond(response, HttpStatus.BAD_REQUEST_400, packet.toJSON());
            this.complete();
            return;
        }

        Packet responsePacket = new Packet(responseDialect);

        int stateCode = Packet.extractCode(responsePacket);
        if (stateCode != FileStorageStateCode.Ok.code) {
            Logger.w(this.getClass(), "#doPost - Service state code : " + stateCode);
            this.respond(response, HttpStatus.NOT_FOUND_404, new JSONObject());
            this.complete();
            return;
        }

        JSONObject fileLabelJson = Packet.extractDataPayload(responsePacket);
        if (fileLabelJson.has("directURL")) {
            fileLabelJson.remove("directURL");
        }

        responsePacket = new Packet(FileStorageAction.FindFile.name, fileLabelJson);
        this.respondOk(response, responsePacket.toJSON());
        this.complete();
    }
}
