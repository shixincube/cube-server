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

import java.util.List;

/**
 * 获取文件标签任务。
 */
public class ListFileLabelsTask extends ServiceTask {

    public ListFileLabelsTask(FileStorageServiceCellet cellet, TalkContext talkContext,
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

        // 最大数量
        final int limit = 10;

        int begin = 0;
        int end = limit - 1;

        if (packet.data.has("begin")) {
            begin = packet.data.getInt("begin");
        }

        if (packet.data.has("end")) {
            end = packet.data.getInt("end");
        }
        else {
            end = begin + limit - 1;
        }

        if (begin > end) {
            // 参数
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, FileStorageStateCode.InvalidParameter.code, packet.data));
            markResponseTime();
            return;
        }
        else if (end - begin + 1 > 10) {
            // 超出限制
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, FileStorageStateCode.InvalidParameter.code, packet.data));
            markResponseTime();
            return;
        }

        FileStorageService service = (FileStorageService) this.kernel.getModule(FileStorageService.NAME);
        List<FileLabel> list = service.getFileLabelsWithOwnerId(authToken.getDomain(), authToken.getContactId(),
                begin, end);

        JSONArray array = new JSONArray();
        for (FileLabel fileLabel : list) {
            array.put(fileLabel.toCompactJSON());
        }

        JSONObject payload = new JSONObject();
        payload.put("ownerId", authToken.getContactId());
        payload.put("domain", authToken.getDomain());
        payload.put("total", service.totalFileLabelsByOwnerId(authToken.getDomain(), authToken.getContactId()));
        payload.put("list", array);
        payload.put("begin", begin);
        payload.put("end", end);

        // 应答
        this.cellet.speak(this.talkContext,
                this.makeResponse(action, packet, FileStorageStateCode.Ok.code, payload));
        markResponseTime();
    }
}
