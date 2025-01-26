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
import cube.service.filestorage.hierarchy.FileHierarchy;
import cube.service.filestorage.recycle.Trash;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * 罗列回收站内的废弃数据。
 */
public class ListTrashTask extends ServiceTask {

    public ListTrashTask(FileStorageServiceCellet cellet, TalkContext talkContext,
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

        // 根目录 ID 和目录 ID
        if (!packet.data.has("root") || !packet.data.has("begin") || !packet.data.has("end")) {
            // 发生错误
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, FileStorageStateCode.Unauthorized.code, packet.data));
            markResponseTime();
            return;
        }

        Long rootId = packet.data.getLong("root");
        int beginIndex = packet.data.getInt("begin");
        int endIndex = packet.data.getInt("end");

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

        // 获取废弃的目录和文件
        List<Trash> list = service.getRecycleBin().get(fileHierarchy.getRoot(), beginIndex, endIndex);

        JSONObject result = new JSONObject();
        result.put("root", rootId.longValue());
        result.put("begin", beginIndex);
        result.put("end", endIndex);
        result.put("size", list.size());

        JSONArray array = new JSONArray();
        for (Trash trash : list) {
            array.put(trash.toCompactJSON());
        }
        result.put("list", array);

        this.cellet.speak(this.talkContext,
                this.makeResponse(action, packet, FileStorageStateCode.Ok.code, result));
        markResponseTime();
    }
}
