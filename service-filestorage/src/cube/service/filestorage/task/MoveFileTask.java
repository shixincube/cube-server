/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 Cube Team.
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
import org.json.JSONObject;

/**
 * 移动文件到指定目录。
 */
public class MoveFileTask extends ServiceTask {

    public MoveFileTask(FileStorageServiceCellet cellet, TalkContext talkContext,
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

        // 检查数据
        if (!packet.data.has("root") || !packet.data.has("srcDirId")
                || !packet.data.has("destDirId") || !packet.data.has("fileCode")) {
            // 发生错误
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, FileStorageStateCode.Forbidden.code, packet.data));
            markResponseTime();
            return;
        }

        // 获取服务
        FileStorageService service = (FileStorageService) this.kernel.getModule(FileStorageService.NAME);

        long rootId = packet.data.getLong("root");
        long srcDirId = packet.data.getLong("srcDirId");
        long destDirId = packet.data.getLong("destDirId");
        String fileCode = packet.data.getString("fileCode");

        // 获取指定 ID 对应的文件层级描述
        FileHierarchy fileHierarchy = service.getFileHierarchy(domain, rootId);
        if (null == fileHierarchy) {
            // 发生错误
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, FileStorageStateCode.NotFound.code, packet.data));
            markResponseTime();
            return;
        }

        // 获取源目录
        Directory srcDirectory = fileHierarchy.getDirectory(srcDirId);
        if (null == srcDirectory) {
            // 发生错误
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, FileStorageStateCode.NotFound.code, packet.data));
            markResponseTime();
            return;
        }

        // 获取目标目录
        Directory destDirectory = fileHierarchy.getDirectory(destDirId);
        if (null == destDirectory) {
            // 发生错误
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, FileStorageStateCode.NotFound.code, packet.data));
            markResponseTime();
            return;
        }

        // 获取指定文件
        FileLabel fileLabel = service.getFile(domain, fileCode);
        if (null == fileLabel) {
            // 发生错误
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, FileStorageStateCode.FileLabelError.code, packet.data));
            markResponseTime();
            return;
        }

        // 添加到目标目录
        if (!destDirectory.addFileWithSilent(fileLabel)) {
            // 文件重复
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, FileStorageStateCode.DuplicationOfName.code, packet.data));
            markResponseTime();
            return;
        }

        // 从源目录移除
        srcDirectory.removeFileWithSilent(fileLabel);

        JSONObject response = new JSONObject();
        response.put("srcDirectory", srcDirectory.toCompactJSON());
        response.put("destDirectory", destDirectory.toCompactJSON());
        response.put("file", fileLabel.toCompactJSON());

        this.cellet.speak(this.talkContext,
                this.makeResponse(action, packet, FileStorageStateCode.Ok.code, response));
        markResponseTime();
    }
}
