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
import cube.service.filestorage.hierarchy.Directory;
import cube.service.filestorage.hierarchy.FileHierarchy;

/**
 * 重命名文件。
 */
public class RenameFileTask extends ServiceTask {

    public RenameFileTask(FileStorageServiceCellet cellet, TalkContext talkContext,
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
        if (!packet.data.has("root") || !packet.data.has("dirId")
                || !packet.data.has("fileCode") || !packet.data.has("fileName")) {
            // 发生错误
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, FileStorageStateCode.Unauthorized.code, packet.data));
            markResponseTime();
            return;
        }

        long rootId = packet.data.getLong("root");
        long dirId = packet.data.getLong("dirId");
        String fileCode = packet.data.getString("fileCode");
        String fileName = packet.data.getString("fileName");

        // 获取服务
        FileStorageService service = (FileStorageService) this.kernel.getModule(FileStorageService.NAME);

        // 获取指定 ROOT ID 对应的文件层级描述
        FileHierarchy fileHierarchy = service.getFileHierarchy(domain, rootId);
        if (null == fileHierarchy) {
            // 发生错误
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, FileStorageStateCode.NoDirectory.code, packet.data));
            markResponseTime();
            return;
        }

        // 查找指定 ID 的目录
        Directory directory = fileHierarchy.getDirectory(dirId);
        if (null == directory) {
            // 发生错误
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, FileStorageStateCode.NoDirectory.code, packet.data));
            markResponseTime();
            return;
        }

        // 判断文件是否重名，这里的文件名不包含扩展名
        if (directory.existsFileWithFilename(fileName)) {
            // 文件重名
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, FileStorageStateCode.DuplicationOfName.code, packet.data));
            markResponseTime();
            return;
        }

        // 获取文件
        FileLabel fileLabel = service.getFile(domain, fileCode);
        if (null == fileLabel) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, FileStorageStateCode.NotFound.code, packet.data));
            markResponseTime();
            return;
        }

        // 重命名，这里的文件名不包含扩展名
        fileLabel = service.updateFileName(fileLabel, fileName);

        // 成功
        this.cellet.speak(this.talkContext,
                this.makeResponse(action, packet, FileStorageStateCode.Ok.code, fileLabel.toCompactJSON()));
        markResponseTime();
    }
}
