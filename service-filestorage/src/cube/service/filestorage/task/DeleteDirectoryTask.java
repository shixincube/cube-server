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
import cube.common.state.FileStorageStateCode;
import cube.service.ServiceTask;
import cube.service.auth.AuthService;
import cube.service.filestorage.FileStorageService;
import cube.service.filestorage.FileStorageServiceCellet;
import cube.service.filestorage.hierarchy.Directory;
import cube.service.filestorage.hierarchy.FileHierarchy;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 删除文件夹。
 */
public class DeleteDirectoryTask extends ServiceTask {

    public DeleteDirectoryTask(FileStorageServiceCellet cellet, TalkContext talkContext,
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

        // 读取参数
        if (!packet.data.has("root") || !packet.data.has("workingId")
                || !packet.data.has("dirList") || !packet.data.has("recursive")) {
            // 发生错误
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, FileStorageStateCode.Unauthorized.code, packet.data));
            markResponseTime();
            return;
        }

        Long rootId = packet.data.getLong("root");
        Long workingId = packet.data.getLong("workingId");
        JSONArray dirList = packet.data.getJSONArray("dirList");
        boolean recursive = packet.data.getBoolean("recursive");

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
                    this.makeResponse(action, packet, FileStorageStateCode.NotFound.code, packet.data));
            markResponseTime();
            return;
        }

        // 查找指定 ID 的目录
        List<Directory> directories = new ArrayList<>();
        for (int i = 0; i < dirList.length(); ++i) {
            Long dirId = dirList.getLong(i);
            Directory directory = workingDir.getDirectory(dirId);
            if (null != directory) {
                directories.add(directory);
            }
        }

        if (0 == directories.size()) {
            // 发生错误
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, FileStorageStateCode.NotFound.code, packet.data));
            markResponseTime();
            return;
        }

        // 删除目录
        List<Directory> deletedList = workingDir.deleteDirectories(directories, recursive);
        if (null == deletedList) {
            // 删除失败
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, FileStorageStateCode.Reject.code, packet.data));
            markResponseTime();
            return;
        }

        JSONArray deleted = new JSONArray();
        for (Directory deletedDir : deletedList) {
            deleted.put(deletedDir.toCompactJSON());
        }

        JSONObject result = new JSONObject();
        result.put("workingId", workingId.longValue());
        result.put("workingDir", workingDir.toCompactJSON());
        result.put("deletedList", deleted);

        // 成功
        this.cellet.speak(this.talkContext,
                this.makeResponse(action, packet, FileStorageStateCode.Ok.code, result));
        markResponseTime();
    }
}
