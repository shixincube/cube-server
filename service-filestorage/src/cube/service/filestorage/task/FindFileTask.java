/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.filestorage.task;

import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.core.talk.dialect.DialectFactory;
import cube.auth.AuthToken;
import cube.benchmark.ResponseTime;
import cube.common.Packet;
import cube.common.entity.FileLabel;
import cube.common.state.FileStorageStateCode;
import cube.service.ServiceTask;
import cube.service.auth.AuthService;
import cube.service.filestorage.FileStorageService;
import cube.service.filestorage.FileStorageServiceCellet;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 查找文件任务。
 */
public class FindFileTask extends ServiceTask {

    public FindFileTask(FileStorageServiceCellet cellet, TalkContext talkContext,
                       Primitive primitive, ResponseTime responseTime) {
        super(cellet, talkContext, primitive, responseTime);
    }

    @Override
    public void run() {
        ActionDialect action = DialectFactory.getInstance().createActionDialect(this.primitive);
        Packet packet = new Packet(action);

        // 获取令牌码
        String tokenCode = this.getTokenCode(action);
        if (null == tokenCode) {
            // 发生错误
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, FileStorageStateCode.Unauthorized.code, packet.data));
            markResponseTime();
            return;
        }

        AuthService authService = (AuthService) this.kernel.getModule(AuthService.NAME);
        AuthToken authToken = authService.getToken(tokenCode);
        if (null == authToken) {
            // 发生错误
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, FileStorageStateCode.InvalidDomain.code, packet.data));
            markResponseTime();
            return;
        }

        // 域
        String domain = authToken.getDomain();
        long contactId = authToken.getContactId();

        String fileName = null;
        long lastModified = 0;
        long fileSize = 0;

        String md5Code = null;
        String fileCode = null;

        try {
            if (packet.data.has("fileName")) {
                fileName = packet.data.getString("fileName");
            }

            if (packet.data.has("md5")) {
                md5Code = packet.data.getString("md5");
            }

            if (packet.data.has("fileCode")) {
                fileCode = packet.data.getString("fileCode");
            }

            if (packet.data.has("lastModified")) {
                lastModified = packet.data.getLong("lastModified");
            }
            if (packet.data.has("fileSize")) {
                fileSize = packet.data.getLong("fileSize");
            }
        } catch (Exception e) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, FileStorageStateCode.Failure.code, packet.data));
            markResponseTime();
            return;
        }

        ArrayList<FileLabel> result = new ArrayList<>();

        FileStorageService service = (FileStorageService) this.kernel.getModule(FileStorageService.NAME);
        if (null != fileName && lastModified > 0 && fileSize > 0) {
            // 精确查找文件
            FileLabel fileLabel = service.findFile(domain, contactId, fileName, lastModified, fileSize);
            if (null != fileLabel) {
                result.add(fileLabel);
            }
        }
        else if (null != fileName) {
            // 使用文件名查找
            List<FileLabel> list = service.findFilesByFileName(domain, contactId, fileName);
            if (null != list) {
                result.addAll(list);
            }
        }
        else if (null != md5Code) {
            // 使用 MD5 查找
            List<FileLabel> list = service.findFilesByMD5(domain, contactId, md5Code);
            if (null != list) {
                result.addAll(list);
            }
        }
        else if (null != fileCode) {
            // 通过文件码查找
            FileLabel fileLabel = service.getFile(domain, fileCode);
            if (null != fileLabel) {
                result.add(fileLabel);
            }
        }

        if (!result.isEmpty()) {
            JSONArray array = new JSONArray();
            for (FileLabel fileLabel : result) {
                array.put(fileLabel.toJSON());
            }
            JSONObject responseData = new JSONObject();
            responseData.put("domain", domain);
            responseData.put("contactId", contactId);
            responseData.put("list", array);

            // 应答
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, FileStorageStateCode.Ok.code, responseData));
        }
        else {
            // 应答
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, FileStorageStateCode.NotFound.code, packet.data));
        }

        markResponseTime();
    }
}
