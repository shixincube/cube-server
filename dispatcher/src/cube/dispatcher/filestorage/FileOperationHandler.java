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

package cube.dispatcher.filestorage;

import cell.core.talk.dialect.ActionDialect;
import cell.util.log.Logger;
import cube.common.Packet;
import cube.common.action.FileStorageAction;
import cube.common.entity.Folder;
import cube.common.state.FileStorageStateCode;
import cube.dispatcher.Performer;
import cube.dispatcher.util.FileLabelHelper;
import cube.util.CrossDomainHandler;
import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONArray;
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
        if (pathInfo.startsWith("/list")) {
            this.listFiles(data, request, response);
        }
        else if (pathInfo.startsWith("/find")) {
            this.findFile(data, request, response);
        }
        else if (pathInfo.startsWith("/delete")) {
            this.deleteFile(data, request, response);
        }
        else {
            response.setStatus(HttpStatus.NOT_ACCEPTABLE_406);
            this.complete();
        }
    }

    private void listFiles(JSONObject data, HttpServletRequest request, HttpServletResponse response) {
        String token = data.has("token") ? data.getString("token") : null;
        if (null == token) {
            response.setStatus(HttpStatus.FORBIDDEN_403);
            this.complete();
            return;
        }

        boolean hierarchy = data.has("hierarchy") && data.getBoolean("hierarchy");

        Packet responseData = null;

        if (hierarchy) {
            // 获取根目录信息
            JSONObject payload = new JSONObject();
            Packet packet = new Packet(FileStorageAction.GetRoot.name, payload);
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
                Logger.w(this.getClass(), "#listFiles - Service state code : " + stateCode);
                this.respond(response, HttpStatus.NOT_FOUND_404, responsePacket.toJSON());
                this.complete();
                return;
            }

            // 根目录
            Folder folder = new Folder(Packet.extractDataPayload(responsePacket));
            if (folder.numFiles == 0) {
                JSONObject packetPayload = new JSONObject();
                packetPayload.put("total", folder.numFiles);
                packetPayload.put("list", new JSONArray());
                responseData = new Packet(FileStorageAction.ListFiles.name, packetPayload);
            }
            else {
                // 获取目录里的文件列表
                payload = new JSONObject();
                payload.put("root", folder.ownerId);
                payload.put("id", folder.id);
                payload.put("begin", 0);
                payload.put("end", folder.numFiles - 1);
                packet = new Packet(FileStorageAction.ListFiles.name, payload);

                responseDialect = this.performer.syncTransmit(FileStorageCellet.NAME, packetDialect);
                if (null == responseDialect) {
                    this.respond(response, HttpStatus.BAD_REQUEST_400, packet.toJSON());
                    this.complete();
                    return;
                }

                responsePacket = new Packet(responseDialect);
                stateCode = Packet.extractCode(responsePacket);
                if (stateCode != FileStorageStateCode.Ok.code) {
                    Logger.w(this.getClass(), "#listFiles - Service state code : " + stateCode);
                    this.respond(response, HttpStatus.NOT_FOUND_404, responsePacket.toJSON());
                    this.complete();
                    return;
                }

                JSONObject packetPayload = new JSONObject();
                JSONArray list = Packet.extractDataPayload(responsePacket).getJSONArray("list");
                for (int i = 0; i < list.length(); ++i) {
                    JSONObject json = list.getJSONObject(i);
                    FileLabelHelper.reviseFileLabel(json, token, this.performer.getExternalHttpEndpoint(),
                            this.performer.getExternalHttpsEndpoint());
                }
                packetPayload.put("total", folder.numFiles);
                packetPayload.put("list", list);
                responseData = new Packet(FileStorageAction.ListFiles.name, packetPayload);
            }
        }
        else {
            // 不从层级结构读取
            JSONObject payload = data;
            Packet packet = new Packet(FileStorageAction.ListFileLabels.name, payload);
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
                Logger.w(this.getClass(), "#listFiles - Service state code : " + stateCode);
                this.respond(response, HttpStatus.NOT_FOUND_404, responsePacket.toJSON());
                this.complete();
                return;
            }

            JSONObject packetPayload = Packet.extractDataPayload(responsePacket);
            JSONArray array = packetPayload.getJSONArray("list");
            for (int i = 0; i < array.length(); ++i) {
                JSONObject json = array.getJSONObject(i);
                FileLabelHelper.reviseFileLabel(json, token, this.performer.getExternalHttpEndpoint(),
                        this.performer.getExternalHttpsEndpoint());
            }
            responseData = new Packet(FileStorageAction.ListFileLabels.name, packetPayload);
        }

        this.respondOk(response, responseData.toJSON());
        this.complete();
    }

    private void findFile(JSONObject data, HttpServletRequest request, HttpServletResponse response) {
        String token = data.has("token") ? data.getString("token") : null;
        String md5 = data.has("md5") ? data.getString("md5") : null;
        String fileName = data.has("fileName") ? data.getString("fileName") : null;
        String fileCode = data.has("fileCode") ? data.getString("fileCode") : null;

        if (null == token) {
            response.setStatus(HttpStatus.FORBIDDEN_403);
            this.complete();
            return;
        }

        if (null == md5 && null == fileName && null == fileCode) {
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
        if (null != fileCode) {
            payload.put("fileCode", fileCode);
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
            Logger.w(this.getClass(), "#findFile - Service state code : " + stateCode);
            this.respond(response, HttpStatus.NOT_FOUND_404, responsePacket.toJSON());
            this.complete();
            return;
        }

        JSONObject responseData = Packet.extractDataPayload(responsePacket);
        JSONArray list = responseData.getJSONArray("list");
        for (int i = 0; i < list.length(); ++i) {
            // 修订文件标签
            JSONObject fileLabelJson = list.getJSONObject(i);
            FileLabelHelper.reviseFileLabel(fileLabelJson, token,
                    this.performer.getExternalHttpEndpoint(), this.performer.getExternalHttpsEndpoint());
        }
        responsePacket = new Packet(FileStorageAction.FindFile.name, responseData);
        this.respondOk(response, responsePacket.toJSON());
        this.complete();
    }

    private void deleteFile(JSONObject data, HttpServletRequest request, HttpServletResponse response) {
        String token = data.has("token") ? data.getString("token") : null;
        JSONArray fileCodeList = data.has("fileCodeList") ? data.getJSONArray("fileCodeList") : null;
        JSONArray md5List = data.has("md5List") ? data.getJSONArray("md5List") : null;

        if (null == token) {
            response.setStatus(HttpStatus.FORBIDDEN_403);
            this.complete();
            return;
        }

        JSONObject payload = new JSONObject();
        if (null != fileCodeList) {
            if (fileCodeList.isEmpty()) {
                response.setStatus(HttpStatus.FORBIDDEN_403);
                this.complete();
                return;
            }

            payload.put("fileCodeList", fileCodeList);
        }
        else if (null != md5List) {
            if (md5List.isEmpty()) {
                response.setStatus(HttpStatus.FORBIDDEN_403);
                this.complete();
                return;
            }

            payload.put("md5List", md5List);
        }

        Packet packet = new Packet(FileStorageAction.DeleteFile.name, payload);
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
            Logger.w(this.getClass(), "#deleteFile - Service state code : " + stateCode);
            this.respond(response, HttpStatus.NOT_FOUND_404, responsePacket.toJSON());
            this.complete();
            return;
        }

        JSONObject responseData = Packet.extractDataPayload(responsePacket);
        JSONArray list = responseData.getJSONArray("list");
        for (int i = 0; i < list.length(); ++i) {
            JSONObject json = list.getJSONObject(i);
            FileLabelHelper.reviseFileLabel(json, token, this.performer.getExternalHttpEndpoint(),
                    this.performer.getExternalHttpsEndpoint());
        }
        responsePacket = new Packet(FileStorageAction.DeleteFile.name, responseData);
        this.respondOk(response, responsePacket.toJSON());
        this.complete();
    }
}
