/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.filestorage;

import cell.core.talk.dialect.ActionDialect;
import cell.util.log.Logger;
import cube.common.Packet;
import cube.common.action.FileStorageAction;
import cube.common.entity.Folder;
import cube.common.state.FileStorageStateCode;
import cube.dispatcher.Performer;
import cube.util.CrossDomainHandler;
import cube.util.FileLabels;
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
            this.respond(response, HttpStatus.FORBIDDEN_403, this.makeError(HttpStatus.FORBIDDEN_403));
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
            this.respond(response, HttpStatus.NOT_ACCEPTABLE_406, this.makeError(HttpStatus.NOT_ACCEPTABLE_406));
            this.complete();
        }
    }

    private void listFiles(JSONObject data, HttpServletRequest request, HttpServletResponse response) {
        String token = data.has("token") ? data.getString("token") : null;
        if (null == token) {
            token = request.getHeader(HEADER_X_BAIZE_API_TOKEN);
            if (null == token) {
                this.respond(response, HttpStatus.FORBIDDEN_403, this.makeError(HttpStatus.FORBIDDEN_403));
                this.complete();
                return;
            }
        }

        String version = request.getHeader(HEADER_X_BAIZE_API_VERSION);
        if (null == version) {
            version = "v0";
        }

        boolean hierarchy = data.has("hierarchy") && data.getBoolean("hierarchy");

        Packet resultPacket = null;
        JSONObject resultJson = null;

        if (hierarchy) {
            // 获取根目录信息
            JSONObject payload = new JSONObject();
            Packet packet = new Packet(FileStorageAction.GetRoot.name, payload);
            ActionDialect packetDialect = packet.toDialect();
            packetDialect.addParam("token", token);

            ActionDialect responseDialect = this.performer.syncTransmit(FileStorageCellet.NAME, packetDialect);
            if (null == responseDialect) {
                this.respond(response, HttpStatus.BAD_REQUEST_400, this.makeError(HttpStatus.BAD_REQUEST_400));
                this.complete();
                return;
            }

            Packet responsePacket = new Packet(responseDialect);
            int stateCode = Packet.extractCode(responsePacket);
            if (stateCode != FileStorageStateCode.Ok.code) {
                Logger.w(this.getClass(), "#listFiles - Service state code : " + stateCode);
                this.respond(response, HttpStatus.NOT_FOUND_404, this.makeError(HttpStatus.NOT_FOUND_404));
                this.complete();
                return;
            }

            // 根目录
            Folder folder = new Folder(Packet.extractDataPayload(responsePacket));
            if (folder.numFiles == 0) {
                resultJson = new JSONObject();
                resultJson.put("total", folder.numFiles);
                resultJson.put("list", new JSONArray());
                resultPacket = new Packet(FileStorageAction.ListFiles.name, resultJson);
            }
            else {
                // 获取目录里的文件列表
                payload = new JSONObject();
                payload.put("root", folder.ownerId);
                payload.put("id", folder.id);
                payload.put("begin", 0);
                payload.put("end", folder.numFiles - 1);
                packet = new Packet(FileStorageAction.ListFiles.name, payload);
                packetDialect = packet.toDialect();
                packetDialect.addParam("token", token);
                responseDialect = this.performer.syncTransmit(FileStorageCellet.NAME, packetDialect);
                if (null == responseDialect) {
                    this.respond(response, HttpStatus.BAD_REQUEST_400, this.makeError(HttpStatus.BAD_REQUEST_400));
                    this.complete();
                    return;
                }

                responsePacket = new Packet(responseDialect);
                stateCode = Packet.extractCode(responsePacket);
                if (stateCode != FileStorageStateCode.Ok.code) {
                    Logger.w(this.getClass(), "#listFiles - Service state code : " + stateCode);
                    this.respond(response, HttpStatus.NOT_FOUND_404, this.makeError(HttpStatus.NOT_FOUND_404));
                    this.complete();
                    return;
                }

                resultJson = new JSONObject();
                JSONArray list = Packet.extractDataPayload(responsePacket).getJSONArray("list");
                for (int i = 0; i < list.length(); ++i) {
                    JSONObject json = list.getJSONObject(i);
                    FileLabels.reviseFileLabel(json, token, this.performer.getExternalHttpEndpoint(),
                            this.performer.getExternalHttpsEndpoint());
                }
                resultJson.put("total", folder.numFiles);
                resultJson.put("list", list);
                resultPacket = new Packet(FileStorageAction.ListFiles.name, resultJson);
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
                this.respond(response, HttpStatus.BAD_REQUEST_400, this.makeError(HttpStatus.BAD_REQUEST_400));
                this.complete();
                return;
            }

            Packet responsePacket = new Packet(responseDialect);
            int stateCode = Packet.extractCode(responsePacket);
            if (stateCode != FileStorageStateCode.Ok.code) {
                Logger.w(this.getClass(), "#listFiles - Service state code : " + stateCode);
                this.respond(response, HttpStatus.NOT_FOUND_404, this.makeError(HttpStatus.NOT_FOUND_404));
                this.complete();
                return;
            }

            resultJson = Packet.extractDataPayload(responsePacket);
            JSONArray array = resultJson.getJSONArray("list");
            for (int i = 0; i < array.length(); ++i) {
                JSONObject json = array.getJSONObject(i);
                FileLabels.reviseFileLabel(json, token, this.performer.getExternalHttpEndpoint(),
                        this.performer.getExternalHttpsEndpoint());
            }
            resultPacket = new Packet(FileStorageAction.ListFileLabels.name, resultJson);
        }

        if (version.equalsIgnoreCase("v1")) {
            this.respondOk(response, resultJson);
        }
        else {
            this.respondOk(response, resultPacket.toJSON());
        }
        this.complete();
    }

    private void findFile(JSONObject data, HttpServletRequest request, HttpServletResponse response) {
        String token = data.has("token") ? data.getString("token") : null;
        String md5 = data.has("md5") ? data.getString("md5") : null;
        String fileName = data.has("fileName") ? data.getString("fileName") : null;
        if (null == fileName) {
            fileName = data.has("filename") ? data.getString("filename") : null;
        }
        String fileCode = data.has("fileCode") ? data.getString("fileCode") : null;

        String version = request.getHeader(HEADER_X_BAIZE_API_VERSION);
        if (null == version) {
            version = "v0";
        }

        if (null == token) {
            token = request.getHeader(HEADER_X_BAIZE_API_TOKEN);
            if (null == token) {
                this.respond(response, HttpStatus.FORBIDDEN_403, this.makeError(HttpStatus.FORBIDDEN_403));
                this.complete();
                return;
            }
        }

        if (null == md5 && null == fileName && null == fileCode) {
            this.respond(response, HttpStatus.FORBIDDEN_403, this.makeError(HttpStatus.FORBIDDEN_403));
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

        ActionDialect responseDialect = this.performer.syncTransmit(FileStorageCellet.NAME, packetDialect, 10 * 1000);
        if (null == responseDialect) {
            this.respond(response, HttpStatus.BAD_REQUEST_400, packet.toJSON());
            this.complete();
            return;
        }

        Packet responsePacket = new Packet(responseDialect);

        int stateCode = Packet.extractCode(responsePacket);
        if (stateCode != FileStorageStateCode.Ok.code) {
            Logger.w(this.getClass(), "#findFile - Service state code : " + stateCode);
            this.respond(response, HttpStatus.NOT_FOUND_404, this.makeError(HttpStatus.NOT_FOUND_404));
            this.complete();
            return;
        }

        JSONObject responseData = Packet.extractDataPayload(responsePacket);
        JSONArray list = responseData.getJSONArray("list");
        for (int i = 0; i < list.length(); ++i) {
            // 修订文件标签
            JSONObject fileLabelJson = list.getJSONObject(i);
            FileLabels.reviseFileLabel(fileLabelJson, token,
                    this.performer.getExternalHttpEndpoint(), this.performer.getExternalHttpsEndpoint());
        }

        // 根据版本返回数据
        if (version.equalsIgnoreCase("v1")) {
            this.respondOk(response, responseData);
        }
        else {
            responsePacket = new Packet(FileStorageAction.FindFile.name, responseData);
            this.respondOk(response, responsePacket.toJSON());
        }
        this.complete();
    }

    private void deleteFile(JSONObject data, HttpServletRequest request, HttpServletResponse response) {
        String token = data.has("token") ? data.getString("token") : null;
        JSONArray fileCodeList = data.has("fileCodeList") ? data.getJSONArray("fileCodeList") : null;
        JSONArray md5List = data.has("md5List") ? data.getJSONArray("md5List") : null;

        String version = request.getHeader(HEADER_X_BAIZE_API_VERSION);
        if (null == version) {
            version = "v0";
        }

        if (null == token) {
            token = request.getHeader(HEADER_X_BAIZE_API_TOKEN);
            if (null == token) {
                response.setStatus(HttpStatus.FORBIDDEN_403);
                this.complete();
                return;
            }
        }

        JSONObject payload = new JSONObject();
        if (null != fileCodeList) {
            if (fileCodeList.isEmpty()) {
                this.respond(response, HttpStatus.FORBIDDEN_403, this.makeError(HttpStatus.FORBIDDEN_403));
                this.complete();
                return;
            }

            payload.put("fileCodeList", fileCodeList);
        }
        else if (null != md5List) {
            if (md5List.isEmpty()) {
                this.respond(response, HttpStatus.FORBIDDEN_403, this.makeError(HttpStatus.FORBIDDEN_403));
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
            this.respond(response, HttpStatus.BAD_REQUEST_400, this.makeError(HttpStatus.BAD_REQUEST_400));
            this.complete();
            return;
        }

        Packet responsePacket = new Packet(responseDialect);
        int stateCode = Packet.extractCode(responsePacket);
        if (stateCode != FileStorageStateCode.Ok.code) {
            Logger.w(this.getClass(), "#deleteFile - Service state code : " + stateCode);
            this.respond(response, HttpStatus.NOT_FOUND_404, this.makeError(HttpStatus.NOT_FOUND_404));
            this.complete();
            return;
        }

        JSONObject responseData = Packet.extractDataPayload(responsePacket);
        JSONArray list = responseData.getJSONArray("list");
        for (int i = 0; i < list.length(); ++i) {
            JSONObject json = list.getJSONObject(i);
            FileLabels.reviseFileLabel(json, token, this.performer.getExternalHttpEndpoint(),
                    this.performer.getExternalHttpsEndpoint());
        }

        if (version.equalsIgnoreCase("v1")) {
            this.respondOk(response, responseData);
        }
        else {
            responsePacket = new Packet(FileStorageAction.DeleteFile.name, responseData);
            this.respondOk(response, responsePacket.toJSON());
        }
        this.complete();
    }
}
