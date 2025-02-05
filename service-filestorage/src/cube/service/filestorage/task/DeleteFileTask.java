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
import cube.common.entity.Contact;
import cube.common.entity.Device;
import cube.common.entity.FileLabel;
import cube.common.state.FileStorageStateCode;
import cube.file.hook.FileStorageHook;
import cube.service.ServiceTask;
import cube.service.auth.AuthService;
import cube.service.contact.ContactManager;
import cube.service.filestorage.FileStoragePluginContext;
import cube.service.filestorage.FileStorageService;
import cube.service.filestorage.FileStorageServiceCellet;
import cube.service.filestorage.hierarchy.Directory;
import cube.service.filestorage.hierarchy.FileHierarchy;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 删除文件。
 */
public class DeleteFileTask extends ServiceTask {

    public DeleteFileTask(FileStorageServiceCellet cellet, TalkContext talkContext,
                          Primitive primitive, ResponseTime responseTime) {
        super(cellet, talkContext, primitive, responseTime);
    }

    @Override
    public void run() {
        ActionDialect action = DialectFactory.getInstance().createActionDialect(this.primitive);
        Packet packet = new Packet(action);

        // 获取令牌码
        String tokenCode = this.getTokenCode(action);
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
        // 联系人 ID
        long contactId = authToken.getContactId();

        // 读取参数
        if (packet.data.has("root") && packet.data.has("workingId")
                && packet.data.has("fileList")) {
            Long rootId = packet.data.getLong("root");
            Long workingId = packet.data.getLong("workingId");
            JSONArray fileList = packet.data.getJSONArray("fileList");

            // 获取服务
            FileStorageService service = (FileStorageService) this.kernel.getModule(FileStorageService.NAME);

            // 获取指定 ROOT ID 对应的文件层级描述
            FileHierarchy fileHierarchy = service.getFileHierarchy(domain, rootId);
            if (null == fileHierarchy) {
                // 发生错误
                this.cellet.speak(this.talkContext,
                        this.makeResponse(action, packet, FileStorageStateCode.NotFound.code, packet.data));
                markResponseTime();
                return;
            }

            // 获取工作目录
            Directory workingDir = fileHierarchy.getDirectory(workingId);
            if (null == workingDir) {
                // 发生错误
                this.cellet.speak(this.talkContext,
                        this.makeResponse(action, packet, FileStorageStateCode.Reject.code, packet.data));
                markResponseTime();
                return;
            }

            // 读取文件码
            List<String> fileCodeList = new ArrayList<>();
            for (int i = 0; i < fileList.length(); ++i) {
                String fileCode = fileList.getString(i);
                fileCodeList.add(fileCode);
            }

            // 删除文件
            List<FileLabel> deletedList = workingDir.removeFiles(fileCodeList);
            JSONArray deleted = new JSONArray();
            for (FileLabel deletedFile : deletedList) {
                deleted.put(deletedFile.toCompactJSON());
            }

            JSONObject result = new JSONObject();
            result.put("workingId", workingId.longValue());
            result.put("workingDir", workingDir.toCompactJSON());
            result.put("deletedList", deleted);

            // 成功
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, FileStorageStateCode.Ok.code, result));
            markResponseTime();

            // 调用 Hook
            Contact contact = ContactManager.getInstance().getContact(tokenCode);
            Device device = ContactManager.getInstance().getDevice(tokenCode);
            FileStorageHook hook = service.getPluginSystem().getDeleteFileHook();
            for (FileLabel fileLabel : deletedList) {
                hook.apply(new FileStoragePluginContext(workingDir, fileLabel, contact, device));
            }
        }
        else if (packet.data.has("fileCodeList")) {
            JSONArray fileList = packet.data.getJSONArray("fileCodeList");
            List<String> fileCodeList = new ArrayList<>();
            for (int i = 0; i < fileList.length(); ++i) {
                String fileCode = fileList.getString(i);
                fileCodeList.add(fileCode);
            }

            // 获取服务
            FileStorageService service = (FileStorageService) this.kernel.getModule(FileStorageService.NAME);
            // 批量删除文件
            List<FileLabel> fileLabelList = service.deleteFiles(domain, contactId, fileCodeList);

            JSONObject result = new JSONObject();
            JSONArray array = new JSONArray();
            for (FileLabel fileLabel : fileLabelList) {
                array.put(fileLabel.toCompactJSON());
            }
            result.put("list", array);
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, FileStorageStateCode.Ok.code, result));
            markResponseTime();
        }
        else if (packet.data.has("md5List")) {
            JSONArray md5Array = packet.data.getJSONArray("md5List");
            List<String> md5List = new ArrayList<>();
            for (int i = 0; i < md5Array.length(); ++i) {
                String md5 = md5Array.getString(i);
                md5List.add(md5);
            }

            // 获取服务
            FileStorageService service = (FileStorageService) this.kernel.getModule(FileStorageService.NAME);
            // 批量删除文件
            List<FileLabel> fileLabelList = service.deleteFilesByMD5(domain, contactId, md5List);

            JSONObject result = new JSONObject();
            JSONArray array = new JSONArray();
            for (FileLabel fileLabel : fileLabelList) {
                array.put(fileLabel.toCompactJSON());
            }
            result.put("list", array);
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, FileStorageStateCode.Ok.code, result));
            markResponseTime();
        }
        else {
            // 发生错误
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, FileStorageStateCode.Unauthorized.code, packet.data));
            markResponseTime();
        }
    }
}
